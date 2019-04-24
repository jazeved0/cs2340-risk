package game.mode.skirmish

import actors.PlayerWithActor
import common.Pure
import game.GameContext
import game.state.{PlayerState, TurnState}

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
        // Apply a mapping function to each player's state
        val playerStateProcessor: PlayerState => PlayerState =
          currentState.turnState.advanceState() match {
            // Advance the current one to idle and then the next one to reinforcement
            case TurnState(TurnState.Idle, _) =>
              val nextState = PlayerStateHandler.reinforcement(next.player)(context)
              advanceAndInitialize(current, next, nextState)
            // Advance the current one to whatever is next
            case state =>
              advance(current, state)
          }
        context.map(gs => gs.copy(
          playerStates = gs.playerStates.map(playerStateProcessor),
          turn         = gs.nextTurn
        ))
      case None => context // pass
    }
  }

  /**
    * Partially applied processing function that advances the state of a single
    * player in the turn order
    * @param current The target player to update
    * @param toState The turn state to replace their current one with
    * @param process The un-curried parameter used to map all player states
    * @return A mapping result depending on the input un-curried parameter
    */
  @Pure
  def advance(current: PlayerWithActor, toState: TurnState)
             (process: PlayerState): PlayerState = {
    val CurrentPlayer = current.player
    process.player match {
      case CurrentPlayer => process.copy(turnState = toState)
      case _             => process
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
}
