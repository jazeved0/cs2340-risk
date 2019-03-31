package game.mode

import actors.PlayerWithActor
import common.{Resources, Util}
import controllers._
import game.Gameboard
import game.mode.GameMode._
import game.state.TurnState._
import game.state.{GameState, PlayerState, TurnState}
import models.{Army, OwnedArmy, Player}

import scala.util.Random

/**
  * Concrete implementation of GameMode bound for DI at runtime. Defines the rules
  * for the Skirmish mode from GOT Risk. See
  * [[https://theop.games/wp-content/uploads/2019/02/got_risk_rules.pdf]] for the rules
  */
class SkirmishGameMode extends GameMode {
  type StateType = GameState
  /** GameMode-specific gameboard, loaded through Resource injection */
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

  /**
    * Utility method that creates a reinforcement state machine object and
    * calculates the reinforcement allocation as necessary
    * @param player The player to use to calculate reinforcements
    * @param state The GameState context object
    * @return A new TurnState object for Reinforcement State containing the
    *         calculated allocation
    */
  def reinforcement(player: Player)(implicit state: GameState): TurnState =
    TurnState(Reinforcement, "amount" -> calculateReinforcement(player))

  /**
    * Performs the calculation logic according to values injected from Resources
    * for the target player
    * @param player The player to calculate reinforcements for
    * @param state The GameState context object
    * @return The number of reinforcements the player should receive, as an Int
    */
  def calculateReinforcement(player: Player)(implicit state: GameState): Int = {
    val conquered = state.boardState.zipWithIndex
      .filter { case (oaOption, _) => oaOption.isDefined && oaOption.get.owner == player }
    val territories = conquered.length
    val castles = conquered.count { case (_, index) => gameboard.nodes(index).dto.hasCastle }
    val base = Resources.SkirmishReinforcementBase
    val divisor = Resources.SkirmishReinforcementDivisor
    Math.max(Math.floor((territories + castles) / divisor.toDouble), base.toDouble).toInt
  }

  /**
    * Calculates initial allocations for all players in the game (by turn order),
    * equally dividing territories randomly between each player and giving the
    * remainder, if any, equally to the last players
    * @param state The GameState context object
    * @return A list giving the number of army tokens each player should receive,
    *         ordered by the turn order
    */
  def calculateAllocations(implicit state: GameState): Seq[Int] = {
    val base = state.boardState.length / state.gameSize
    val remainder = state.boardState.length % state.gameSize
    if (remainder == 0) {
      List.fill(state.gameSize)(base)
    } else {
      // Give equal amounts to each player (base), while distributing the
      // remainder equally to the last players by turn order
      (0 until state.gameSize)
        .map(i => if (i >= state.gameSize - remainder) base + 1 else base)
    }
  }

  override def handlePacket(packet: InGamePacket, callback: Callback)
                           (implicit state: GameState): Unit = {
    state.turnOrder.find(a => a.id == packet.playerId).foreach { player =>
      packet match {
        case RequestPlaceReinforcements(_, _, assignments) =>
          requestPlaceReinforcements(callback, player, assignments)
      }
    }
  }

  /**
    * Handles incoming request place reinforcements packet. Validates the
    * request and then sends a RequestReply depending on whether the request
    * gets approved. If it is approved, adjust game state as necessary and
    * move the turn state's state machine forwards
    *
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param actor The player that initiated the request
    * @param assignments The proposed assignments Seq[(territory index -> amount)]
    * @param state The GameState context object
    */
  def requestPlaceReinforcements(callback: GameMode.Callback, actor: PlayerWithActor,
                                 assignments: Seq[(Int, Int)])
                                (implicit state: GameState): Unit = {
    import play.api.Logger
    val logger = Logger(this.getClass).logger
    val name = actor.player.settings.fold("")(ps => ps.name)
    logger.error(s"reinforcements received from $name: $assignments")
    if (validateReinforcements(callback, actor, assignments)) {
      // TODO logic
    }
  }

  /**
    * Validates the reinforcement request given by the player
    *
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param actor The player that initiated the request
    * @param assignments The proposed assignments Seq[(territory index -> amount)]
    * @param state The GameState context object
    * @return
    */
  def validateReinforcements(callback: GameMode.Callback, actor: PlayerWithActor,
                             assignments: Seq[(Int, Int)])
                            (implicit state: GameState): Boolean = {
    val calculated = calculateReinforcement(actor.player)
    val totalPlaced = assignments.map(tup => tup._2).sum
    if (state.isInState(actor.player, TurnState.Reinforcement)) {
      if (totalPlaced == calculated) {
        // Valid
        true
      } else {
        // Invalid
        val descriptor = if (calculated > totalPlaced) "many" else "few"
        callback.send(RequestReply(RequestResponse.Rejected, s"Too $descriptor " +
          s"reinforcements in attempted placement $totalPlaced for " +
          s"allocation $calculated"), actor.id)
        false
      }
    } else {
      callback.send(RequestReply(RequestResponse.Rejected, "Invalid state to " +
        "place reinforcements"), actor.id)
      false
    }
  }

  override def playerDisconnect(actor: PlayerWithActor, callback: Callback)
                               (implicit state: GameState): Unit = {
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

  /**
    * Subclass hook that allows subclasses to register custom GameState implementations
    * to be used when initializing games.
    * @param turnOrder The turn order of the game (sent from the Game actor)
    * @return A GameState object
    */
  override def makeGameState(turnOrder: Seq[PlayerWithActor]): GameState = {
    val seq = IndexedSeq() ++ turnOrder
    new GameState(seq, gameboard.nodeCount)
  }
}
