package actors

import actors.LobbySupervisor.{LobbyExists, MakeLobby}
import akka.actor.{Actor, ActorRef}
import models.ClientSettings

import scala.collection.mutable

object LobbySupervisor {
  // internal messages from MainController
  // TODO Implement Lobby destruction/closure
  case class LobbyExists(id: String)
  case class MakeLobby(hostInfo: ClientSettings)
}

class LobbySupervisor extends Actor {
  val lobbies: mutable.HashMap[String, ActorRef] = mutable.HashMap[String, ActorRef]()
  override def receive: Receive = {
    // Internal message to poll lobby existence
    case LobbyExists(lobbyId) =>
      sender() ! lobbies.isDefinedAt(lobbyId)

      // Internal message to make a new Lobby actor given the host's ClientSettings
    case MakeLobby(hostInfo) =>
      val id = Lobby.generateId
      val lobby = context.actorOf(Lobby(id, hostInfo))
      lobbies += id -> lobby
      sender() ! id

    // Any deserialized network packets getting sent to the lobby
    case inPacket: InPacket =>
      lobbies.get(inPacket.lobbyId).foreach(_ ! inPacket)

    case _ =>
  }
}
