package common

import models.Color

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

  val OriginsConfigKey = "app.controllers.origins"
  val ClientIdCookieKey = "clientId"
  val BaseUrl = "localhost:9000"
  val LobbyIdChars: Seq[Char] = "BCEFGHJMPQRTVYWX".toLowerCase.toList
  val MinimumPlayers: Int = 2
}