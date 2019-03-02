package controllers

import akka.actor.ActorRef
import controllers.RequestResponse.Response
import game.{Army, PlayerState}
import models.PlayerSettings
import play.api.libs.json._

// Incoming packets from the network
sealed trait InPacket {
  def gameId: String
  def playerId: String
  def unapply(packet: InPacket): (String, String) = (packet.gameId, packet.playerId)
}

sealed trait LobbyPacket extends InPacket
sealed trait InGamePacket extends InPacket
sealed trait GlobalPacket extends InPacket
// Player connection internal message created when WebSocket is established
// Also used to add the host when they are the first one to join
case class PlayerConnect(gameId: String, playerId: String, actor: ActorRef) extends LobbyPacket
// Requests to join the game app with the provided settings
case class RequestPlayerJoin(gameId: String, playerId: String, withSettings: PlayerSettings) extends LobbyPacket
// Player connection internal message created when WebSocket is closed
case class PlayerDisconnect(gameId: String, playerId: String) extends GlobalPacket
// Incoming packet for starting the game
case class RequestStartGame(gameId: String, playerId: String) extends LobbyPacket
// Temporary class to satisfy Marshaller macros
case class Unused(gameId: String, playerId: String) extends InGamePacket
// Expected Ping Packet Response from Client (in Response to PingPlayer)
case class PingResponse(gameId: String, playerId: String) extends GlobalPacket

// Outgoing packets to the network
sealed trait OutPacket
case class GameLobbyUpdate(seq: Seq[PlayerSettings], host: Int) extends OutPacket
case class RequestReply(response: Response, message: String = "") extends OutPacket
case class BadPacket(message: String = "") extends OutPacket
case class StartGame(identity: String = "start") extends OutPacket
case class UpdatePlayerState(seq: Seq[PlayerState]) extends OutPacket
case class PingPlayer(identity: String = "ping") extends OutPacket
case class SendConfig(config: String) extends OutPacket

// Response type to the given Request
object RequestResponse extends Enumeration {
  type Response = Value
  val Accepted, Rejected = Value
}

class UnusedFormat[T <: InPacket] extends Reads[T] {
  override def reads(json: JsValue): JsResult[T] = {
    throw new NotImplementedError("Cannot deserialize internal messages")
  }
}

object JsonMarshallers {
  // Data object marshallers
  implicit val playerSettingsR: Reads[PlayerSettings] = Json.reads[PlayerSettings]
  implicit val playerSettingsW: Writes[PlayerSettings] = Json.writes[PlayerSettings]
  implicit val armyW: Writes[Army] = Json.writes[Army]
  implicit val playerStateW: Writes[PlayerState] = Json.writes[PlayerState]

  // Deserializers
  implicit val requestPlayerJoin: Reads[RequestPlayerJoin] = Json.reads[RequestPlayerJoin]
  implicit val requestStartGame: Reads[RequestStartGame] = Json.reads[RequestStartGame]
  implicit val pingResponse: Reads[PingResponse] = Json.reads[PingResponse]
  implicit val unused: Reads[Unused] = Json.reads[Unused]

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

  // Trait marshallers
  implicit val globalPacket: Reads[GlobalPacket] = Json.reads[GlobalPacket]
  implicit val lobbyPacket: Reads[LobbyPacket] = Json.reads[LobbyPacket]
  implicit val inGamePacket: Reads[InGamePacket] = Json.reads[InGamePacket]
  implicit val inPacket: Reads[InPacket] = Json.reads[InPacket]
  implicit val outPacket: Writes[OutPacket] = Json.writes[OutPacket]
}
