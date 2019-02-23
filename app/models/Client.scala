package models

import common.Util

import scala.collection.mutable

// player DTO
object Client {
  def apply(id: String, name: String, color: Color): Client = Client(id, Some(ClientSettings(name, color)))
  def apply(id: String): Client = Client(id, None)
  val IdLength = 16
  val Ids: mutable.HashSet[String] = new mutable.HashSet()
  def generateClientId: String = {
    var id = Util.randomString(IdLength)
    while (Ids.contains(id)) id = Util.randomString(IdLength)
    Ids += id
    id
  }
  def freeClientId(id: String): Unit = Ids -= id
  def isValidId(id: String): Boolean =
    id.length == IdLength &&
    id.forall(Util.isAlphanumeric) &&
    Ids.contains(id)
}

case class Client(id: String, settings: Option[ClientSettings])