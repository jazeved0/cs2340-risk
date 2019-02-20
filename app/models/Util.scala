package models

import scala.collection.mutable

object Util {
  def randomString(length: Int): String = {
    // make a new Random using the current time as its seed
    val r = new scala.util.Random(System.currentTimeMillis())
    // return a random string of alphanumeric characters
    r.alphanumeric.take(length).mkString
  }

  val ClientIdLength = 16
  val ClientIds: mutable.HashSet[String] = new mutable.HashSet()
  def generateClientID: String = {
    var id = Util.randomString(ClientIdLength)
    while (!ClientIds.contains(id)) id = Util.randomString(ClientIdLength)
    id
  }
  def freeClientID(id: String): Unit = ClientIds -= id
}
