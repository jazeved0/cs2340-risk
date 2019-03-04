package game

// Gameboard DTO
case class Gameboard(nodeCount: Int,
                     pathData: Seq[String],
                     centers: Seq[(Float, Float)],
                     regions: Seq[Range],
                     waterConnections: Seq[Connection],
                     territories: Seq[Territory])
