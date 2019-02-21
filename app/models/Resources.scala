package models

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
      "colorIndex" -> number
    )(UserData.apply)(UserData.unapply)
  )

  val OriginsConfigKey = "app.controllers.origins"
  val ClientIdCookieKey: String = "clientId"
}
