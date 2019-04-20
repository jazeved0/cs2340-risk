package models

/**
  * Army DTO
  * @param size The size of the army (number of stacking army tokens)
  */
case class Army(size: Int) {
  /**
    * Adds the amount onto an army, returning a new one
    * @param amount The amount of army tokens to add
    * @return A new Army object
    */
  // noinspection ScalaStyle
  def +(amount: Int): Army = Army(size + amount)
}
