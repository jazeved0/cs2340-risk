package common

import akka.parboiled2.util.Base64

import scala.collection.immutable.Queue
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.Random

/**
  * Various utility functions used throughout the application
  */
object Util {
  /** Set of characters A-Z, a-z, and 0-9 that can be used for fast lookup */
  val AlphanumericChars: Set[Int] =
    FastIntSet.fromChar(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9'))
  /** Set of URL-safe base 64 characters that can be used for fast lookup */
  val Base64Chars: Set[Int] = FastIntSet(Base64.custom.getAlphabet)

  /**
    * Collects a sorted list into its contiguous runs and then creates a list of
    * inclusive intervals from those runs (which may be of length 1)
    * @param list The source list
    * @param num The numeric information for the type
    * @tparam T The type of element in the list
    * @return A list of tuples defining the inclusive intervals that fully
    *         span the input set
    */
  def collectToRanges[T](list: Seq[T])(implicit num: Numeric[T]): Seq[(T, T)] = {
    @scala.annotation.tailrec
    def helper(list: List[T], res: List[List[T]]): List[List[T]] = {
      import num._
      list match {
        case a :: _ =>
          // If last element + 1 == current element
          if (res.head.head + num.one == a) {
            helper(list.tail, (a :: res.head) :: res.tail)
          } else {
            helper(list.tail, List(a) :: res)
          }
        case Nil => res
      }
    }
    helper(list.tail.toList, List(List(list.head)))
      .reverse
      .map(list => (list.last, list.head))
  }

  /**
    * Generates a random alphanumeric string of the desired length
    * @param length The length of the new string to generate
    * @return The generated string
    */
  def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

  /**
    * Generates a random string of the desired length
    * @param length The length of the new string to generate
    * @param from The pool of characters to pull from (equal chance of sampling)
    * @return The generated string
    */
  def randomString(length: Int, from: Seq[Char]): String =
    Random.shuffle(from).take(length).mkString

  /**
    * Removes an element from an immutable list
    * @param elem The element to remove
    * @param list The original list
    * @tparam A The type of elements in the list
    * @tparam B The type of element to remove (may be a subtype due to polymorphism)
    * @return A new list without the target item
    */
  def remove[A, B >: A](elem: B, list: Seq[A]): Seq[A] = list diff List(elem)

  /**
    * Removes an element from an immutable IndexedSeq
    * @param elem The element to remove
    * @param list The original list
    * @tparam A The type of elements in the list
    * @tparam B The type of element to remove (may be a subtype due to polymorphism)
    * @return A new list without the target item
    */
  def remove[A, B >: A](elem: B, list: IndexedSeq[A]): IndexedSeq[A] = list diff Vector(elem)

  /**
    * Factory method for a mutable ListBuffer given an initial collection
    * @param traversableOnce The source collection
    * @tparam T The type of element to contain in the collection
    * @return A new collection instance
    */
  def listBuffer[T](traversableOnce: TraversableOnce[T]): ListBuffer[T] =
    mutable.ListBuffer[T]() ++ traversableOnce

  /**
    * Factory method for a mutable Buffer given an initial collection
    * (defaults to an ArrayBuffer according to the collections API)
    * @param traversableOnce The source collection
    * @tparam T The type of element to contain in the collection
    * @return A new collection instance
    */
  def buffer[T](traversableOnce: TraversableOnce[T]): mutable.Buffer[T] =
    mutable.Buffer[T]() ++ traversableOnce

  /**
    * Factory method for a mutable ArrayBuffer given an initial collection
    * @param traversableOnce The source collection
    * @tparam T The type of element to contain in the collection
    * @return A new collection instance
    */
  def arrayBuffer[T](traversableOnce: TraversableOnce[T]): ArrayBuffer[T] =
    mutable.ArrayBuffer[T]() ++ traversableOnce

  /**
    * Performs a breadth first search on the graph specified by the start node
    * and the node -> edge mapping function
    *
    * @param start    The node to start at
    * @param getEdges A node -> edge list mapping function that defines the graph
    * @tparam Node    The type of each Node
    * @return A stream providing the traversal
    */
  def bfs[Node](start: Node, getEdges: Node => Queue[Node]): Stream[Node] = {
    def rBfs(edges: Queue[Node]): Stream[Node] = edges match {
      case node +: tail => node #:: rBfs(tail ++ getEdges(node))
      case _            => Stream.Empty
    }
    start #:: rBfs(getEdges(start))
  }
}
