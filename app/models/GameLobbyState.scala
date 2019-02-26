package models

// State enumeration for a game lobby
object GameLobbyState extends Enumeration {
  type State = Value
  val Lobby, InGame = Value
}
