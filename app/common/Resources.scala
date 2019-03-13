package common

import controllers.routes
import game.Gameboard
import game.mode.GameMode
import models.{Color, PlayerSettings}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Call

import scala.concurrent.duration.FiniteDuration

/**
  * General resources for the application loaded from the configuration
  * file
  */
object Resources {
  val UserForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "ordinal" -> number
    )(PlayerSettings.apply)(PlayerSettings.unapply)
  )
  val MakeUrl: Call = routes.MainController.make()
  val NonHostSubmitURL: Call = routes.MainController.make()

  // Consumed in Module
  object ConfigKeys {
    val OriginsConfig = "app.controllers.origins"
    val PlayerIdCookie = "app.controllers.playerIdCookie"
    val BaseUrl = "app.controllers.baseUrl"
    val IncomingPacketBufferSize = "app.controllers.incomingPacketBufferSize"
    val InitialPingDelay = "app.controllers.initialPingDelay"
    val PingDelay = "app.controllers.pingDelay"
    val PingTimeout = "app.controllers.pingTimeout"
    val PingTimeoutCheckDelay = "app.controllers.pingTimeoutCheckDelay"
    val PingTimeoutCheckInterval = "app.controllers.pingTimeoutCheckInterval"
    val PublicConfigPath = "app.controllers.publicConfigPath"
    val SpaEntryPoint = "app.controllers.spaEntryPoint"

    val Colors = "app.settings.colors"
    val GameIdChars = "app.settings.gameIdChars"
    val NameRegex = "app.settings.nameRegex"
    val GameIdLength = "app.settings.gameIdLength"
    val PlayerIdLength = "app.settings.playerIdLength"
    val MinNameLength = "app.settings.minNameLength"
    val MaxNameLength = "app.settings.maxNameLength"

    val GameMode = "app.gameplay.gameMode"
    val MinimumPlayers = "app.gameplay.minPlayers"
    val MaximumPlayers = "app.gameplay.maxPlayers"
    val SkirmishInitialArmy = "app.gameplay.skirmish.initialArmy"
    var SkirmishGameboard = "app.gameplay.skirmish.gameboard"
  }

  // ********************
  // CONFIG LOADED VALUES
  // ********************

  var Origins: Seq[String] = _
  var PlayerIdCookie: String = _
  var BaseUrl: String = _
  var IncomingPacketBufferSize: Int = _
  var InitialPingDelay: FiniteDuration = _
  var PingDelay: FiniteDuration = _
  var PingTimeout: FiniteDuration = _
  var PingTimeoutCheckDelay: FiniteDuration = _
  var PingTimeoutCheckInterval: FiniteDuration = _
  var PublicConfigPath: String = _
  var SpaEntryPoint: String = _

  var Colors: Seq[Color] = _
  var GameIdChars: Seq[Char] = _
  var NameRegex: String = _
  var GameIdLength: Int = _
  var PlayerIdLength: Int = _
  var MinNameLength: Int = _
  var MaxNameLength: Int = _

  var GameMode: GameMode = _
  var MinimumPlayers: Int = _
  var MaximumPlayers: Int = _
  var SkirmishInitialArmy: Int = _
  var SkirmishGameboard: Gameboard = _
}
