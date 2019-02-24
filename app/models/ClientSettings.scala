package models

import common.Resources

object ClientSettings {
  val MaxNameLength = 16

  def isValid(arg: ClientSettings): Boolean =
    arg.name.length <= ClientSettings.MaxNameLength &&
      (arg.ordinal < Resources.Colors.size && arg.ordinal >= 0)

  def formatInvalid(userData: ClientSettings): String = userData match {
    case ClientSettings(name: String, _) if name.length > MaxNameLength =>
      s"Given name $name is too long (max: $MaxNameLength)"
    case ClientSettings(_, ordinal: Int) if ordinal < 0 =>
      s"Given color index $ordinal is invalid (cannot be negative)"
    case ClientSettings(_, ordinal: Int) if ordinal >= Resources.Colors.length =>
      s"Given color index $ordinal is invalid (max: ${Resources.Colors.length - 1})"
    case _ =>
      "Given client settings are invalid"
  }
}

case class ClientSettings(name: String, ordinal: Int) {
  def color: Color = Resources.Colors.lift(ordinal).getOrElse(Color.default)
}
