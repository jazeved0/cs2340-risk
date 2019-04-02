package common

import scala.collection.mutable

/**
  * Utility trait for managing a sequence of HashSets of case class
  * or tuple values in order to provide a way to ensure full uniqueness
  * among a collection of the target type T. The internal collection can
  * be polled to determine if the given instance is unique, be added to,
  * and be polled to give a sequence of the non-unique elements of the
  * given Product instance.
  *
  * Known implementors are Game[PlayerSettings]
  * @tparam T The type of the value to manage (should be a case class/product)
  */
trait UniqueValueManager[T <: Product] {
  /**
    * The current list of value HashSets, ordered by product order and
    * encapsulated within an Option if not initialized. Uses HashSets of type
    * Any for guaranteed amortized constant lookup
    * */
  protected var values: Option[List[mutable.HashSet[Any]]] = None

  /**
    * Whether the given managed value is fully unique (all fields are unique)
    * @param t The value to test
    * @return True if and only if all product fields are unique, false otherwise
    */
  def isUnique(t: T): Boolean = {
    if (values.isEmpty) {
      true
    } else if ((values.get.iterator zip t.productIterator)
      .exists { case (set, v) => set.contains(v) }) {
      false
    } else {
      true
    }
  }

  /**
    * Adds a unique value to the list of currently used values, ensuring no other
    * filtered products have any of its same fields
    * @param t The element to add to the collection of unique fields
    */
  def add(t: T): Unit = {
    if (values.isEmpty) {
      values = Some(t.productIterator.map(mutable.HashSet[Any](_)).toList)
    } else {
      (values.get.iterator zip t.productIterator)
        .foreach { case (set, v) => set += v }
    }
  }

  /**
    * Releases the list of fields that are attached to the passed in product
    * @param t The template of fields to release
    */
  def remove(t: T): Unit = {
    if (values.isDefined) {
      (values.get.iterator zip t.productIterator)
        .foreach { case (set, v) => set -= v }
    }
  }

  /**
    * Gets a list of product values that causes the given object to be non-unique
    * in the context of the value manager
    * @param t The value to examine
    * @return A Seq[Any] containing the product values, or Nil if fully unique
    */
  def nonUniqueElements(t: T): Seq[Any] = {
    if (values.isEmpty) {
      Nil
    } else {
      (values.get.iterator zip t.productIterator)
        .filter { case (set, v) => set.contains(v) }
        .map { case (_, v) => v }
        .toList
    }
  }
}
