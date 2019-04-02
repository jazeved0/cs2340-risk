package models

import common.Resources

object PlayerSettings {
  /**
    * Validates the length of the name and the color ordinal to ensure they fit
    * within their valid bounds (as determined by resource injection)
    * @param arg The player settings object to validate
    * @return True if the object is valid, false otherwise
    */
  def isValid(arg: PlayerSettings): Boolean =
    arg.name.length <= Resources.MaxNameLength &&
    arg.name.length >= Resources.MinNameLength &&
      (arg.ordinal < Resources.Colors.size && arg.ordinal >= 0)

  /**
    * Formats a error message for when a player settings object is invalid
    * (doesn't actually perform validation logic; just formats message based on
    * the information)
    * @param userData The invalid player settings to format an error message for
    * @return The error message
    */
  def formatInvalid(userData: PlayerSettings): String = userData match {
    case PlayerSettings(name: String, _) if name.length > Resources.MaxNameLength =>
      s"Given name $name is too long (max: ${Resources.MaxNameLength})"
    case PlayerSettings(name: String, _) if name.length < Resources.MinNameLength =>
      s"Given name $name is too short (min: ${Resources.MinNameLength})"
    case PlayerSettings(_, ordinal: Int) if ordinal < 0 =>
      s"Given color index $ordinal is invalid (cannot be negative)"
    case PlayerSettings(_, ordinal: Int) if ordinal >= Resources.Colors.length =>
      s"Given color index $ordinal is invalid (max: ${Resources.Colors.length - 1})"
    case _ =>
      "Given player settings are invalid"
  }
}

/**
  * Player settings DTO containing information on the chosen color and name
  * of a given player
  * @param name The player name
  * @param ordinal The index of the player's color
  */
case class PlayerSettings(name: String, ordinal: Int) {
  def color: Color = Resources.Colors.lift(ordinal).getOrElse(Resources.Colors.head)
}
