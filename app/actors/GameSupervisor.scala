package actors

import actors.GameSupervisor.{GameExists, MakeGame}
import akka.actor.{Actor, ActorRef}
import controllers.InPacket
import models.PlayerSettings
import play.api.Logger

import scala.collection.mutable

object GameSupervisor {
  // internal messages from MainController
  case class GameExists(id: String)
  case class MakeGame(hostInfo: PlayerSettings)
}

/**
  * Root actor of the actor system that holds reference to every game that
  * has been generated
  */
class GameSupervisor extends Actor {
  val logger: Logger = Logger(this.getClass)
  val lobbies: mutable.HashMap[String, ActorRef] = mutable.HashMap[String, ActorRef]()

  override def receive: Receive = {

    // Internal message to poll lobby existence
    case GameExists(gameId) =>
      sender() ! lobbies.isDefinedAt(gameId)

      // Internal message to make a new Game actor given the host's PlayerSettings
    case MakeGame(hostInfo) =>
      val id = Game.generateAndIssueId
      val game = context.actorOf(Game(id, hostInfo))
      lobbies += id -> game
      sender() ! id

    // Any deserialized network packets getting sent to the game
    case inPacket: InPacket =>
      lobbies.get(inPacket.gameId).foreach(_ ! inPacket)

    case _ =>
  }
}
