package game.state

import actors.PlayerWithActor
import common.{Pure, Util}
import game.Gameboard
import game.state.TurnState.Idle
import models._

import scala.collection.mutable

/**
  * Immutable game state object used to hold all relevant information about
  * the state of a single Game instance
  * @param turnOrder The order that turns take place, by the player actor
  * @param playerStates The state of each player, sorted by turn order
  * @param boardState The state of each element on the board
  * @param gameboard The gameboard DTO of the current game state
  */
case class GameState(turnOrder: IndexedSeq[PlayerWithActor],
                     playerStates: IndexedSeq[PlayerState],
                     boardState: IndexedSeq[TerritoryState],
                     gameboard: Gameboard) {
  @Pure
  def size: Int = turnOrder.length

  /**
    * Zips the board state with its indices and filters by territories with
    * armies owned by the specified player
    * @param player The player to filter ownership by
    * @return A list of tuples giving (OwnedArmy, index)
    */
  @Pure
  def ownedByZipped(player: Player): IndexedSeq[(TerritoryState, Int)] =
    this.boardState.zipWithIndex.filter { case (territoryState, _) =>
      territoryState.owner == player
    }

  /**
    * Recreates the game state with updated board state
    * @param newBoardState The new board state to use
    * @return A new GameState object with everything other than board
    *         state untouched
    */
  @Pure
  def withBoardState(newBoardState: IndexedSeq[TerritoryState]): GameState =
    this.copy(boardState = newBoardState)

  /**
    * Recreates the game state with updated player states
    * @param newPlayerStates The new player states to use
    * @return A new GameState object with everything other than player
    *         states untouched
    */
  @Pure
  def withPlayerStates(newPlayerStates: IndexedSeq[PlayerState]): GameState =
    this.copy(playerStates = newPlayerStates)
}





// TODO refactor to this class, make immutable
/**
  * Mutable game state object used to hold all relevant information about
  * the state of a single Game instance
  *
  * Internally accessible, not serialized (can use ActorRefs and IDs), only
  * partially serialized (see <code>UpdateBoardState</code> and
  * <code>UpdatePlayerState</code> for examples of partial serializations)
  */
class GameState2(private var _turnOrder: Seq[PlayerWithActor], territories: Int) {
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
      val player = defendingPlayer.get
      stateOf(player).map(_.turnState.advanceDefenseState(payload:_*)).foreach {
        nextState => this (player) = constructPlayerState(player, nextState)
      }
    } else {
      clearPayloads()
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
  def clearPayloads(): Unit = {
    playerStates.foreach{
      playerState => {
        val player = playerState.player
        this(player) = constructPlayerState(player, playerState.turnState.clearPayload())
      }
    }
  }
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

  /**
    * Zips the board state with its indices and filters by territories with
    * armies owned by the specified player
    * @param player The player to filter ownership by
    * @return A list of tuples giving (OwnedArmy, index)
    */
  def ownedByZipped(player: Player): Seq[(OwnedArmy, Int)] =
    this.boardState.zipWithIndex.filter { case (oaOption, _) =>
      oaOption match {
        case Some(ownedArmy) => ownedArmy.owner == player
        case _ => false
      }
    }.map { case (oaOption, index) => (oaOption.get, index) }
}
