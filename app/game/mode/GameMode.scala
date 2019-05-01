package game.mode

import actors.PlayerWithActor
import common.{Impure, Pure}
import controllers._
import game.GameContext
import game.state.GameState

import scala.util.Random

/**
  * Base trait for implementing a game mode (defines ways of processing incoming
  * packets and mutable state objects into internal mutations and outgoing
  * messages/packets)
  *
  * Uses Strategy design pattern
  */
trait GameMode {
  /**
    * Responsible for initializing a game upon start
    * The default packet order is as follows:
    *  1. Broadcast the chosen gameboard
    *  5. Broadcast the generated player state
    *  5. Broadcast the generated board state
    *  6. Broadcast and send any other OutPackets from the latent callbacks
    *
    * @param joinOrder The join order from the Game actor
    * @return An outgoing context wrapping the updated game state as well as any
    *         packets that will be sent as a result of initializing the game
    */
  @Impure.Nondeterministic
  def startGame(joinOrder: Seq[PlayerWithActor]): GameContext = {
    // Non-deterministic by default
    val turnOrder = assignTurnOrder(joinOrder)
    val state = createGameState(turnOrder)
    // Call initialize game hook
    val processedContext = hookInitializeGame(GameContext(state))
    GameContext(processedContext.state)
      .thenBroadcast(SendGameboard(processedContext.state.gameboard))
      .thenBroadcast(UpdatePlayerState(processedContext.state))
      .thenBroadcast(UpdateBoardState(processedContext.state))
      // Send all other packets spawned from the hook
      .thenSendAll(processedContext)
  }

  /**
    * Assigns turn order of each player based on their position of joining
    * (first player is the host)
    *
    * @param joinOrder The order by which players joined the lobby
    * @return A list of the same player with actor objects that may or may not
    *         be in the same order
    */
  @Impure.Nondeterministic
  def assignTurnOrder(joinOrder: Seq[PlayerWithActor]): IndexedSeq[PlayerWithActor] =
    Vector() ++ Random.shuffle(joinOrder)

  /**
    * Subclass hook that allows subclasses to register custom GameState
    * implementations to be used when initializing games.
    *
    * @param turnOrder The turn order of the game (sent from the Game actor)
    * @return A GameState object
    */
  @Pure
  def createGameState(turnOrder: IndexedSeq[PlayerWithActor]): GameState

  /**
    * Lifecycle hook for handling game state initialization
    *
    * @param context Incoming context wrapping current game state
    * @return An outgoing context wrapping the updated game state as well as any
    *         packets that will be sent as a result of handling the initialization
    */
  @Pure
  def hookInitializeGame(implicit context: GameContext): GameContext = pass

  /**
    * Lifecycle hook for handling an incoming packet
    *
    * @param packet  The packet instance (contains source player id)
    * @param context Incoming context wrapping current game state
    * @return An outgoing context wrapping the updated game state as well as any
    *         packets that will be sent as a result of handling the packet
    */
  @Pure
  def hookPacket(packet: InGamePacket)
                (implicit context: GameContext): GameContext = pass

  /**
    * Lifecycle hook for handling a player disconnect
    *
    * @param actor   The actor of the player that is leaving
    * @param context Incoming context wrapping current game state
    * @return An outgoing context wrapping the updated game state as well as any
    *         packets that will be sent as a result of handling the disconnect
    */
  @Pure
  def hookPlayerDisconnect(actor: PlayerWithActor)
                          (implicit context: GameContext): GameContext = pass

  /**
    * Default implementation of lifecycle hook that returns unmodified context
    *
    * @param context Incoming context wrapping current game state
    * @return The same context object
    */
  protected def pass(implicit context: GameContext): GameContext = context
}
