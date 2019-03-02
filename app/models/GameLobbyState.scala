package models

// State enumeration for a game app
object GameLobbyState extends Enumeration {
  type State = Value
  val Lobby, InGame = Value
}
