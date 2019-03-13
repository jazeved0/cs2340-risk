package game

import models.Player

// owned army DTO
case class OwnedArmy(army: Army, owner: Player)
