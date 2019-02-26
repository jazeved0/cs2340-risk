package gameplay

import models.Player

import scala.collection.mutable

trait GameMode {
  def assignTurnOrder(players: Seq[Player]): Seq[Player]
  def assignInitialArmies(gameState: GameState): Seq[Army]
  def initializeGameState(joinOrder: Seq[Player]): GameState = {
    val turnOrder: Seq[Player] = assignTurnOrder(joinOrder)
    val gameState = new GameState(turnOrder)
    gameState.playerStates = mutable.Seq[PlayerState](
      (turnOrder zip assignInitialArmies(gameState)).map {
        case (p: Player, a: Army) => PlayerState(p.settings.get, a)
      }.toList:_*)
    gameState
  }
}
