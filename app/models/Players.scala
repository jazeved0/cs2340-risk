package models

import common.{Resources, UniqueIdProvider, Util}

import scala.collection.immutable.WrappedString

sealed abstract class Player(val settings: Option[PlayerSettings])

object Player extends UniqueIdProvider[WrappedString] {
  // Methods for UniqueIdProvider
  override val idLength: Int = Resources.PlayerIdLength
  override protected def generateId(len: Int): WrappedString =
    Util.randomString(len, Util.Base64Chars.toList.map(i => i.toChar))
  override protected def isIdChar(c: Char): Boolean = Util.Base64Chars.contains(c)

  // Factory methods
  def apply(name: String, color: Color): Player =
    ConcretePlayer(Some(PlayerSettings(name, Resources.Colors.indexOf(color))))

  def apply(settings: PlayerSettings): Player =
    ConcretePlayer(Some(settings))

  def apply(playerSettings: Option[PlayerSettings]): Player =
    ConcretePlayer(playerSettings)

  def apply(): Player = NeutralPlayer
}

/**
  * Serializable player DTO, doesn't include any server secrets
  * @param settingsOption An option of player settings associated with this player object
  */
case class ConcretePlayer(settingsOption: Option[PlayerSettings]) extends Player(settingsOption) {
  /** Slower than comparing PlayerWithActors by their ID, only use when that
    * isn't available */
  override def equals(a: Any): Boolean = a match {
    // compares other settings option and this's settings option
    case ConcretePlayer(Some(s)) => settings.contains(s)
    case _ => false
  }
  override def hashCode(): Int = ConcretePlayer.unapply(this).##
}

case object NeutralPlayer extends Player(Some(PlayerSettings("_neutral", Int.MinValue))) {
  override def equals(a: Any): Boolean = a match {
    case p: Player => p.settings.contains(this.settings.get)
    case _         => false
  }
  override def hashCode(): Int = settings.hashCode
}
