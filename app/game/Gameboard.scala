package game

import game.Gameboard.{Location, Node}

object Gameboard {
  object Location {
    def apply(tup: (Float, Float)): Location = Location(tup._1, tup._2)
  }
  case class Location(a: Float, b: Float)
  case class Node(path: String, iconPath: String, center: Location, dto: Territory)
}

// Gameboard DTO
case class Gameboard(nodes: Seq[Node], regions: Seq[Range], waterConnections: Seq[Connection], size: Location) {
  def nodeCount: Int = nodes.length
}
