package common

import scala.collection.mutable
import scala.util.Random

object Util {
  // adapted from the Open Location Code
  private val IdChars: Seq[Char] = "BCEFGHJMPQRTVYWX".toLowerCase.toList

  def randomString(length: Int): String = Random.alphanumeric.take(length).mkString
  def randomString(length: Int, from: Seq[Char]): String = Random.shuffle(from).take(length).mkString
  def randomId(length: Int): String = randomString(length, IdChars)

  val ClientIdLength = 16
  val ClientIds: mutable.HashSet[String] = new mutable.HashSet()
  def generateClientId: String = {
    var id = Util.randomString(ClientIdLength)
    while (ClientIds.contains(id)) id = Util.randomString(ClientIdLength)
    ClientIds += id
    id
  }
  def freeClientId(id: String): Unit = ClientIds -= id
}
