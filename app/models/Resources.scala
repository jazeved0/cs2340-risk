package models

import play.api.data.Form
import play.api.data.Forms._

object Resources {
  val COLORS = List(
    Color("16a085"),
    Color("27ae60"),
    Color("8e44ad"),
    Color("f39c12"),
    Color("c0392b"),
    Color("bdc3c7")
  )

  val userForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "colorIndex" -> number
    )(UserData.apply)(UserData.unapply)
  )
}
