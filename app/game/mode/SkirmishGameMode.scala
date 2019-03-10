package game.mode

import actors.PlayerWithActor
import common.{Resources, Util}
import controllers.{InGamePacket, OutPacket, UpdateBoardState, UpdatePlayerState}
import game._

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

  override def initializeGameState(state: GameState,
                                   broadcastCallback: (OutPacket, Option[String]) => Unit,
                                   sendCallback: (OutPacket, String) => Unit): Unit = {
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
    samplesPerPlayer.zipWithIndex.foreach { t =>
      val territories = territoryIndices.take(t._1)
      territoryIndices --= territories
      territories.foreach { i =>
        val army = OwnedArmy(Army(perTerritory), state.turnOrder(t._2).player)
        state.boardState.update(i, Some(army))
      }
      state.playerStates.update(t._2, PlayerState(
        state.turnOrder(t._2).player,
        Army(t._1 * perTerritory)))
    }
    broadcastCallback(updateBoardState(state), None)
  }

  def updateBoardState(state: GameState): UpdateBoardState =
    UpdateBoardState(
      state.boardState,
      state.turnOrder.map(actor => actor.player))

  override def handlePacket(packet: InGamePacket,
                            state: GameState,
                            broadcastCallback: (OutPacket, Option[String]) => Unit,
                            sendCallback: (OutPacket, String) => Unit): Unit = {
    packet match {
      case _ =>
    }
  }

  override def playerDisconnect(actor: PlayerWithActor,
                                state: GameState,
                                broadcastCallback: (OutPacket, Option[String]) => Unit,
                                sendCallback: (OutPacket, String) => Unit): Unit = {
    state.boardState.zipWithIndex
      .filter(t => t._1.forall(oa => oa.owner == actor.player))
      .foreach(t => state.boardState.update(t._2, None))
    state.turnOrder = Util.remove(actor, state.turnOrder)
    broadcastCallback(updateBoardState(state), Some(actor.id))
    broadcastCallback(UpdatePlayerState(state.playerStates), Some(actor.id))
  }
}
