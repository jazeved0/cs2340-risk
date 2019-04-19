package models

/**
  * DTO representing an army that is owned by a player (has colored tokens).
  * Fully serializable
 *
  * @param army The army object defining the size
  * @param owner The player object defining the color
  */
case class OwnedArmy(army: Army, owner: Player) {
  /**
    * Adds the amount onto an owned army, returning a new one with the same owner
    * @param amount The amount of army tokens to add
    * @return A new OwnedArmy object
    */
  // noinspection ScalaStyle
  def +(amount: Int): OwnedArmy = OwnedArmy(this.army + amount, this.owner)
}
