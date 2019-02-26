package models

import common.Resources

object PlayerSettings {
  val MaxNameLength = 16

  def isValid(arg: PlayerSettings): Boolean =
    arg.name.length <= PlayerSettings.MaxNameLength &&
      (arg.ordinal < Resources.Colors.size && arg.ordinal >= 0)

  def formatInvalid(userData: PlayerSettings): String = userData match {
    case PlayerSettings(name: String, _) if name.length > MaxNameLength =>
      s"Given name $name is too long (max: $MaxNameLength)"
    case PlayerSettings(_, ordinal: Int) if ordinal < 0 =>
      s"Given color index $ordinal is invalid (cannot be negative)"
    case PlayerSettings(_, ordinal: Int) if ordinal >= Resources.Colors.length =>
      s"Given color index $ordinal is invalid (max: ${Resources.Colors.length - 1})"
    case _ =>
      "Given player settings are invalid"
  }
}

case class PlayerSettings(name: String, ordinal: Int) {
  def color: Color = Resources.Colors.lift(ordinal).getOrElse(Resources.Colors.head)
}
