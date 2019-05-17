package controllers

import akka.actor.ActorRef
import RequestResponse.Response
import game.Gameboard
import game.state.{GameState, PlayerState}
import models._

/** Incoming format from the network */
sealed trait InPacket {
  def gameId: String
  def playerId: String
  def unapply(packet: InPacket): (String, String) = (packet.gameId, packet.playerId)
}

/** Incoming format from the network in the lobby state */
sealed trait LobbyPacket extends InPacket
/** Incoming format from the network in the game state */
sealed trait InGamePacket extends InPacket
/** Incoming format from the network in either state state */
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
/** Requests to end the current turn's attack phase */
case class RequestEndAttack(gameId: String, playerId: String) extends InGamePacket
/** Requests to place troops during the maneuver phase at the end of the turn */
case class RequestDoManeuver(gameId: String, playerId: String, origin: Int, amount: Int, destination: Int) extends InGamePacket
/** Serves to receive the chosen amount of defending troops from a player in the Defense phase */
case class DefenseResponse(gameId: String, playerId: String, defenders: Int) extends InGamePacket

/** Outgoing format to the network */
sealed trait OutPacket
/** Update packet for when the list of connected players/the host changes */
case class GameLobbyUpdate(seq: Seq[PlayerSettings], host: Int) extends OutPacket
/** Generic reply to request format */
case class RequestReply(response: Response, message: String = "") extends OutPacket
/** Response packet to invalid states/malformed format */
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

object UpdateBoardState {
  def apply(state: GameState): UpdateBoardState = {
    val playerToInt = state.turnOrder.map(actor => actor.player).zipWithIndex.toMap
    new UpdateBoardState(state.boardState.zipWithIndex
      .map {
        case (territoryState, index) =>
          val playerIndex = territoryState.owner match {
            case p: ConcretePlayer => playerToInt(p)
            case    NeutralPlayer  => -1
          }
          (index, (territoryState.size, playerIndex))
      }
      .toMap)
  }
}
