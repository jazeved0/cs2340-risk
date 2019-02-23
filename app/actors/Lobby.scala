package actors

import akka.actor.{Actor, Props}
import common.Util
import models.ClientSettings

import scala.collection.mutable

object Lobby {

  // Lobby Id management (generation and return)
  val IdLength = 4
  val LobbyIds: mutable.HashSet[String] = new mutable.HashSet()
  def generateId: String = {
    var id = Util.randomId(IdLength)
    while (LobbyIds.contains(id)) id = Util.randomId(IdLength)
    LobbyIds += id
    id
  }
  def freeId(id: String): Unit = LobbyIds -= id

  // Actor factory methods
  def props: Props = Props[Lobby]
  def apply(id: String, hostInfo: ClientSettings): Props =
    Props(new Lobby(id, Some(hostInfo)))
}

class Lobby(val id: String,
            // mutable depending on state
            var hostInfo: Option[ClientSettings],
            // List of clients who have joined the lobby
            val players:    mutable.LinkedHashMap[String, ClientWithActor]
                          = mutable.LinkedHashMap[String, ClientWithActor](),
            // List of clients who have established a ws connection, but not in lobby
            val connected:  mutable.LinkedHashMap[String, ClientWithActor]
                          = mutable.LinkedHashMap[String, ClientWithActor]()) extends Actor {

  def hasHostJoined: Boolean = hostInfo.isEmpty
  def close(): Unit = {
    // close the lobby
    Lobby.freeId(id)
  }

  // TODO Handle reception of incoming network packets here
  override def receive: Receive = {
    case ClientConnect(_, clientId: String, actor: ClientWithActor) =>
      // No need to validate input, message generated internally
      connected += clientId -> actor
    case RequestClientJoin(_, clientId: String, withSettings: ClientSettings) =>

  }
}
