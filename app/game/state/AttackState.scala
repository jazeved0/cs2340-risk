package game.state

// noinspection ZeroIndexToHead
object AttackState {
  def apply(data: Seq[Int]): AttackState = AttackState(data(0), data(1), data(2))
}

/**
  * Represents the current state of an attack
  *
  * @param attackingIndex The territory index of the territory containing the attacking troops
  * @param defendingIndex The territory index of the territory containing the defending troops
  * @param attackAmount   The number of troops the attacker is committing to the attack
  */
case class AttackState(attackingIndex: Int, defendingIndex: Int, attackAmount: Int) {
  def unapply: Seq[Int] = Seq(attackAmount, defendingIndex, attackAmount)
}
