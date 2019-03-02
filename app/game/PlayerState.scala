package game

import models.PlayerSettings

case class PlayerState(settings: PlayerSettings, units: Army)
