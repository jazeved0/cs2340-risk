package models

import common.{Resources, UniqueIdProvider, Util}

object Player extends UniqueIdProvider {
  // Methods for UniqueIdProvider
  override def idLength: Int = Resources.PlayerIdLength
  override protected def generateId(len: Int): String = Util.randomString(len)
  override protected def isIdChar(c: Char): Boolean = Util.isAlphanumeric(c)

  def apply(name: String, color: Color): Player =
    Player(Some(PlayerSettings(name, Resources.Colors.indexOf(color))))
  def apply: Player = Player(None)
}

// player DTO
case class Player(settings: Option[PlayerSettings])
