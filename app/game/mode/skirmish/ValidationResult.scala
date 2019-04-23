package game.mode.skirmish

import game.GameContext

object ValidationResult {
  /**
    * Utility factor creator to pass through the game context unchanged
    * @param result Whether the validation passed or not
    * @param gameContext Incoming context wrapping current game state
    * @return A context object wrapping the unmodified game context and the result
    */
  def apply(result: Boolean)(implicit gameContext: GameContext): ValidationResult =
    ValidationResult(result, gameContext)
}

/**
  * Method return type object that wraps the result of a validation step in the
  * packet processing pipeline
  * @param result Whether the validation passed or not
  * @param gameContext Incoming context wrapping current game state
  */
case class ValidationResult(result: Boolean, gameContext: GameContext)
