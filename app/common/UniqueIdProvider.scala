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
  * @tparam T The type of the Id (must be a sequence of Chars)
  */
trait UniqueIdProvider[T <: StringLike[_]] {
  /**
    * Virtual method to define how long each unique Id should be
    * (implement with val if possible)
    * @return The Id length, as an Int
    */
  def idLength: Int

  /**
    * Virtual method to generate the generic Id type given the target length
    * @param len The length of the Id to generate
    * @return A random Id (does not need to be unique, that comparison is
    *         performed after generation as necessary)
    */
  protected def generateId(len: Int): T

  /**
    * Virtual method to determine if the given Char value object is valid or not
    * in the Id
    * @param c The character to test
    * @return True of the character is a valid Id character, false otherwise
    */
  protected def isIdChar(c: Char): Boolean

  /**
    * Collection of each currently issued Id, HashSet to guarantee
    * amortized constant lookup
    */
  protected val Ids: mutable.HashSet[T] = new mutable.HashSet()

  /**
    * Issues a given Id and prevents it from being re-generated
    * @param id The Id to issue
    */
  protected def issueId(id: T): Unit = {
    Ids += id
  }

  /**
    * Returns a given Id and allows it to be re-generated
    * @param id The Id to return
    */
  def returnId(id: T): Unit = {
    Ids -= id
  }

  /**
    * Generates a unique Id and prevents it from being re-generated until it
    * gets returned
    * @return A unique Id object
    */
  def generateAndIssueId: T = {
    var id: Option[T] = None
    while (id.forall(Ids.contains)) {
      id = Some(generateId(idLength))
    }
    issueId(id.get)
    id.get
  }

  /**
    * Whether a given Id is valid or not (tests length, each character, and that
    * it was generated/issued and has not been returned)
    * @param id The Id to test
    * @return True if the Id is a valid Id in the current state, false otherwise
    */
  def isValidId(id: T): Boolean =
    id.length == idLength &&
      id.forall(isIdChar) &&
      Ids.contains(id)

  /**
    * Whether or not a given Id has currently been issued and not returned
    * @param id The Id to test
    * @return True if the Id is currently issued, false otherwise
    */
  def contains(id: T): Boolean =
    Ids.contains(id)
}
