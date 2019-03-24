package models

object Location {
  def apply(tup: (Float, Float)): Location = Location(tup._1, tup._2)
}

/**
  * Location case class used to define a pair of coordinates/bounds
  * @param a The first coordinate
  * @param b The second coordinate
  */
case class Location(a: Float, b: Float)
