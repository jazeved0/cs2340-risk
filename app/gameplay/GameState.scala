package gameplay

import models.Player

import scala.collection.mutable

/**
  * Mutable game state object
  */
class GameState(var turnOrder: Seq[Player]) {
  var playerStates: mutable.Seq[PlayerState] = _
  def gameSize: Int = turnOrder.length
}
