package game.mode

import actors.PlayerWithActor
import common.Resources
import controllers.{InGamePacket, OutPacket, UpdateBoardState}
import game.{Army, GameState, Gameboard, OwnedArmy}

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
    val territoryIndices = Random.shuffle(state.boardState.indices.toList)
    (territoryIndices zip Stream
      .continually(state.turnOrder.toStream)
      .flatten
      .take(gameboard.nodeCount))
      .map(t => (t._1, OwnedArmy(Army(perTerritory), t._2.player)))
      .foreach(t => state.boardState.update(t._1, Some(t._2)))
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
    broadcastCallback(updateBoardState(state), None)
  }
}
