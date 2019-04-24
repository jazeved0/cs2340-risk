package game.mode.skirmish

import actors.PlayerWithActor
import common.Pure
import controllers.{UpdateBoardState, UpdatePlayerState}
import game.GameContext
import game.state.{PlayerState, TurnState}
import models.{Army, Player}

object SkirmishGameContext {
  /**
    * Extends the default context object to provide contextual extensions specific
    * to SkirmishGameMode
    *
    * @param context Incoming context wrapping current game state
    * @return An extended context object allowing for additional methods to be used
    */
  implicit def extendContext(context: GameContext): SkirmishGameContext =
    new SkirmishGameContext(context)
}

/**
  * Companion context extension class
  * @param context Incoming context to be wrapped
  */
class SkirmishGameContext(context: GameContext) {
  @Pure
  def advanceTurnState: GameContext = {
    val current = context.state.currentPlayer
    val next    = context.state.nextPlayer
    context.state.stateOf(current.player) match {
      case Some(currentState) =>
        currentState.turnState.advanceState() match {
          // Advance the current one to idle and then the next one to reinforcement
          case TurnState(TurnState.Idle, _) =>
            val nextState = PlayerStateHandler.reinforcement(next.player)(context)
            context.map(gs => gs.copy(
              playerStates = gs.playerStates.map(advanceAndInitialize(current, next, nextState)),
              turn = gs.nextTurn
            ))

          // Advance the current one to whatever is next
          case state =>
            context.updatePlayerState(currentState.copy(turnState = state))
        }

      case None => context // pass
    }
  }

  /**
    * Partially applied processing function that advances a player to Idle state
    * and then advances the next one in the turn order to the next state
    * @param current The target player to make idle
    * @param next The target player to initialize
    * @param initializeWith The turn state to replace the next player's one with
    * @param process The un-curried parameter used to map all player states
    * @return A mapping result depending on the input un-curried parameter
    */
  @Pure
  def advanceAndInitialize(current: PlayerWithActor, next: PlayerWithActor,
                           initializeWith: TurnState)(process: PlayerState): PlayerState = {
    val CurrentPlayer = current.player
    val NextPlayer    = next.player
    process.player match {
      case CurrentPlayer => process.copy(turnState = TurnState(TurnState.Idle))
      case NextPlayer    => process.copy(turnState = initializeWith)
      case _             => process
    }
  }

  /**
    * Advances the turn state of the defending player during the defense/attack phase
    * @param defender The player that will be defending
    * @param payload Any payload arguments to add to the newly constructed player state
    * @return An updated game context object
    */
  @Pure
  def advanceAttackTurnState(defender: Player, payload: (String, Any)*): GameContext = {
    context.state.stateOf(defender) match {
      case Some(currentState) =>
        val nextState = currentState.turnState.advanceDefenseState(payload:_*)
        val nextPlayerState = constructPlayerState(defender, nextState)
        context.updatePlayerState(nextPlayerState)
      case None => context // pass
    }
  }

  /**
    * Constructs a player state object given the arguments
    * @param player The player to make a player state for
    * @param turnState The target turn state to use
    * @return A new player state object
    */
  def constructPlayerState(player: Player, turnState: TurnState): PlayerState =
    context.state.stateOf(player) match {
      case Some(state) => PlayerState(player, state.units, turnState)
      case None        => PlayerState(player, Army.Empty,  turnState)
    }

  /**
    * Helper method to broadcast board state based on the current context
    * @return An updated GameContext
    */
  @Pure
  def thenBroadcastBoardState: GameContext =
    context.thenBroadcast(UpdateBoardState(context.state))

  /**
    * Helper method to broadcast player state based on the current context
    * @return An updated GameContext
    */
  @Pure
  def thenBroadcastPlayerState: GameContext =
    context.thenBroadcast(UpdatePlayerState(context.state))

  /**
    * Helper method to broadcast board state based on the current context
    * @param idExclude A player Id to exclude from the broadcast
    * @return An updated GameContext
    */
  @Pure
  def thenBroadcastBoardState(idExclude: String = ""): GameContext =
    context.thenBroadcast(UpdateBoardState(context.state), idExclude)

  /**
    * Helper method to broadcast player state based on the current context
    * @param idExclude A player Id to exclude from the broadcast
    * @return An updated GameContext
    */
  @Pure
  def thenBroadcastPlayerState(idExclude: String = ""): GameContext =
    context.thenBroadcast(UpdatePlayerState(context.state), idExclude)
}
