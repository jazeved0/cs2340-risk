package game.mode.skirmish

import common.{Impure, Pure, Resources, Util}
import game.GameContext
import game.mode.skirmish.PlayerStateHandler._
import game.state.TurnState.Idle
import game.state.{PlayerState, TerritoryState, TurnState}
import models.Army

import scala.util.Random

/**
  * Sub-object of SkirmishGameMode that handles game initialization for the
  * skirmish game mode
  */
object InitializationHandler {
  lazy val perTerritory: Int = Resources.SkirmishInitialArmy

  /**
    * Calculates initial allocations for all players in the game (by turn order),
    * equally dividing territories randomly between each player and giving the
    * remainder, if any, equally to the last players
    *
    * @param territoryCount The number of territories on the map
    * @param playerCount    The number of players in the game
    * @return A list giving the number of army tokens each player should receive,
    *         ordered by the turn order
    */
  @Pure
  def calculateAllocations(territoryCount: Int, playerCount: Int): Seq[Int] = {
    val base = territoryCount / playerCount
    val remainder = territoryCount % playerCount
    remainder match {
      case 0 => List.fill(playerCount)(base)
      case _ => (0 until playerCount)
        .map(i => if (i >= playerCount - remainder) {
          base + 1
        } else {
          base
        })
    }
  }

  /**
    * Assigns territories to players, drawing randomly from a pool of territory
    * indices
    *
    * @param territoryAmounts The amount of territories to draw for each player
    * @param territoryCount   The amount of territories in total
    * @return A list of sets of territories to be given to each player
    */
  @Impure.Nondeterministic
  def assignTerritories(territoryAmounts: Seq[Int], territoryCount: Int): Seq[Set[Int]] = {
    val indices = (0 until territoryCount).toList
    // Non-deterministic
    val territoryPool = Util.listBuffer(Random.shuffle(indices))
    territoryAmounts.map {
      // If the pool isn't large enough, skip
      case sampleSize if sampleSize > territoryPool.size => Set[Int]()
      case sampleSize =>
        // Sample and then remove from remaining territories
        val sample = territoryPool.take(sampleSize)
        territoryPool --= sample
        sample.toSet
    }
  }

  /**
    * Initializes the board state according to the assignments created
    *
    * @param territoryAssignments A list of sets of territories to be given to
    *                             each player
    * @param context              Incoming context wrapping current game state
    * @return An indexed sequence of OwnedArmy's representing the army on each
    *         territory, by territory index
    */
  @Pure
  def makeBoardState(territoryAssignments: Seq[Set[Int]])
                    (implicit context: GameContext): IndexedSeq[TerritoryState] = {
    val territoryCount = context.state.gameboard.nodeCount
    // Mutable local board state collection
    val boardState: Array[Option[TerritoryState]] = Array.fill(territoryCount)(None)
    (territoryAssignments zip context.state.turnOrder).foreach {
      case (territories, actor) =>
        val armyOption = Some(TerritoryState(perTerritory, actor.player))
        // Mutate local collection
        territories.foreach(t => boardState(t) = armyOption)
    }
    boardState.map {
      case Some(territoryState) => territoryState
      case None                 => TerritoryState(0)
    }
  }

  /**
    * Initializes the player states according to the assignments created
    *
    * @param territoryAssignments A list of sets of territories to be given to
    *                             each player
    * @param context              Incoming context wrapping current game state
    * @return A sequence of PlayerState objects representing the state of each
    *         player in the game, ordered by turn order
    */
  @Pure
  def makePlayerStates(territoryAssignments: Seq[Set[Int]])
                      (implicit context: GameContext): IndexedSeq[PlayerState] = {
    val totalArmies = territoryAssignments.map(_.size * perTerritory)
    val FirstPlayer = context.state.turnOrder.head.player
    (totalArmies zip context.state.turnOrder).map {
      case (armySize, actor) => actor.player match {
        case FirstPlayer => PlayerState(FirstPlayer,  Army(armySize), reinforcement(FirstPlayer))
        case player =>      PlayerState(player,       Army(armySize), TurnState(Idle))
      }
    }.toIndexedSeq
  }
}
