package models

/**
  * Node that represents one territory on the connection graph (not used
  * for back-end calculations; only used for parsing from Resource injection)
 *
  * @param path The data path of the svg path
  * @param iconPath The data path of a smaller, centered svg path
  * @param center The center/key point of the territory (where numbers/lines
  *               get rendered)
  * @param dto The data object giving critical information about its functional
  *            properties
  */
case class Node(path: String, iconPath: String, center: Location, dto: Territory)
