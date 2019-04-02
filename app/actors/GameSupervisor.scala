package actors

import actors.Game.{CanBeHosted, CanBeJoined}
import actors.GameSupervisor.{CanHost, CanJoin, MakeGame}
import akka.actor.{Actor, ActorRef}
import controllers.InPacket
import models.PlayerSettings

import scala.collection.mutable

object GameSupervisor {

  // internal messages from MainController
  /** Ask message whether the game with the given Id exists and return a Boolean */
  case class GameExists(id: String)
  /** Ask message to make the game and return the Id created */
  case class MakeGame(hostInfo: PlayerSettings)
  /** Ask message whether the game at the Id can be hosted and return a CanHost enum */
  case class CanHost(id: String)
  /** Ask message whether the game at the Id can be joined and return a CanJoin
    * enum (does not validate game size at this stage) */
  case class CanJoin(id: String)

  /**
    * Response value for the CanHost flag message:
    *
    * Yes: Host has not joined, game Id is valid, game has not started, (yes)
    *  InvalidId: game Id is invalid, (no)
    *  Hosted: Game has already been hosted, (no)
    *  Started: Game has already started (no)
    */
  object CanHost extends Enumeration {
    type CanHost = Value
    val Yes, InvalidId, Hosted, Started = Value
  }

  /**
    * Response value for the CanJoin flag message:
    *
    * Yes: game Id is valid, game has not started, (yes)
    *  InvalidId: game Id is invalid, (no)
    *  Started: Game has already started (no)
    */
  object CanJoin extends Enumeration {
    type CanJoin = Value
    val Yes, InvalidId, Started = Value
  }
}

/**
  * Root actor of the actor system that holds reference to every game that
  * has been generated, responsible for handling internal messages about the
  * state of the Game actor subsystem as well as forwarding incoming
  * network packets to the proper Game actor (resolved by game Id)
  */
class GameSupervisor extends Actor {
  /** Map of game Id -> indirect reference to Game actors */
  val games: mutable.HashMap[String, ActorRef] = mutable.HashMap[String, ActorRef]()

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

    case _ => // no op
  }
}
