package game.mode

import actors.PlayerWithActor
import common.{Resources, Util}
import controllers.{InGamePacket, RequestPlaceReinforcements, UpdateBoardState, UpdatePlayerState}
import game.PlayerState.{Idle, Reinforcement, TurnState}
import game._
import game.mode.GameMode._
import models.Player
import play.api.Logger

import scala.util.Random

/**
  * Concrete implementation of GameMode bound for DI at runtime
  */
class SkirmishGameMode extends GameMode {
  // GameMode-specific gameboard
  lazy override val gameboard: Gameboard = Resources.SkirmishGameboard

  override def assignTurnOrder(players: Seq[PlayerWithActor]): Seq[PlayerWithActor] =
    Random.shuffle(players)

  override def initializeGameState(callback: Callback)(implicit state: GameState): Unit = {
    // Assign territories to players
    val perTerritory = Resources.SkirmishInitialArmy
    val territoryIndices = Util.listBuffer(Random.shuffle(state.boardState.indices.toList))
    val firstPlayer = state.turnOrder.head.player
    calculateAllocations(state).zipWithIndex.foreach { case (allocation, index) =>
      // Sample and then remove from remaining territories
      val territories = territoryIndices.take(allocation)
      territoryIndices --= territories
      // Add OwnedArmy's to each of the sampled territories
      territories.foreach { i =>
        val army = OwnedArmy(Army(perTerritory), state.turnOrder(index).player)
        state.boardState.update(i, Some(army))
      }
      // Update the total army count for the player and assign turn states
      val player = state.turnOrder(index).player
      state.playerStates.update(index, PlayerState(player, Army(allocation * perTerritory),
        if (player == firstPlayer) reinforcement(player) else TurnState(Idle)))
    }
    callback.broadcast(UpdateBoardState(state), None)
  }

  def reinforcement(player: Player)(implicit state: GameState): TurnState =
    TurnState(Reinforcement, "amount" -> calculateReinforcement(player))

  def calculateReinforcement(player: Player)(implicit state: GameState): Int = {
    val conquered = state.boardState.zipWithIndex
      .filter { case (oaOption, _) => oaOption.isDefined && oaOption.get.owner == player }
    val territories = conquered.length
    val castles = conquered.count { case (_, index) => gameboard.nodes(index).dto.hasCastle }
    val base = Resources.SkirmishReinforcementBase
    val divisor = Resources.SkirmishReinforcementDivisor
    Math.max(Math.floor((territories + castles) / divisor.toDouble), base.toDouble).toInt
  }

  def calculateAllocations(implicit state: GameState): Seq[Int] = {
    val base = state.boardState.size / state.gameSize
    val remainder = state.boardState.size % state.gameSize
    if (remainder == 0) {
      List.fill(state.gameSize)(base)
    } else {
      // Give equal amounts to each player (base), while distributing the
      // remainder equally to the last players by turn order
      (0 until state.gameSize).map(i => if (i >= state.gameSize - remainder) base + 1 else base)
    }
  }

  override def handlePacket(packet: InGamePacket, callback: Callback)(implicit state: GameState): Unit = {
    state.turnOrder.find(a => a.id == packet.playerId).foreach { player =>
      packet match {
        case RequestPlaceReinforcements(_, _, assignments) =>
          requestReplaceReinforcements(callback, player, assignments)
      }
    }
  }

  def requestReplaceReinforcements(callback: GameMode.Callback, actor: PlayerWithActor,
                                   assignments: Seq[(Int, Int)])(implicit state: GameState): Unit = {
    val logger = Logger(this.getClass).logger
    // TODO remove debug message
    logger.error(s"reinforcements received from ${actor.player.settings.fold("")(ps => ps.name)}: $assignments")
    // TODO validate and generate response, if successful, move turn ahead
  }

  override def playerDisconnect(actor: PlayerWithActor, callback: Callback)(implicit state: GameState): Unit = {
    // Release all owned territories
    state.boardState.zipWithIndex
      .filter { case (armyOption, _) => armyOption.forall(oa => oa.owner == actor.player) }
      .foreach { case (_, index) => state.boardState.update(index, None) }
    // Remove from turn order
    state.turnOrder = Util.remove(actor, state.turnOrder)
    // Notify game of changes (no need to send to the disconnecting player)
    callback.broadcast(UpdateBoardState(state), Some(actor.id))
    callback.broadcast(UpdatePlayerState(state), Some(actor.id))
  }
}
