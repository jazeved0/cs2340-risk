package game.state

import actors.PlayerWithActor
import common.Util
import game.state.TurnState.Idle
import models.{Army, OwnedArmy, Player}

import scala.collection.mutable

/**
  * Mutable game state object used to hold all relevant information about
  * the state of a single Game instance
  *
  * Internally accessible, not serialized (can use ActorRefs and IDs), only
  * partially serialized (see <code>UpdateBoardState</code> and
  * <code>UpdatePlayerState</code> for examples of partial serializations)
  */
class GameState(private var _turnOrder: Seq[PlayerWithActor], territories: Int) {
  /** mutable/growable buffer containing (in turn order) every player's state */
  var playerStates: mutable.Buffer[PlayerState] = Util.buffer(
    _turnOrder.map(actor => PlayerState(actor.player, Army(0), TurnState(Idle))))
  /** mutable (but not growable) array containing every owned army (by territory index) */
  var boardState: Array[Option[OwnedArmy]] = Array.fill(territories)(None)
  /** The current turn index */
  var turn = 0

  /**
    * The size of the game (number of players)
    * @return The game size, as an Int
    */
  def gameSize: Int = _turnOrder.length

  /**
    * Getter for turn order
    * @return A list of PlayerWithActors in the order of the turn order
    */
  def turnOrder: Seq[PlayerWithActor] = _turnOrder

  /**
    * Setter for turn order that updates the player states buffer accordingly
    * @param newOrder The new turn order
    */
  // noinspection ScalaStyle
  def turnOrder_=(newOrder: Seq[PlayerWithActor]): Unit = {
    // Update playerStates
    val oldStates = _turnOrder.zipWithIndex.toMap
    playerStates = Util.buffer(newOrder.map(actor => playerStates(oldStates(actor))))
    _turnOrder = newOrder
  }

  /**
    * Finds the state for the corresponding player
    * @param player The player object
    * @return An option giving Some(PlayerState) if successful, or None otherwise
    */
  def stateOf(player: Player): Option[PlayerState] =
    playerStates.find(p => p.player == player)

  /**
    * Determines whether the given player is within the turn substate
    * @param player The player object
    * @param state The turn state machine enum case object
    * @return True if the player is in the substate, false otherwise
    */
  def isInState(player: Player, state: TurnState.State): Boolean =
    stateOf(player).exists(ps => ps.turnState.state == state)
}
