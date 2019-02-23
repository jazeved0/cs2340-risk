package models

import common.Resources

object ClientSettings {
  val MaxNameLength = 16
  def apply(name: String, colorIndex: Int): ClientSettings = {
    ClientSettings(name, Resources.Colors.lift(colorIndex).getOrElse(Color.default))
  }
  def unapply(arg: ClientSettings): Option[(String, Int)] = {
    Some(arg.name, arg.ordinal)
  }
}

case class ClientSettings(name: String, color: Color) {
  def ordinal: Int = Resources.Colors.indexOf(color)
}
