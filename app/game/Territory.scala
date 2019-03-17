package game

import game.Gameboard.Location

// Territory DTO
case class Territory(connections: Set[Int], castle: Option[Location]) {
  def hasCastle: Boolean = castle.isDefined
}
