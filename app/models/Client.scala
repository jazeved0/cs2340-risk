package models

import common.{Resources, UniqueIdProvider, Util}

object Client extends UniqueIdProvider {
  // Methods for UniqueIdProvider
  override def idLength = 16
  override protected def generateId(len: Int): String = Util.randomString(len)
  override protected def isIdChar(c: Char): Boolean = Util.isAlphanumeric(c)

  def apply(id: String, name: String, color: Color): Client =
    Client(id, Some(ClientSettings(name, Resources.Colors.indexOf(color))))
  def apply(id: String): Client = Client(id, None)
}

// client DTO
case class Client(id: String, settings: Option[ClientSettings])