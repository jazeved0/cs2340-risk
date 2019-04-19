package game

import models.{Connection, Location, Node}

import scala.collection.immutable.Range.Inclusive

/**
  * Gameboard wrapper object
  *
  * @param nodes A list of Node DTOs that exist in the connection graph
  * @param regions A list of inclusive ranges that define the regions
  * @param waterConnections The water connections that exist on the map,
  *                         including optional display information
  * @param size The bounds (width/height) of the gameboard
  */
case class Gameboard(nodes: Seq[Node], regions: Seq[Inclusive],
                     waterConnections: Seq[Connection], size: Location) {
  /**
    * @return The size of the nodes list
    */
  def nodeCount: Int = nodes.length

  def hasCastle(territoryIndex: Int): Boolean = territoryIndex match {
    case i if territoryIndex > 0 && territoryIndex <= nodes.length => nodes(i).dto.hasCastle
    case _ => false
  }
}
