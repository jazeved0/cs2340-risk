package controllers

import akka.actor.ActorRef
import controllers.RequestResponse.Response
import models.ClientSettings
import play.api.libs.json.{Json, Reads, Writes}

// Incoming packets from the network
sealed trait InPacket {
  def lobbyId: String
  def clientId: String
  def unapply(packet: InPacket): (String, String) = (packet.lobbyId, packet.clientId)
}
sealed trait LobbyPacket extends InPacket
sealed trait InGamePacket extends InPacket
// Client connection internal message created when WebSocket is established
// Also used to add the host when they are the first one to join
case class ClientConnect(lobbyId: String, clientId: String, actor: ActorRef) extends LobbyPacket
// Requests to join the lobby with the provided settings
case class RequestClientJoin(lobbyId: String, clientId: String, withSettings: ClientSettings) extends LobbyPacket
// Client connection internal message created when WebSocket is closed
case class ClientDisconnect(lobbyId: String, clientId: String) extends LobbyPacket with InGamePacket
// Incoming packet for starting the lobby
case class RequestStartLobby(lobbyId: String, clientId: String) extends LobbyPacket

// Outgoing packets to the network
sealed trait OutPacket
case class LobbyUpdate(iter: Iterable[ClientSettings], host: String) extends OutPacket
case class RequestReply(response: Response, message: String = "") extends OutPacket
case class BadPacket(message: String = "") extends OutPacket
case class StartLobby(identity: String = "start") extends OutPacket

// Response type to the given Request
object RequestResponse extends Enumeration {
  type Response = Value
  val Accepted, Rejected = Value
}

object JsonMarshallers {
  // Data object marshallers
  implicit val clientSettingsR: Reads[ClientSettings] = Json.reads[ClientSettings]
  implicit val clientSettingsW: Writes[ClientSettings] = Json.writes[ClientSettings]

  // Deserializers
  implicit val clientConnect: Reads[ClientConnect] = null // unused
  implicit val requestClientJoin: Reads[RequestClientJoin] = Json.reads[RequestClientJoin]
  implicit val clientDisconnect: Reads[ClientDisconnect] = Json.reads[ClientDisconnect]
  implicit val requestStartLobby: Reads[RequestStartLobby] = Json.reads[RequestStartLobby]

  // Serializers
  implicit val lobbyUpdate: Writes[LobbyUpdate] = Json.writes[LobbyUpdate]
  implicit val requestReply: Writes[RequestReply] = Json.writes[RequestReply]
  implicit val badPacket: Writes[BadPacket] = Json.writes[BadPacket]
  implicit val startLobby: Writes[StartLobby] = Json.writes[StartLobby]

  // Trait marshallers
  implicit val lobbyPacket: Reads[LobbyPacket] = Json.reads[LobbyPacket]
  implicit val inGamePacket: Reads[InGamePacket] = Json.reads[InGamePacket]
  implicit val inPacket: Reads[InPacket] = Json.reads[InPacket]
  implicit val outPacket: Writes[OutPacket] = Json.writes[OutPacket]
}
