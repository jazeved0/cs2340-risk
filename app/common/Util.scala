package common

import scala.util.Random

object Util {
  // adapted from the Open Location Code
  private val IdChars: Seq[Char] = "BCEFGHJMPQRTVYWX".toLowerCase.toList
  private val AlphanumericChars: Seq[Char] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toList
  val Alphanumeric: List[Range] = {
    def reduceRanges(ranges: List[Range]): List[Range] = {
      ranges match {
        case a :: b :: tail =>
          if(a.end == b.start)
            reduceRanges((a.start to b.end) :: tail)
          else
            a :: reduceRanges(b :: tail)
        case _ => ranges
      }
    }
    reduceRanges(AlphanumericChars
      .map(c => c.toInt)
      .sliding(2)
      .map(e => e.head to e(1))
      .filter(r => r.end == r.start + 1)
      .toList)
  }
  def isAlphanumeric(c: Char): Boolean = Some(c)
    .map(c => c.toInt)
    .map(i => Alphanumeric.exists(r => i >= r.start && i <= r.end)).get

  def randomString(length: Int): String = Random.alphanumeric.take(length).mkString
  def randomString(length: Int, from: Seq[Char]): String = Random.shuffle(from).take(length).mkString
  def randomId(length: Int): String = randomString(length, IdChars)
}
