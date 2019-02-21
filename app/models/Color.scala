package models

object Color {
  def default: Color = Color(DefaultColorHex)
  val DefaultColorHex = "FF33FF"
}

case class Color(hex: String)