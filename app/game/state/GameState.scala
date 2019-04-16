package game.state

import actors.PlayerWithActor
import common.Util
import game.state.TurnState.Idle
import models._

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
  /** The current attack; used during the Defense phase of the receiving player */
  var currentAttack: Option[Seq[Int]] = None

  /**
    * The size of the game (number of players)
    * @return The game size, as an Int
    */
  def gameSize: Int = _turnOrder.length

  /**
    * Returns whether or not the game is currently in a Defense phase
    */
  def isInDefense: Boolean = currentAttack.isDefined

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
    * Advances the turn state of the current player, optionally moving the turn
    * along according to the state machine
    * @param defendingPlayer player object representing the player of defending territory;
    *                        relevant during the attack phase
    */
  def advanceTurnState(defendingPlayer: Option[Player], payload: (String, Any)*): Unit = {
    if (defendingPlayer.isDefined) {
      defendingPlayer.get match {
        case _: ConcretePlayer => {
          val player = defendingPlayer.get
          stateOf(player).map(_.turnState.advanceDefenseState(payload:_*)).foreach {
            nextState => this (player) = constructPlayerState(player, nextState)
          }
        }
        case _: NeutralPlayer => {
          val player = currentPlayer
          stateOf(player).map(_.turnState).foreach {
            nextState => this (player) = constructPlayerState(player, TurnState(nextState.state, payload:_*))
          }
        }
      }
    } else {
      stateOf(currentPlayer).map(_.turnState.advanceState(payload:_*)).foreach {
        nextState => {
          this(currentPlayer) = constructPlayerState(currentPlayer, nextState)
          if (nextState.state == TurnState.Idle) {
            advanceTurn()
            this(currentPlayer) = constructPlayerState(currentPlayer, nextState.advanceState(payload:_*))
          }
        }
      }
    }
  }

  def currentPlayer: Player = turnOrder(turn).player
  def advanceTurn(): Unit = turn = (turn + 1) % gameSize
  def modifyTurnAfterDisconnecting(playerTurn: Int): Unit = {
    if (playerTurn < turn) {
      turn -= 1
    } else if (playerTurn == turn && turn == gameSize - 1) {
      turn = 0
    }
  }
  def constructPlayerState(player: Player, turnState: TurnState): PlayerState =
    PlayerState(player, stateOf(player).get.units, turnState)

  /**
    * Determines whether the given player is within the turn substate
    * @param player The player object
    * @param state The turn state machine enum case object
    * @return True if the player is in the substate, false otherwise
    */
  def isInState(player: Player, state: TurnState.State): Boolean =
    stateOf(player).exists(ps => ps.turnState.state == state)

  /**
    * Updates the player state internal collection
    * @param player The player key
    * @param newState The new state
    */
  def update(player: Player, newState: PlayerState): Unit = {
    playerStates.indexWhere(ps => ps.player == player) match {
      case -1 =>
      case i => playerStates(i) = newState
    }
  }
}
