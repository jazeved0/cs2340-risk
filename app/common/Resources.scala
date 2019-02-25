package common

import controllers.routes
import models.{ClientSettings, Color}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Call

/**
  * General resources for the application loaded from the configuration
  * file
  */
object Resources {
  val UserForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "ordinal" -> number
    )(ClientSettings.apply)(ClientSettings.unapply)
  )
  val MakeUrl: Call = routes.MainController.make()
  val NonHostSubmitURL: Call = routes.MainController.make()

  // Consumed in Module
  object ConfigKeys {
    val OriginsConfig = "app.controllers.origins"

    val Colors = "app.settings.colors"
    val ClientIdCookie = "app.controllers.clientIdCookie"
    val BaseUrl = "app.controllers.baseUrl"
    val LobbyIdChars = "app.settings.lobbyIdChars"
    val LobbyIdLength = "app.settings.lobbyIdLength"
    val ClientIdLength = "app.settings.clientIdLength"
    val NameRegex = "app.settings.nameRegex"
    val MinNameLength = "app.settings.minNameLength"
    val MaxNameLength = "app.settings.maxNameLength"
    val MinimumPlayers = "app.gameplay.minPlayers"
    val MaximumPlayers = "app.gameplay.maxPlayers"
  }

  // ********************
  // CONFIG LOADED VALUES
  // ********************

  var Origins: Seq[String] = _
  var Colors: Seq[Color] = _
  var ClientIdCookie: String = _
  var BaseUrl: String = _
  var LobbyIdChars: Seq[Char] = _
  var NameRegex: String = _

  var LobbyIdLength: Int = _
  var ClientIdLength: Int = _
  var MinNameLength: Int = _
  var MaxNameLength: Int = _
  var MinimumPlayers: Int = _
  var MaximumPlayers: Int = _
}