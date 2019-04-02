package game.mode

import actors.PlayerWithActor
import controllers.{InGamePacket, OutPacket, SendGameboard, UpdatePlayerState}
import game.Gameboard
import game.mode.GameMode._
import game.state.GameState

import scala.collection.mutable

object GameMode {
  /** Callback parameter wrapper */
  case class Callback(broadcast: (OutPacket, Option[String]) => Unit,
                      send: (OutPacket, String) => Unit) {
    def apply(payload: (OutPacket, Option[String]), flag: CallbackFlag): Unit = {
      flag match {
        case Broadcast => broadcast(payload._1, payload._2)
        case Send => send(payload._1, payload._2.getOrElse(""))
      }
    }
  }
  /** Used as a generic identifier for the callback function to use.
    * See <code>GameMode.initializeGame(...)</code> for a usage of these
    * to implement a latent callback feature */
  sealed trait CallbackFlag
  /** A entire-game-lobby broadcast, with the option to exclude one actor */
  case object Broadcast extends CallbackFlag
  /** A targeted message send */
  case object Send extends CallbackFlag
}

/**
  * Base trait for implementing a game mode (defines ways of processing incoming
  * packets and mutable state objects into internal mutations and outgoing
  * messages/packets)
  */
trait GameMode {
  /**
    * Responsible for generating a GameState, initializing it, and sending the gameboard and player state to the entire lobby.
    * The default behavior is as follows:
    *  1. Broadcast the proper gameboard
    *  2. Call assignTurnOrder to determine turn order
    *  3. Call makeGameState with the previously assigned turn order
    *  4. Call initializeGameState to perform any initialization actions
    *  5. Broadcast the generated player state
    *  6. Broadcast and send any other OutPackets from the latent callbacks
    * @param joinOrder The join order from the Game actor
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @return A newly generated GameState instance
    * @todo Refactor to use abstract type member
    */
  def initializeGame(joinOrder: List[PlayerWithActor], callback: Callback): GameState = {
    // Use latent sending callbacks to preserve send order
    val messageQueue: mutable.Queue[(CallbackFlag, (OutPacket, Option[String]))] = mutable.Queue()
    val latentCallback = Callback(
      (p: OutPacket, s: Option[String]) => messageQueue += ((Broadcast, (p, s))),
      (p: OutPacket, s: String) => messageQueue += ((Send, (p, Some(s))))
    )

    callback.broadcast(SendGameboard(gameboard), None)
    val turnOrder: Seq[PlayerWithActor] = assignTurnOrder(joinOrder)
    val gameState = makeGameState(turnOrder)
    initializeGameState(latentCallback)(gameState)
    callback.broadcast(UpdatePlayerState(gameState), None)

    // Consume earlier built latent callbacks
    messageQueue.foreach { case (flag, payload) => callback(payload, flag) }
    gameState
  }

  /** Abstract getter (implement with lazy val if possible) */
  def gameboard: Gameboard

  /**
    * Assigns turn order by re-ordering the join order list
    * @param players The join order of the players in the game lobby (actor list)
    * @return A newly ordered list of the same size as the original one
    */
  def assignTurnOrder(players: Seq[PlayerWithActor]): Seq[PlayerWithActor]

  /**
    * Initializes the game state (called once) to perform any additional logic.
    * Note: any packets sent to the callback object will be sent '''after''' the
    * gameboard and player state have been broadcast to the lobby
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param state The GameState context object
    */
  def initializeGameState(callback: Callback)(implicit state: GameState): Unit

  /**
    * Handles incoming network packets that have been routed from the Game actor
    * @param packet The packet instance (contains source player id)
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param state The GameState context object
    */
  def handlePacket(packet: InGamePacket, callback: Callback)(implicit state: GameState): Unit

  /**
    * Handles the disconnect of a player from the game and performs any necessary
    * logic upon the disconnection
    * @param player The player actor that is being disconnected
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param state The GameState context object
    */
  def playerDisconnect(player: PlayerWithActor, callback: Callback)(implicit state: GameState): Unit

  /**
    * Subclass hook that allows subclasses to register custom GameState implementations
    * to be used when initializing games.
    * @param turnOrder The turn order of the game (sent from the Game actor)
    * @return A GameState object
    */
  def makeGameState(turnOrder: Seq[PlayerWithActor]): GameState
}
