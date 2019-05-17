package controllers.format

import controllers._
import game.Gameboard
import game.state.{PlayerState, TurnState}
import models._
import play.api.libs.json.{JsString, Json, Reads, Writes}

/**
  * Contains (automatically generated) implementations for Reads and Writes
  * formatters, placed in the implicit scope
  */
object JsonMarshallers {
  // Data object marshallers
  implicit val playerSettingsR: Reads[PlayerSettings] = Json.reads[PlayerSettings]
  implicit val playerSettingsW: Writes[PlayerSettings] = Json.writes[PlayerSettings]
  implicit val armyW: Writes[Army] = Json.writes[Army]
  implicit val playerW: Writes[Player] = Json.writes[Player]
  implicit val neutralPlayer: Writes[NeutralPlayer.type] = new UndefinedWriter[NeutralPlayer.type]("NeutralPlayer")
  implicit val concretePlayer: Writes[ConcretePlayer] = Json.writes[ConcretePlayer]
  implicit val stateW: Writes[TurnState.State] = (s: TurnState.State) => JsString(TurnState.State.unapply(s).getOrElse(""))
  implicit val payloadW: Writes[Seq[(String, Any)]] = new PayloadWrites
  implicit val turnStateW: Writes[TurnState] = Json.writes[TurnState]
  implicit val playerStateW: Writes[PlayerState] = Json.writes[PlayerState]
  implicit val locationW: Writes[Location] = Json.writes[Location]
  implicit val territoryW: Writes[Territory] = Json.writes[Territory]
  implicit val connectionW: Writes[Connection] = Json.writes[Connection]
  implicit val nodeW: Writes[Node] = Json.writes[Node]
  implicit val gameboardW: Writes[Gameboard] = Json.writes[Gameboard]

  // Deserializers
  implicit val requestPlayerJoin: Reads[RequestPlayerJoin] = Json.reads[RequestPlayerJoin]
  implicit val requestStartGame: Reads[RequestStartGame] = Json.reads[RequestStartGame]
  implicit val pingResponse: Reads[PingResponse] = Json.reads[PingResponse]
  implicit val requestPlaceReinforcements: Reads[RequestPlaceReinforcements] = Json.reads[RequestPlaceReinforcements]
  implicit val requestAttack: Reads[RequestAttack] = Json.reads[RequestAttack]
  implicit val requestEndTurn: Reads[RequestEndAttack] = Json.reads[RequestEndAttack]
  implicit val requestDoManeuver: Reads[RequestDoManeuver] = Json.reads[RequestDoManeuver]
  implicit val defenseResponse: Reads[DefenseResponse] = Json.reads[DefenseResponse]

  // Unused Deserializers; necessary for macros to work
  implicit val playerConnect: Reads[PlayerConnect] = new UnusedFormat[PlayerConnect]
  implicit val playerDisconnect: Reads[PlayerDisconnect] = new UnusedFormat[PlayerDisconnect]

  // Serializers
  implicit val gameLobbyUpdate: Writes[GameLobbyUpdate] = Json.writes[GameLobbyUpdate]
  implicit val requestReply: Writes[RequestReply] = Json.writes[RequestReply]
  implicit val badPacket: Writes[BadPacket] = Json.writes[BadPacket]
  implicit val startGame: Writes[StartGame] = Json.writes[StartGame]
  implicit val updatePlayerState: Writes[UpdatePlayerState] = Json.writes[UpdatePlayerState]
  implicit val pingPlayer: Writes[PingPlayer] = Json.writes[PingPlayer]
  implicit val sendConfig: Writes[SendConfig] = Json.writes[SendConfig]
  implicit val sendGameboard: Writes[SendGameboard] = Json.writes[SendGameboard]
  implicit val updateBoardState: Writes[UpdateBoardState] = Json.writes[UpdateBoardState]

  // Trait marshallers
  implicit val globalPacket: Reads[GlobalPacket] = Json.reads[GlobalPacket]
  implicit val lobbyPacket: Reads[LobbyPacket] = Json.reads[LobbyPacket]
  implicit val inGamePacket: Reads[InGamePacket] = Json.reads[InGamePacket]
  implicit val inPacket: Reads[InPacket] = Json.reads[InPacket]
  implicit val outPacket: Writes[OutPacket] = Json.writes[OutPacket]
}
