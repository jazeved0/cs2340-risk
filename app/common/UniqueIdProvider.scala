package common

import scala.collection.immutable.StringLike
import scala.collection.mutable

/**
  * Utility trait for managing a HashSet of unique String Ids
  * according to the given id generator function and related
  * matching predicate. The lifecycle of an Id is to be generated,
  * issued, consumed, and then returned
  *
  * Known implementors are Player and Game
  */
trait UniqueIdProvider[T <: StringLike[C], C] {
  def idLength: Int
  protected def generateId(len: Int): T
  protected def isIdChar(c: Char): Boolean
  protected val Ids: mutable.HashSet[T] = new mutable.HashSet()
  protected def issueId(id: T) {
    Ids += id
  }

  def returnId(id: T) {
    Ids -= id
  }

  def generateAndIssueId: T = {
    var id: T = {
      do id = generateId(idLength)
      while (Ids.contains(id))
      id
    }
    issueId(id)
    id
  }

  def isValidId(id: T): Boolean =
    id.length == idLength &&
      id.forall(isIdChar) &&
      Ids.contains(id)

  def contains(id: T): Boolean =
    Ids.contains(id)
}
