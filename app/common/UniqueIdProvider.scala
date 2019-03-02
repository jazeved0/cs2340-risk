package common

import scala.collection.mutable

/**
  * Utility trait for managing a HashSet of unique String Ids
  * according to the given id generator function and related
  * matching predicate. The lifecycle of an Id is to be generated,
  * issued, consumed, and then returned
  *
  * Known implementors are Player and Game
  */
trait UniqueIdProvider {
  def idLength: Int
  protected def generateId(len: Int): String
  protected def isIdChar(c: Char): Boolean
  protected val Ids: mutable.HashSet[String] = new mutable.HashSet()
  protected def issueId(id: String) {
    Ids += id
  }

  def returnId(id: String) {
    Ids -= id
  }
  def generateAndIssueId: String = {
    var id = ""
    do id = generateId(idLength)
    while (Ids.contains(id))
    issueId(id)
    id
  }
  def isValidId(id: String): Boolean =
    id.length == idLength &&
    id.forall(isIdChar) &&
    this.contains(id)
  def contains(id: String): Boolean =
    Ids.contains(id)
}
