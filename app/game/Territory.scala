package game

case class Territory(connections: Set[Int],
                     var occupier: Option[OwnedArmy] = None)
