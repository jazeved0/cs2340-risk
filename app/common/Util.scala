package common

import scala.util.Random

/**
  * Various utility functions used throughout the application
  */
object Util {
  private val AlphanumericChars: Seq[Char] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toList
  val Alphanumeric: List[Range] = {
    val CharValues = AlphanumericChars.map(c => c.toInt).toList

    @scala.annotation.tailrec
    def helper(list: List[Int], res: List[List[Int]]): List[List[Int]] = {
      list match {
        case a :: _ =>
          // If last element + 1 == current element
          if (res.head.head + 1 == a) {
            helper(list.tail, (a :: res.head) :: res.tail)
          } else {
            helper(list.tail, List(a) :: res)
          }
        case Nil => res
      }
    }

    helper(CharValues.tail, List(List(CharValues.head)))
      .reverse
      .map(list => Range(list.last, list.head))
  }

  def isAlphanumeric(c: Char): Boolean = Some(c)
    .map(c => c.toInt)
    .map(i => Alphanumeric.exists(r => i >= r.start && i <= r.end)).get

  def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

  def randomString(length: Int, from: Seq[Char]): String = Random.shuffle(from).take(length).mkString
}
