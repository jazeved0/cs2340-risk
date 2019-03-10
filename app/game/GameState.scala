package game

import actors.PlayerWithActor

import scala.collection.mutable

/**
  * Mutable game state object
  * Internally accessible, not serialized (can use ActorRefs and IDs)
  */
class GameState(private var _turnOrder: Seq[PlayerWithActor], territories: Int) {
  var playerStates: mutable.Buffer[PlayerState] = mutable.Buffer() ++
    _turnOrder.map(actor => PlayerState(actor.player, Army(0)))
  var boardState: mutable.Buffer[Option[OwnedArmy]] =
    mutable.ArrayBuffer.fill(territories)(None)

  def gameSize: Int = _turnOrder.length

  def turnOrder: Seq[PlayerWithActor] = _turnOrder
  def turnOrder_(newOrder: Seq[PlayerWithActor]): Unit = {
    // Update playerStates
    val oldStates = _turnOrder.zipWithIndex.toMap
    playerStates = mutable.Buffer() ++
      newOrder.map(actor => playerStates(oldStates(actor)))
    _turnOrder = newOrder
  }
}
