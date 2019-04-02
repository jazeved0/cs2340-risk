package models

/**
  * Territory DTO, contains functional properties (used in back end)
 *
  * @param connections index-wise connections to this territory
  * @param castle the location of its castle, if it has one
  */
case class Territory(connections: Set[Int], castle: Option[Location]) {
  def hasCastle: Boolean = castle.isDefined
}
