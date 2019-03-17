package game

import actors.PlayerWithActor
import common.Util
import game.PlayerState.{Idle, TurnState}

import scala.collection.mutable

/**
  * Mutable game state object
  * Internally accessible, not serialized (can use ActorRefs and IDs)
  */
class GameState(private var _turnOrder: Seq[PlayerWithActor], territories: Int) {
  var playerStates: mutable.Buffer[PlayerState] = Util.buffer(
    _turnOrder.map(actor => PlayerState(actor.player, Army(0), TurnState(Idle))))
  var boardState: mutable.Buffer[Option[OwnedArmy]] = mutable.ArrayBuffer.fill(territories)(None)
  var turn = 0

  def gameSize: Int = _turnOrder.length

  def turnOrder: Seq[PlayerWithActor] = _turnOrder
  def turnOrder_=(newOrder: Seq[PlayerWithActor]): Unit = {
    // Update playerStates
    val oldStates = _turnOrder.zipWithIndex.toMap
    playerStates = Util.buffer(newOrder.map(actor => playerStates(oldStates(actor))))
    _turnOrder = newOrder
  }
}
