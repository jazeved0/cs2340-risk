package common

import controllers.routes
import models.{ClientSettings, Color}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Call

/**
  * General resources for the application
  */
object Resources {

  // colors from https://flatuicolors.com/palette/defo
  val Colors: Seq[Color] = Vector(
    Color("2980b9"),
    Color("27ae60"),
    Color("8e44ad"),
    Color("f39c12"),
    Color("c0392b"),
    Color("bdc3c7")
  )

  val UserForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "ordinal" -> number
    )(ClientSettings.apply)(ClientSettings.unapply)
  )

  val OriginsConfigKey = "app.controllers.origins"
  val ClientIdCookieKey = "clientId"
  val BaseUrl = "localhost:9000"
  val MakeUrl: Call = routes.MainController.make()
  val NonHostSubmitURL: Call = routes.MainController.make()
  val LobbyIdChars: Seq[Char] = "BCEFGHJMPQRTVYWX".toLowerCase.toList
  val MinimumPlayers: Int = 2
}