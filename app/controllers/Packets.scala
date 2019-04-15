package controllers

import akka.actor.ActorRef
import controllers.RequestResponse.Response
import game.Gameboard
import game.state.{GameState, PlayerState, TurnState}
import models._
import play.api.libs.json._

/** Incoming packets from the network */
sealed trait InPacket {
  def gameId: String
  def playerId: String
  def unapply(packet: InPacket): (String, String) = (packet.gameId, packet.playerId)
}

/** Incoming packets from the network in the lobby state */
sealed trait LobbyPacket extends InPacket
/** Incoming packets from the network in the game state */
sealed trait InGamePacket extends InPacket
/** Incoming packets from the network in either state state */
sealed trait GlobalPacket extends InPacket
/** Player connection internal message created when WebSocket is established
  * Also used to add the host when they are the first one to join */
case class PlayerConnect(gameId: String, playerId: String, actor: ActorRef) extends LobbyPacket
/** Requests to join the game app with the provided settings */
case class RequestPlayerJoin(gameId: String, playerId: String, withSettings: PlayerSettings) extends LobbyPacket
/** Player connection internal message created when WebSocket is closed */
case class PlayerDisconnect(gameId: String, playerId: String) extends GlobalPacket
/** Incoming packet for starting the game */
case class RequestStartGame(gameId: String, playerId: String) extends LobbyPacket
/** Expected Ping Packet Response from Client (in Response to PingPlayer) */
case class PingResponse(gameId: String, playerId: String) extends GlobalPacket
/** Requests to place reinforcements at the given territories */
case class RequestPlaceReinforcements(gameId: String, playerId: String, assignments: Seq[(Int, Int)]) extends InGamePacket
/** Requests to execute an attack from one territory to another
  * `attack` contains 3 integers: the first territory index, the second territory index, and the amount of attacking
  * armies, in that order */
case class RequestAttack(gameId: String, playerId: String, attack: Seq[Int]) extends InGamePacket
/** Requests to end the current turn and validate army assignment and attack data */
case class RequestEndTurn(gameId: String, playerId: String) extends InGamePacket
/** Serves to receive the chosen amount of defending troops from a player in the Defense phase */
case class DefenseResponse(gameId: String, playerId: String, defenders: Int) extends InGamePacket

/** Outgoing packets to the network */
sealed trait OutPacket
/** Update packet for when the list of connected players/the host changes */
case class GameLobbyUpdate(seq: Seq[PlayerSettings], host: Int) extends OutPacket
/** Generic reply to request packets */
case class RequestReply(response: Response, message: String = "") extends OutPacket
/** Response packet to invalid states/malformed packets */
case class BadPacket(message: String = "") extends OutPacket
/** Control packet to start the game */
case class StartGame(identity: String = "start") extends OutPacket
/** Updates the gamestate of the players in the game */
case class UpdatePlayerState(seq: Seq[PlayerState], turn: Int) extends OutPacket
object UpdatePlayerState {
  def apply(state: GameState): UpdatePlayerState = UpdatePlayerState(state.playerStates, state.turn)
}
/** Outgoing packet to ping a player */
case class PingPlayer(identity: String = "ping") extends OutPacket
/** Sends the public config file as JSON */
case class SendConfig(config: String) extends OutPacket
/** Sends the Gameboard object parsed from the data/maps folder */
case class SendGameboard(gameboard: Gameboard) extends OutPacket
/** Updates the gamestate of the gameboard (all territories) */
case class UpdateBoardState(armies: Map[Int, (Int, Int)]) extends OutPacket
/** Sends the results of an attack; contains a list that has the combined dice rolls of both attacks */
case class SendAttackResult(diceRolls: Seq[Int]) extends OutPacket

object UpdateBoardState {
  def apply(state: GameState): UpdateBoardState = {
    val playerToInt = state.turnOrder.map(actor => actor.player).zipWithIndex.toMap
    new UpdateBoardState(state.boardState.zipWithIndex
      .filter(t => t._1.isDefined)
      .map(
          t => {
            val owner = t._1.get.owner
            val index = owner match {
              case _: ConcretePlayer => playerToInt(owner)
              case _: NeutralPlayer => -1
            }
            (t._2, (t._1.get.army.size, index))
          }
      )
      .toMap)
  }
}

/** Response type to the given Request */
object RequestResponse extends Enumeration {
  type Response = Value
  val Accepted, Rejected = Value
}

/** Used to satisfy the compile-time macros; [unused] */
class UnusedFormat[T <: InPacket] extends Reads[T] {
  override def reads(json: JsValue): JsResult[T] = {
    throw new NotImplementedError("Cannot deserialize internal messages")
  }
}

