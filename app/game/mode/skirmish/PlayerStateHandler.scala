package game.mode.skirmish

import common.{Pure, Resources}
import game.GameContext
import game.state.TurnState
import game.state.TurnState.Reinforcement
import models.Player

/**
  * Sub-object of SkirmishGameMode that handles the player state machine
  */
object PlayerStateHandler {
  /**
    * Utility method that creates a reinforcement state machine object and
    * calculates the reinforcement allocation as necessary
    * @param player The player to use to calculate reinforcements
    * @param context Incoming context wrapping current game state
    * @return A new TurnState object for Reinforcement State containing the
    *         calculated allocation
    */
  @Pure
  def reinforcement(player: Player)(implicit context: GameContext): TurnState =
    TurnState(Reinforcement, "amount" -> calculateReinforcement(player))

  /**
    * Performs the calculation logic according to values injected from Resources
    * for the target player
    * @param player The player to calculate reinforcements for
    * @param context Incoming context wrapping current game state
    * @return The number of reinforcements the player should receive, as an Int
    */
  @Pure
  def calculateReinforcement(player: Player)(implicit context: GameContext): Int = {
    val conquered = context.state.ownedByZipped(player)
    val gameboard = context.state.gameboard
    val territories = conquered.length
    val castles = conquered.count { case (_, index) => gameboard.hasCastle(index) }
    val base = Resources.SkirmishReinforcementBase
    val divisor = Resources.SkirmishReinforcementDivisor
    // Calculate according to the formula max(floor(territories + castles) / 3), 3)
    Math.max(Math.floor((territories + castles) / divisor.toDouble), base.toDouble).toInt
  }
}
