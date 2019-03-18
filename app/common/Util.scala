package common

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Various utility functions used throughout the application
  */
object Util {
  private val AlphanumericChars: Seq[Char] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toList
  val Alphanumeric: List[Range] = {
    val charValues = AlphanumericChars.map(c => c.toInt).toList

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

    helper(charValues.tail, List(List(charValues.head)))
      .reverse
      .map(list => Range(list.last, list.head))
  }

  def isAlphanumeric(c: Char): Boolean = Some(c)
    .map(c => c.toInt)
    .map(i => Alphanumeric.exists(r => i >= r.start && i <= r.end)).get

  def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

  def randomString(length: Int, from: Seq[Char]): String = Random.shuffle(from).take(length).mkString

  def remove[A, B >: A](elem: B, list: Seq[A]): Seq[A] = list diff List(elem)

  def listBuffer[B](traversableOnce: TraversableOnce[B]): ListBuffer[B] = mutable.ListBuffer[B]() ++ traversableOnce

  def buffer[B](traversableOnce: TraversableOnce[B]): mutable.Buffer[B] = mutable.Buffer[B]() ++ traversableOnce
}