/** Writes a generic String -> Any map to JSON */
class PayloadWrites extends Writes[Seq[(String, Any)]] {
  override def writes(data: Seq[(String, Any)]): JsValue = {
    JsObject(data.iterator
      .map { case (key, value) => (key, JsString(value.toString)) }.toList)
  }
}

/** Contains (automatically generated) implementations for Reads and Writes
  * formatters, placed in the implicit scope */
object JsonMarshallers {
  // Data object marshallers
  implicit val playerSettingsR: Reads[PlayerSettings] = Json.reads[PlayerSettings]
  implicit val playerSettingsW: Writes[PlayerSettings] = Json.writes[PlayerSettings]
  implicit val armyW: Writes[Army] = Json.writes[Army]
  implicit val playerW: Writes[Player] = Json.writes[Player]
  implicit val concretePlayer: Writes[ConcretePlayer] = Json.writes[ConcretePlayer]
  implicit val neutralPlayer: Writes[NeutralPlayer] = Json.writes[NeutralPlayer]
  implicit val stateW: Writes[TurnState.State] = (s: TurnState.State) => JsString(TurnState.State.unapply(s).getOrElse(""))
  implicit val payloadW: Writes[Seq[(String, Any)]] = new PayloadWrites
  implicit val turnStateW: Writes[TurnState] = Json.writes[TurnState]
  implicit val playerStateW: Writes[PlayerState] = Json.writes[PlayerState]
  implicit val ownedArmyW: Writes[OwnedArmy] = Json.writes[OwnedArmy]
  implicit val locationW: Writes[Location] = Json.writes[Location]
  implicit val territoryW: Writes[Territory] = Json.writes[Territory]
  implicit val connectionW: Writes[Connection] = Json.writes[Connection]
  implicit val nodeW: Writes[Node] = Json.writes[Node]
  implicit val gameboardW: Writes[Gameboard] = Json.writes[Gameboard]

  // Deserializers
  implicit val requestPlayerJoin: Reads[RequestPlayerJoin] = Json.reads[RequestPlayerJoin]
  implicit val requestStartGame: Reads[RequestStartGame] = Json.reads[RequestStartGame]
  implicit val pingResponse: Reads[PingResponse] = Json.reads[PingResponse]
  implicit val requestPlaceReinforcements: Reads[RequestPlaceReinforcements] = Json.reads[RequestPlaceReinforcements]
  implicit val requestAttack: Reads[RequestAttack] = Json.reads[RequestAttack]
  implicit val requestEndTurn: Reads[RequestEndTurn] = Json.reads[RequestEndTurn]
  implicit val defenseResponse: Reads[DefenseResponse] = Json.reads[DefenseResponse]

  // Unused Deserializers; necessary for macros to work
  implicit val playerConnect: Reads[PlayerConnect] = new UnusedFormat[PlayerConnect]
  implicit val playerDisconnect: Reads[PlayerDisconnect] = new UnusedFormat[PlayerDisconnect]

  // Serializers
  implicit val gameLobbyUpdate: Writes[GameLobbyUpdate] = Json.writes[GameLobbyUpdate]
  implicit val requestReply: Writes[RequestReply] = Json.writes[RequestReply]
  implicit val badPacket: Writes[BadPacket] = Json.writes[BadPacket]
  implicit val startGame: Writes[StartGame] = Json.writes[StartGame]
  implicit val updatePlayerState: Writes[UpdatePlayerState] = Json.writes[UpdatePlayerState]
  implicit val pingPlayer: Writes[PingPlayer] = Json.writes[PingPlayer]
  implicit val sendConfig: Writes[SendConfig] = Json.writes[SendConfig]
  implicit val sendGameboard: Writes[SendGameboard] = Json.writes[SendGameboard]
  implicit val updateBoardState: Writes[UpdateBoardState] = Json.writes[UpdateBoardState]
  implicit val sendAttackResult: Writes[SendAttackResult] = Json.writes[SendAttackResult]

  // Trait marshallers
  implicit val globalPacket: Reads[GlobalPacket] = Json.reads[GlobalPacket]
  implicit val lobbyPacket: Reads[LobbyPacket] = Json.reads[LobbyPacket]
  implicit val inGamePacket: Reads[InGamePacket] = Json.reads[InGamePacket]
  implicit val inPacket: Reads[InPacket] = Json.reads[InPacket]
  implicit val outPacket: Writes[OutPacket] = Json.writes[OutPacket]
}
