package game.mode

import actors.PlayerWithActor
import common.{Resources, Util}
import controllers.{InGamePacket, UpdateBoardState, UpdatePlayerState}
import game._
import game.mode.GameMode._

import scala.collection.mutable
import scala.util.Random

/**
  * Concrete implementation of GameMode bound for DI at runtime
  */
class SkirmishGameMode extends GameMode {
  // GameMode-specific gameboard
  lazy override val gameboard: Gameboard = Resources.SkirmishGameboard

  override def assignTurnOrder(players: Seq[PlayerWithActor]): Seq[PlayerWithActor] =
    Random.shuffle(players)

  override def initializeGameState(state: GameState, callback: Callback): Unit = {
    // Assign territories to players
    val perTerritory = Resources.SkirmishInitialArmy
    val territoryIndices = mutable.ListBuffer() ++ Random.shuffle(state.boardState.indices.toList)
    val samplesPerPlayer: Seq[Int] =
      if (state.boardState.size % state.gameSize == 0) {
        List.fill(state.gameSize)(state.boardState.size / state.gameSize)
      } else {
        val base = state.boardState.size / state.gameSize
        val remainder = state.boardState.size % state.gameSize
        (0 until state.gameSize).map(i => if (i >= state.gameSize - remainder) base + 1 else base)
      }
    samplesPerPlayer.zipWithIndex.foreach { case (sample, index) =>
      val territories = territoryIndices.take(sample)
      territoryIndices --= territories
      territories.foreach { i =>
        val army = OwnedArmy(Army(perTerritory), state.turnOrder(index).player)
        state.boardState.update(i, Some(army))
      }
      state.playerStates.update(index, PlayerState(
        state.turnOrder(index).player,
        Army(sample * perTerritory)))
    }
    callback.broadcast(updateBoardState(state), None)
  }

  def updateBoardState(state: GameState): UpdateBoardState =
    UpdateBoardState(
      state.boardState,
      state.turnOrder.map(actor => actor.player))

  override def handlePacket(packet: InGamePacket, state: GameState, callback: Callback): Unit = {
    packet match {
      case _ =>
    }
  }

  override def playerDisconnect(actor: PlayerWithActor, state: GameState, callback: Callback): Unit = {
    state.boardState.zipWithIndex
      .filter { case (armyOption, _) => armyOption.forall(oa => oa.owner == actor.player) }
      .foreach { case (_, index) => state.boardState.update(index, None) }
    state.turnOrder = Util.remove(actor, state.turnOrder)
    callback.broadcast(updateBoardState(state), Some(actor.id))
    callback.send(UpdatePlayerState(state.playerStates), actor.id)
  }
}
