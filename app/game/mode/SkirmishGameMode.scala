package game.mode

import common.Resources
import game.{Army, GameState}
import models.Player

import scala.util.Random

/**
  * Concrete implementation of GameMode bound for DI at runtime
  */
class SkirmishGameMode extends GameMode {
  override def assignTurnOrder(players: Seq[Player]): Seq[Player] = Random.shuffle(players)
  override def assignInitialArmies(gameState: GameState): Seq[Army] = {
    val armySize = Resources.InitialArmies.getOrElse(gameState.gameSize, 0)
    gameState.turnOrder.map(_ => Army(armySize))
  }
}
