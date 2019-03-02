package models

import common.{Resources, UniqueIdProvider, Util}

object Player extends UniqueIdProvider {
  // Methods for UniqueIdProvider
  override def idLength: Int = Resources.PlayerIdLength
  override protected def generateId(len: Int): String = Util.randomString(len)
  override protected def isIdChar(c: Char): Boolean = Util.isAlphanumeric(c)

  def apply(id: String, name: String, color: Color): Player =
    Player(id, Some(PlayerSettings(name, Resources.Colors.indexOf(color))))
  def apply(id: String): Player = Player(id, None)
}

// player DTO
case class Player(id: String, settings: Option[PlayerSettings])
