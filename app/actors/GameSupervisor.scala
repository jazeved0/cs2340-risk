package actors

import actors.Game.{CanBeHosted, CanBeJoined}
import actors.GameSupervisor.{CanHost, CanJoin, MakeGame}
import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import controllers.InPacket
import models.PlayerSettings

import scala.collection.mutable
import scala.concurrent.duration._

object GameSupervisor {

  // internal messages from MainController
  case class GameExists(id: String)
  case class MakeGame(hostInfo: PlayerSettings)
  case class CanHost(id: String)
  case class CanJoin(id: String)

  object CanHost extends Enumeration {
    type CanHost = Value
    val Yes, InvalidId, Hosted, Started = Value
  }

  object CanJoin extends Enumeration {
    type CanJoin = Value
    val Yes, InvalidId, Started = Value
  }
}

/**
  * Root actor of the actor system that holds reference to every game that
  * has been generated
  */
class GameSupervisor extends Actor {
  val games: mutable.HashMap[String, ActorRef] = mutable.HashMap[String, ActorRef]()
  implicit val timeout: Timeout = 5.seconds

  override def receive: Receive = {

    // Internal message to make a new Game actor given the host's PlayerSettings
    case MakeGame(hostInfo) =>
      val id = Game.generateAndIssueId
      val game = context.actorOf(Game(id, hostInfo))
      games += id.toString -> game
      sender() ! id.toString

    // Internal message to poll game state
    case CanJoin(gameId) =>
      if (games.isDefinedAt(gameId)) {
        games(gameId) forward CanBeJoined()
      } else {
        sender() ! CanJoin.InvalidId
      }

    // Internal message to determine whether the lobby can be hosted
    case CanHost(gameId) =>
      if (games.isDefinedAt(gameId)) {
        games(gameId) forward CanBeHosted()
      } else {
        sender() ! CanHost.InvalidId
      }

    // Any deserialized network packets getting sent to the game
    case inPacket: InPacket =>
      games.get(inPacket.gameId).foreach(_ ! inPacket)

    case _ =>
  }
}
