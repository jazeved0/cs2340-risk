package game.state

import common.Pure
import models.{Army, NeutralPlayer, Player}

object TerritoryState {
  @Pure
  def apply(amount: Int): TerritoryState =
    TerritoryState(Army(amount), NeutralPlayer)

  @Pure
  def apply(amount: Int, owner: Player): TerritoryState =
    TerritoryState(Army(amount), owner)
}

/**
  * Represents the state of a single territory on the board
  *
  * @param army  The army that is on the territory (wrapper for Int)
  * @param owner The player option that owns the territory
  *              (can be "the" neutral player)
  */
case class TerritoryState(army: Army, owner: Player) {
  /**
    * Adds the number of army tokens to the board state
    *
    * @param other The other army to add
    * @return A new territory state with the owner preserved
    */
  def add(other: Army): TerritoryState = this.copy(army = army + other.size)

  /**
    * Whether the territory is a Neutral one (spawned as a result of player
    * disconnect)
    *
    * @return True if the territory is neutral, false otherwise
    */
  @Pure
  def isNeutral: Boolean = owner == NeutralPlayer

  /**
    * The size of the internal army
    *
    * @return The size, as an integer
    */
  @Pure
  def size: Int = army.size
}
