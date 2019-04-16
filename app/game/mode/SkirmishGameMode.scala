package game.mode

import actors.PlayerWithActor
import common.{Resources, Util}
import controllers._
import game.Gameboard
import game.mode.GameMode._
import game.state.TurnState._
import game.state.{GameState, PlayerState, TurnState}
import models.{Army, NeutralPlayer, OwnedArmy, Player}
import play.api.Logger

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
        case RequestAttack(_, _, attack) =>
          requestAttack(callback, player, attack)
        case DefenseResponse(_, _, defenders) =>
          defenseResponse(callback, player, defenders)
        case RequestEndTurn(_, _) =>
          requestEndTurn(callback, player)
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
    val logger = Logger(this.getClass).logger
    logger.error("henlo")
    if (validateReinforcements(callback, actor, assignments)) {
      callback.send(RequestReply(RequestResponse.Accepted), actor.id)
      assignments.foreach(tuple => {
        val index = tuple._1
        val increment = tuple._2
        val oldArmy = state.boardState(index)
        oldArmy.foreach(army => state.boardState(index) =
          Some(OwnedArmy(army.army += increment, army.owner)))
      })
      val total = assignments.map(_._2).sum
      val oldState = state.stateOf(actor.player)
      oldState.foreach(ps => state(actor.player) =
        PlayerState(actor.player, ps.units += total, ps.turnState))
      state.advanceTurnState(None)
      callback.broadcast(UpdateBoardState(state), None)
      callback.broadcast(UpdatePlayerState(state), None)
    }
  }

  /**
    * Handles incoming request attack packet. Validates the
    * request and then sends a RequestReply depending on whether the request
    * gets approved. If it is approved, adjust game state as necessary and
    * move the turn state's state machine forwards
    *
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param actor The player that initiated the request
    * @param attack The proposed attack Seq[1st territory index, 2nd territory index, attack amount]
    * @param state The GameState context object
    */
  def requestAttack(callback: GameMode.Callback, actor: PlayerWithActor,
                    attack: Seq[Int])
                   (implicit state: GameState): Unit = {
    val logger = Logger(this.getClass).logger
    logger.error("request attack!")
    if (validateAttack(callback, actor, attack)) {
      logger.error("request attack was valid!")
      callback.send(RequestReply(RequestResponse.Accepted), actor.id)
      val attackingIndex = attack.head
      val defendingIndex = attack.tail.head
      val attackAmount = attack.tail.tail.head
      val defendingPlayer = state.boardState(defendingIndex).get.owner
      defendingPlayer match {
        case neutralDefender: NeutralPlayer => {
          var defenders = state.boardState(defendingIndex).get.army.size
          if (defenders > 2) {
            defenders = 2
          }
          val attack = (attackingIndex, defendingIndex, attackAmount, defenders)
          val result: (Seq[Int], Int, Int) = attackResult(attackAmount, defenders, state)
          state.advanceTurnState(Some(defendingPlayer), ("attack", attack), ("result", result))
          callback.broadcast (UpdateBoardState (state), None)
          callback.broadcast (UpdatePlayerState (state), None)
        }
        case _ => {
          state.currentAttack = Some (Seq (attackingIndex, defendingIndex, attackAmount) )
          state.advanceTurnState(Some(defendingPlayer), ("attack", state.currentAttack.get))
          callback.broadcast (UpdatePlayerState (state), None)
        }
      }
    }
  }

  /**
    * Handles incoming packet with defense amounts, coming from the defending player.
    * Validates the packet and then proceeds to calculate the attack, mutate armies
    * and territories as necessary, and send the result.
    *
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param actor The player that initiated the request
    * @param defenders the number of defenders the person defending has requested
    * @param state The GameState context object
    */
  def defenseResponse(callback: GameMode.Callback, actor: PlayerWithActor,
                      defenders: Int)
                   (implicit state: GameState): Unit = {
    if (validateDefenseResponse(callback, actor, defenders)) {
      callback.send(RequestReply(RequestResponse.Accepted), actor.id)
      val attackers: Int = state.currentAttack.get.tail.tail.head

      val result: (Seq[Int], Int, Int) = attackResult(attackers, defenders, state)
      val attack = state.currentAttack.get ++ Seq(defenders)

      state.currentAttack = None
      state.advanceTurnState(Some(actor.player), ("attack", attack), ("result", result))
      callback.broadcast (UpdateBoardState (state), None)
      callback.broadcast (UpdatePlayerState (state), None)
    }
  }

  /**
    * Produces the result of an attack; the result is a list of integers,
    * representing the dice rolls.  The amount of attackers takes up
    * that amount in the list first; the last part of the list is the dice rolls
    * for the defenders
    * @param attackers
    * @param defenders
    * @return a list containing the dice roll results, and then the amount of attackers destroyed,
    *         and then the number of defenders destroyed
    */
  def attackResult(attackers: Int, defenders: Int, state: GameState): (Seq[Int], Int, Int) = {
    var attackerResult = (for(_ <- 1 to attackers) yield 1 + scala.util.Random.nextInt(6)).sortWith(_ > _)
    val defenderResult = (for(_ <- 1 to defenders) yield 1 + scala.util.Random.nextInt(6)).sortWith(_ > _)
    var attackersDestroyed: Int = 0
    var defendersDestroyed: Int = 0
    for (i <- 0 until defenders) {
      if (attackerResult(i) < defenderResult(i)) {
        attackersDestroyed += 1
      } else if (attackerResult(i) > defenderResult(i)) {
        defendersDestroyed += 1
      }
    }
    val attackingArmy = state.boardState(state.currentAttack.get.head)
    val defendingArmy = state.boardState(state.currentAttack.get.tail.head)
    attackingArmy.foreach(
      army => state.boardState(state.currentAttack.get.head) =
        Some(OwnedArmy(army.army += -attackersDestroyed, army.owner))
    )
    defendingArmy.foreach(
      army => state.boardState(state.currentAttack.get.tail.head) =
        Some(OwnedArmy(army.army += -defendersDestroyed, army.owner))
    )
    if (defendingArmy.isDefined && defendingArmy.get.army.size == 0) {
      attackingArmy.foreach(
        army => state.boardState(state.currentAttack.get.head) =
          Some(OwnedArmy(army.army += -1, army.owner))
      )
      defendingArmy.foreach(
        army => state.boardState(state.currentAttack.get.tail.head) =
          Some(OwnedArmy(army.army += 1, attackingArmy.get.owner))
      )
    }
    (attackerResult ++ defenderResult, attackersDestroyed, defendersDestroyed)
  }

  def requestEndTurn(callback: GameMode.Callback, actor: PlayerWithActor,
                     )
                     (implicit state: GameState): Unit = {
    val logger = Logger(this.getClass).logger
    logger.error("hello")
    logger.error(state.turn.toString)
    state.advanceTurnState(None)
    state.advanceTurnState(None)
    logger.error(state.turn.toString)
    callback.broadcast(UpdateBoardState(state), None)
    callback.broadcast(UpdatePlayerState(state), None)
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
    val invalidAssignment = assignments.exists(
      assignment => state.boardState(assignment._1).exists(army => army.owner != actor.player)
    )

    if (state.isInState(actor.player, TurnState.Reinforcement)) {
      if (invalidAssignment) {
        callback.send(RequestReply(RequestResponse.Rejected,
          s"Invalid territory placement; either a selected territory is undefined" +
            s" or that player does not own one of the territories."
        ), actor.id)
        false
      } else if (totalPlaced == calculated) {
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

  /**
    * Validates the attack request given by the player
    *
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param actor The player that initiated the request
    * @param attack The proposed attack Seq[1st territory index, 2nd territory index, attack amount]
    *               1st territory is attacking, 2nd territory is defending
    * @param state The GameState context object
    * @return
    */
  def validateAttack(callback: GameMode.Callback, actor: PlayerWithActor,
                     attack: Seq[Int])
                            (implicit state: GameState): Boolean = {
    if (state.isInDefense) {
      callback.send(RequestReply(RequestResponse.Rejected,
        s"Invalid attack request; there is already an ongoing attack"
      ), actor.id)
      false
    } else if (attack.length != 3) {
      callback.send(RequestReply(RequestResponse.Rejected,
        s"Invalid attack request; attack must be an array of 3 integers"
      ), actor.id)
      false
    } else if (state.currentPlayer != actor.player) {
      callback.send(RequestReply(RequestResponse.Rejected,
        s"Invalid attack request; it is not that player's attacking turn"
      ), actor.id)
      false
    } else {
      val attackingIndex = attack.head
      val defendingIndex = attack.tail.head
      val attackAmount = attack.tail.tail.head
      val invalidOwner = state.boardState(attackingIndex).fold(true)(
        armyWithOwner => armyWithOwner.owner != actor.player
      )
      val validAttack = gameboard.nodes(attackingIndex).dto.connections.contains(defendingIndex)
      val invalidAmount = state.boardState(attackingIndex).fold(true){
        armyWithOwner => attackAmount >= armyWithOwner.army.size
      } || attackAmount < 1
      if (invalidOwner) {
        callback.send(RequestReply(RequestResponse.Rejected,
          s"Invalid attack request; either the attacking territory could not be found"
          + " or the current player does not own that territory."
        ), actor.id)
        false
      } else if (!validAttack) {
        callback.send(RequestReply(RequestResponse.Rejected,
          s"Invalid attack request; the defending territory is not adjacent"
          + " to the attacking territory."
        ), actor.id)
        false
      } else if (invalidAmount) {
        callback.send(RequestReply(RequestResponse.Rejected,
          s"Invalid attack request; the attacking troop amount must be non-zero and lower"
          + " than the troop amount in the attacking territory"
        ), actor.id)
        false
      } else {
        //Valid

        true
      }
    }
  }

  /**
    * Validates the defense response given by the player
    *
    * @param callback The Callback object providing a means of sending outgoing
    *                 packets to either the entire lobby or to one player
    * @param actor The player that initiated the request
    * @param defenders the number of defenders the person defending has requested
    * @param state The GameState context object
    * @return
    */
  def validateDefenseResponse(callback: GameMode.Callback, actor: PlayerWithActor,
                              defenders: Int)
                             (implicit state: GameState): Boolean = {
    val attackHappening = state.isInDefense
    val isDefender = state.stateOf(actor.player).fold(false)(
      playerState => playerState.turnState.state == TurnState.Defense
    )
    if (!attackHappening) {
      callback.send(RequestReply(RequestResponse.Rejected,
        s"Invalid defense response; no attack is currently occurring."
      ), actor.id)
      false
    } else if (!isDefender) {
      callback.send(RequestReply(RequestResponse.Rejected,
        s"Invalid defense response; this player is not currently defending."
      ), actor.id)
      false
    } else {
      val currentAttack: Seq[Int] = state.currentAttack.get
      val defendingTerritory: Int = currentAttack.tail.head
      val validDefenders = state.boardState(defendingTerritory).fold(false)(
        ownedArmy => defenders <= ownedArmy.army.size && defenders >= 1
      )
      if (!validDefenders) {
        callback.send(RequestReply(RequestResponse.Rejected,
          s"Invalid defense response; the defender amount given is invalid."
        ), actor.id)
        false
      } else {
        //Valid
        true
      }
    }
  }

  override def playerDisconnect(actor: PlayerWithActor, callback: Callback)
                               (implicit state: GameState): Unit = {
    if (state.isInDefense) {
      val currentState = state.stateOf(actor.player).get.turnState
      currentState.state match {
        case TurnState.Defense => {
          //Puts defender in Idle
          state.advanceTurnState(Some(actor.player))
          state.currentAttack = None
        }
        case TurnState.Attack => {
          //Puts defender in Idle
          state.advanceTurnState(Some(state.boardState(state.currentAttack.get.tail.head).get.owner))
          state.currentAttack = None
        }
        case _ =>
      }
    }

    state.modifyTurnAfterDisconnecting(state.turnOrder.indexOf(actor))

    // Release all owned territories
    state.boardState.zipWithIndex
      .filter { case (armyOption, _) => armyOption.forall(oa => oa.owner == actor.player) }
      .foreach {
        case (ownedArmyOption, index) => ownedArmyOption.foreach {
          ownedArmy => state.boardState.update(index, Some(OwnedArmy(ownedArmy.army, Player())))
        }
      }
    // Remove from turn order
    state.turnOrder = Util.remove(actor, state.turnOrder)

    if (state.stateOf(state.currentPlayer).get.turnState.state == TurnState.Idle) {
      state.advanceTurnState(None)
    }

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
