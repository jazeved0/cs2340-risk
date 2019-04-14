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

  def apply(name: String, color: Color): Player =
    ConcretePlayer(Some(PlayerSettings(name, Resources.Colors.indexOf(color))))
  def apply(playerSettings: Option[PlayerSettings]) = ConcretePlayer(playerSettings)
  def apply(): Player = NeutralPlayer()
}

/**
  * Serializable player DTO, doesn't include any server secrets
  * @param settings An option of player settings associated with this player object
  */
case class ConcretePlayer(override val settings: Option[PlayerSettings]) extends Player(settings) {
  /** Slower than comparing PlayerWithActors by their ID, only use when that
    * isn't available */
  override def equals(a: Any): Boolean = a match {
    // compares other settings option and this's settings option
    case other: ConcretePlayer => other.settings.exists(settings.contains)
    case _ => false
  }
  override def hashCode(): Int = ConcretePlayer.unapply(this).##
}

case class NeutralPlayer(override val settings: Option[PlayerSettings] = Some(PlayerSettings("Neutral", Resources.MaximumPlayers))) extends Player(settings) {
  override def equals(a: Any): Boolean = a match {
    case _: NeutralPlayer => true
    case _ => false
  }
}
