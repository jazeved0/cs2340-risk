package models

import scala.collection.mutable

object Lobby {
  val IdLength = 5
  val LobbyIds: mutable.HashSet[String] = new mutable.HashSet()

  def generateId: String = {
    var id = Util.randomString(IdLength)
    while (!LobbyIds.contains(id)) id = Util.randomString(IdLength)
    id
  }

  def freeId(id: String): Unit = LobbyIds -= id

  def make(hostName: String, hostColor: Color): Lobby = {
    val l = new Lobby(mutable.Buffer[Player](), generateId, null)
    l.hostInfo = Option(hostName, hostColor)
    l
  }
}

class Lobby(val players: mutable.Buffer[Player], val id: String, var host: Player) {
  // add constructor id to HashSet
  Lobby.LobbyIds += id

  // temporary information for the host of the lobby as they get redirected
  // to the host lobby page
  private var hostInfo: Option[(String, Color)] = Option.empty

  def join(player: Player): Unit = {
    if (players.isEmpty) host = player
    players += player
  }

  /**
    * Used to make the host of the lobby officially join as a player, using their
    * previously collected information as well as their client id to construct a
    * Player object for them
    * @param id The client ID of the host
    * @return True if the function succeeded, false otherwise
    */
  def hostJoin(id: String): Boolean = {
    if (hostInfo.isEmpty) false
    else {
      join(Player(id, hostInfo.get._1, hostInfo.get._2))
      hostInfo = Option.empty
      true
    }
  }

  def close(): Unit = {
    // close the lobby
    Lobby.freeId(id)
  }
}
