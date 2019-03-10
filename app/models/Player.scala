package models

import common.{Resources, UniqueIdProvider, Util}

object Player extends UniqueIdProvider {
  // Methods for UniqueIdProvider
  override val idLength: Int = Resources.PlayerIdLength
  override protected def generateId(len: Int): String = Util.randomString(len)
  override protected def isIdChar(c: Char): Boolean = Util.isAlphanumeric(c)

  def apply(name: String, color: Color): Player =
    Player(Some(PlayerSettings(name, Resources.Colors.indexOf(color))))
  def apply: Player = Player(None)
}

// player DTO
case class Player(settings: Option[PlayerSettings]) {
  // Slower than comparing PlayerWithActors by their ID, only use when that
  // isn't available
  override def equals(a: Any): Boolean = a match {
    case other: Player => other.settings.exists(settings.contains)
    case _ => false
  }
  override def hashCode(): Int = Player.unapply(this).##
}
