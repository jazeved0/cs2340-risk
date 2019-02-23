package common

import controllers.routes
import models.{ClientSettings, Color}
import play.api.data.Form
import play.api.data.Forms._

object Resources {

  // colors from https://flatuicolors.com/palette/defo
  val Colors = List(
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
  val BaseUrl = "localhost:/9000"
  val MakeUrl = routes.MainController.make()
  val NonHostSubmitURL = routes.MainController.make()
}
