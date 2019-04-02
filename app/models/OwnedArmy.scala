package models

/**
  * DTO representing an army that is owned by a player (has colored tokens).
  * Fully serializable
 *
  * @param army The army object defining the size
  * @param owner The player object defining the color
  */
case class OwnedArmy(army: Army, owner: Player)
