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
  */
trait UniqueValueManager[T <: Product] {
  protected var values: Option[List[mutable.HashSet[Any]]] = None

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

  def add(t: T): Unit = {
    if (values.isEmpty) {
      values = Some(t.productIterator.map(mutable.HashSet[Any](_)).toList)
    } else {
      (values.get.iterator zip t.productIterator)
        .foreach { case (set, v) => set += v }
    }
  }

  def remove(t: T): Unit = {
    if (values.isDefined) {
      (values.get.iterator zip t.productIterator)
        .foreach { case (set, v) => set -= v }
    }
  }

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
