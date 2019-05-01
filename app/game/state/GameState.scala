package game.state

import actors.PlayerWithActor
import common.Pure
import game.Gameboard
import models._

/**
  * Immutable game state object used to hold all relevant information about
  * the state of a single Game instance
  *
  * @param turnOrder     The order that turns take place, by the player actor
  * @param playerStates  The state of each player, sorted by turn order
  * @param boardState    The state of each element on the board
  * @param gameboard     The gameboard DTO of the current game state
  * @param currentAttack The current attack of the game state (or None if not in
  *                      attack phase)
  * @param turn          The player index of the player whose turn it currently is
  */
case class GameState(turnOrder: IndexedSeq[PlayerWithActor],
                     playerStates: IndexedSeq[PlayerState],
                     boardState: IndexedSeq[TerritoryState],
                     gameboard: Gameboard,
                     currentAttack: Option[AttackState] = None,
                     turn: Int = 0) {
  @Pure
  def isEmpty: Boolean = this.size == 0

  @Pure
  def currentPlayer: PlayerWithActor = turnOrder(turn)

  @Pure
  def nextPlayer: PlayerWithActor = turnOrder(nextTurn)

  @Pure
  def size: Int = turnOrder.length

  @Pure
  def nextTurn: Int = (turn + 1) % size

  /**
    * Returns whether or not the game is currently in a Defense phase
    */
  @Pure
  def inBattle: Boolean = currentAttack.isDefined

  /**
    * Gets the state of the given player, or None if it was not found
    *
    * @param player The target player to get the state for
    * @return A PlayerState option representing the current player state of the
    *         given player if they were found within the turn order
    */
  @Pure
  def stateOf(player: Player): Option[PlayerState] = turnOrder
    .indexWhere(pa => pa.player == player) match {
    case -1 => None
    case index => playerStates.lift(index)
  }

  /**
    * Zips the board state with its indices and filters by territories with
    * armies owned by the specified player
    *
    * @param player The player to filter ownership by
    * @return A list of tuples giving (OwnedArmy, index)
    */
  @Pure
  def ownedByZipped(player: Player): IndexedSeq[(TerritoryState, Int)] =
    this.boardState.zipWithIndex.filter { case (territoryState, _) =>
      territoryState.owner == player
    }

  /**
    * Recalculates the proper turn upon a player disconnect
    * @param disconnectingActor The player with actor that is disconnecting
    * @return The new turn index that should be used to maintain valid order
    */
  @Pure
  def turnUponDisconnect(disconnectingActor: PlayerWithActor): Int = {
    turnOrder.indexOf(disconnectingActor) match {
      case -1         => turn // pass
      case playerTurn =>
        if (playerTurn < turn) {
          // move turn back
          turn - 1
        } else if (playerTurn == turn && turn == size - 1) {
          // move turn to start
          0
        } else {
          // turn unchanged
          turn
        }
    }
  }
}
