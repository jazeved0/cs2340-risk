package actors

import actors.RequestResponse.Response
import models.ClientSettings

// Incoming packets from the network
trait InPacket {
  def lobbyId: String
  def clientId: String
}
// Client connection internal message created when WebSocket is established
case class ClientConnect(lobbyId: String, clientId: String, actor: ClientWithActor) extends InPacket
// Requests to join the lobby with the provided settings
// TODO Move validation logic to here instead of returning a BadPacket; develop
//  schema for response format: (maybe when a client joins, send them a response packet
//  instead of a LobbyUpdate and only send that to everyone else
case class RequestClientJoin(lobbyId: String, clientId: String, withSettings: ClientSettings) extends InPacket
// Client connection internal message created when WebSocket is closed
// TODO Send a lobby update when this happens
case class ClientDisconnect(lobbyId: String, clientId: String) extends InPacket
// Incoming packet for starting the lobby
// TODO figure out how to send response for this (i.e. invalid InPacket)
case class RequestStartLobby(lobbyId: String, clientId: String) extends InPacket

// Outgoing packets to the network
trait OutPacket
case class LobbyUpdate(iter: Iterable[ClientSettings], host: String) extends OutPacket
case class RequestReply(response: Response, message: String = "") extends OutPacket
case class StartLobby() extends OutPacket

// Response type to the given Request
object RequestResponse extends Enumeration {
  type Response = Value
  val Success, Rejected = Value
}
