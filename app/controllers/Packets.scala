package controllers

import akka.actor.ActorRef
import controllers.RequestResponse.Response
import models.ClientSettings

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
case class StartLobby() extends OutPacket

// Response type to the given Request
object RequestResponse extends Enumeration {
  type Response = Value
  val Accepted, Rejected = Value
}
