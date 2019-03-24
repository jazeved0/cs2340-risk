package game.state

import models.{Army, Player}

/**
  * PlayerState wrapper that defines the current game state of a single player,
  * including the armies they own and the state of their turn
  * @param player The player this state represents
  * @param units The total number of army tokens this player owns, wrapped as an Army
  * @param turnState The state of the player's turn
  */
case class PlayerState(player: Player, units: Army, turnState: TurnState)
