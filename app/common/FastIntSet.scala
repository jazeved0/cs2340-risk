package common

/**
  * Fast implementation of a Set for integers with contiguous runs. Comparable
  * performance to a HashSet, with better performance in sparse use cases
  */
class FastIntSet(private val source: Set[Int]) extends Set[Int] {
  private val sourceOrdered = source.toList.sorted
  val internalRanges: Seq[Range] = Util.collectToRanges(sourceOrdered).map(tup => tup._1 to tup._2)
  val overallRange: Option[Range] = (internalRanges.headOption zip internalRanges.lastOption)
    .headOption.map(tup => tup._1.start to tup._2.end)
  override def contains(elem: Int): Boolean = {
    overallRange.exists(o =>
        elem >= o.start && elem <= o.end &&
          internalRanges.exists(r => elem >= r.start && elem <= r.end))
  }
  // noinspection ScalaStyle
  override def +(elem: Int): Set[Int] = elem match {
    case e if contains(e) => this
    case e => new FastIntSet(this.source ++ Set(e))
  }
  // noinspection ScalaStyle
  override def -(elem: Int): Set[Int] = elem match {
    case e if !contains(e) => this
    case e => new FastIntSet(this.source -- Set(e))
  }
  override def iterator: Iterator[Int] = sourceOrdered.iterator
}

object FastIntSet {
  def apply(source: Array[Char]): FastIntSet = new FastIntSet(source.map(c => c.toInt).toSet)
  def apply(source: Array[Int]): FastIntSet = new FastIntSet(source.toSet)
  def apply(source: Seq[Int]): FastIntSet = new FastIntSet(source.toSet)
  def apply(source: Set[Int]): FastIntSet = new FastIntSet(source)
  def fromChar(source: Seq[Char]): FastIntSet = FastIntSet(source.map(c => c.toInt))
  def fromChar(source: Set[Char]): FastIntSet = FastIntSet(source.map(c => c.toInt))
}
