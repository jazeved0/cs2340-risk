package game.mode.skirmish

import common.{Impure, Pure, Resources, Util}
import game.GameContext
import game.state.TurnState.{Idle, Reinforcement}
import game.state.{GameState, PlayerState, TurnState}
import models.{Army, OwnedArmy, Player}

import scala.util.Random

/**
  * Sub-object of SkirmishGameMode that handles game initialization for the
  * skirmish game mode
  */
object InitializationHandler {
  /**
    * Applies the handler logic to the incoming game context
    * @param context Incoming context wrapping current game state
    * @return The updated GameContext wrapping the updated state
    */
  @Impure.Nondeterministic
  def apply(implicit context: GameContext): GameContext = {
    val perTerritory = Resources.SkirmishInitialArmy
    val territoryCount = context.state.gameboard.nodeCount
    val allocations = calculateAllocations(territoryCount, context.state.size)

    // Update board state
    val boardState: Array[Option[OwnedArmy]] = Array.fill(territoryCount)(None)
    val territoryAssignments = assignTerritories(allocations, territoryCount)
    (territoryAssignments zip context.state.turnOrder).foreach {
      case (territories, actor) =>
        val armyOption = Some(OwnedArmy(Army(perTerritory), actor.player))
        // Mutate local board state collection
        territories.foreach(t => boardState(t) = armyOption)
    }

    // Update player states
    val totalArmies = territoryAssignments.map(_.size * perTerritory)
    val playerStates = (totalArmies zip context.state.turnOrder).map {
      case (armySize, actor) => initialPlayerState(actor.player, armySize)
    }

    // Generate new game state with old turnOrder and gameboard
    // TODO consider making more clean
    val newState = GameState(context.state.turnOrder, playerStates,
      boardState.flatten, context.state.gameboard)
    context.withState(newState)
  }

  /**
    * Calculates initial allocations for all players in the game (by turn order),
    * equally dividing territories randomly between each player and giving the
    * remainder, if any, equally to the last players
    * @param territoryCount The number of territories on the map
    * @param playerCount The number of players in the game
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
    * @param territoryAmounts The amount of territories to draw for each player
    * @param territoryCount The amount of territories in total
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
    * Creates a PlayerState object for the given player, starting the first player
    * according to the turn order at the Reinforcement turn state
    * @param player The player
    * @param armies The total number of armies they have
    * @param context Incoming context wrapping current game state
    * @return A new PlayerState object for them
    */
  @Pure
  def initialPlayerState(player: Player, armies: Int)
                        (implicit context: GameContext): PlayerState = {
    val firstPlayer = context.state.turnOrder.head.player
    player match {
      case _ if player == firstPlayer =>
        PlayerState(player, Army(armies), reinforcement(player))
      case _ =>
        PlayerState(player, Army(armies), TurnState(Idle))
    }
  }

  /**
    * Utility method that creates a reinforcement state machine object and
    * calculates the reinforcement allocation as necessary
    * @param player The player to use to calculate reinforcements
    * @param context Incoming context wrapping current game state
    * @return A new TurnState object for Reinforcement State containing the
    *         calculated allocation
    */
  @Pure
  def reinforcement(player: Player)(implicit context: GameContext): TurnState =
    TurnState(Reinforcement, "amount" -> calculateReinforcement(player))

  /**
    * Performs the calculation logic according to values injected from Resources
    * for the target player
    * @param player The player to calculate reinforcements for
    * @param context Incoming context wrapping current game state
    * @return The number of reinforcements the player should receive, as an Int
    */
  @Pure
  def calculateReinforcement(player: Player)(implicit context: GameContext): Int = {
    val conquered = context.state.ownedByZipped(player)
    val gameboard = context.state.gameboard
    val territories = conquered.length
    val castles = conquered.count { case (_, index) => gameboard.hasCastle(index) }
    val base = Resources.SkirmishReinforcementBase
    val divisor = Resources.SkirmishReinforcementDivisor
    // Calculate according to the formula max(floor(territories + castles) / 3), 3)
    Math.max(Math.floor((territories + castles) / divisor.toDouble), base.toDouble).toInt
  }
}
