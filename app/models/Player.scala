package models

// player DTO
object Player {
  val MaxNameLength = 16
}

case class Player(id: String, name: String, color: Color)