package game.mode.skirmish

import actors.PlayerWithActor
import common.{Impure, Resources}
import controllers._
import game.GameContext
import game.state.{GameState, PlayerState, TurnState}
import models.OwnedArmy
import play.api.Logger

import scala.math.min

/**
  * Sub-object of SkirmishGameMode that handles game state progression
  * skirmish game mode
  */
object ProgressionHandler {
  /**
    * Handles an incoming packet, processing the current game state using it and
    * potentially sending other packets out as a result
    * @param packet The incoming packet from the network to process
    * @param context Incoming context wrapping current game state
    * @return The updated GameContext wrapping the updated state
    */
  @Impure.Nondeterministic
  def handle(packet: InPacket)(implicit context: GameContext): GameContext =
    packet match {
      // TODO write/rewrite handling cases
      case _ => context
    }










  // OLD CASES
  // TODO refactor
  @Impure.SideEffects
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
  @Impure.SideEffects
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
        oldArmy.foreach(army => state.boardState(index) = Some(army + increment))
      })
      val total = assignments.map(_._2).sum
      val oldState = state.stateOf(actor.player)
      oldState.foreach(ps => state(actor.player) =
        PlayerState(actor.player, ps.units + total, ps.turnState))
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
  @Impure.SideEffects
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
        case _: NeutralPlayer => {
          var defenders = state.boardState(defendingIndex).get.army.size
          if (defenders > 2) {
            defenders = 2
          }
          val attack = Seq(attackingIndex, defendingIndex, attackAmount)
          state.currentAttack = Some(Seq(attackingIndex, defendingIndex, attackAmount))
          state.advanceTurnState(Some(defendingPlayer), ("attack", attack))
          val result: (Seq[Int], Int, Int) = attackResult(attackAmount, defenders, state)
          state.advanceTurnState(Some(defendingPlayer), ("attack", attack ++ Seq(defenders)), ("result", result))
          state.currentAttack = None
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
  @Impure.SideEffects
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
    * @param attackers number of attackers associated with the attack
    * @param defenders number of defenders associated with the attack
    * @return a list containing the dice roll results, and then the amount of attackers destroyed,
    *         and then the number of defenders destroyed
    */
  @Impure
  def attackResult(attackers: Int, defenders: Int, state: GameState): (Seq[Int], Int, Int) = {
    val faces = Resources.DiceFaces
    var attackerResult = (for(_ <- 1 to attackers) yield 1 + scala.util.Random.nextInt(faces)).sortWith(_ > _)
    val defenderResult = (for(_ <- 1 to defenders) yield 1 + scala.util.Random.nextInt(faces)).sortWith(_ > _)
    var attackersDestroyed: Int = 0
    var defendersDestroyed: Int = 0
    for (i <- 0 until min(attackers, defenders)) {
      if (attackerResult(i) <= defenderResult(i)) {
        attackersDestroyed += 1
      } else if (attackerResult(i) > defenderResult(i)) {
        defendersDestroyed += 1
      }
    }
    val logger = Logger(this.getClass).logger
    logger.error("" + state.currentAttack.get.head)
    val attackingArmy = state.boardState(state.currentAttack.get.head)
    val defendingArmy = state.boardState(state.currentAttack.get.tail.head)
    if (attackingArmy.isDefined && defendingArmy.isDefined) {
      state.boardState.update(
        state.currentAttack.get.head,
        Some(OwnedArmy(attackingArmy.get.army + (-1 * attackersDestroyed), attackingArmy.get.owner))
      )
      state.boardState.update(
        state.currentAttack.get.tail.head,
        Some(OwnedArmy(defendingArmy.get.army + (-1 * defendersDestroyed), defendingArmy.get.owner))
      )
      if (state.boardState(state.currentAttack.get.tail.head).get.army.size == 0) {
        state.boardState.update(
          state.currentAttack.get.head,
          Some(OwnedArmy(state.boardState(state.currentAttack.get.head).get.army + -1, attackingArmy.get.owner))
        )
        state.boardState.update(
          state.currentAttack.get.tail.head,
          Some(OwnedArmy(state.boardState(state.currentAttack.get.tail.head).get.army + 1, attackingArmy.get.owner))
        )
      }
    }
    (attackerResult ++ defenderResult, attackersDestroyed, defendersDestroyed)
  }

  @Impure.SideEffects
  def requestEndTurn(callback: GameMode.Callback, actor: PlayerWithActor)
                    (implicit state: GameState): Unit = {
    val logger = Logger(this.getClass).logger
    logger.error("hello")
    logger.error(state.turn.toString)
    state.advanceTurnState(None)
    state.advanceTurnState(None, "amount" -> calculateReinforcement(state.currentPlayer))
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
  @Impure.SideEffects
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
  @Impure.SideEffects
  def validateAttack(callback: GameMode.Callback, actor: PlayerWithActor,
                     attack: Seq[Int])(implicit state: GameState): Boolean = {
    if (state.isInDefense) {
      callback.send(RequestReply(RequestResponse.Rejected,
        s"Invalid attack request; there is already an ongoing attack"), actor.id)
      false
    } else if (attack.length != 3) {
      callback.send(RequestReply(RequestResponse.Rejected,
        s"Invalid attack request; attack must be an array of 3 integers"), actor.id)
      false
    } else if (state.currentPlayer != actor.player) {
      callback.send(RequestReply(RequestResponse.Rejected,
        s"Invalid attack request; it is not that player's attacking turn"), actor.id)
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
            + " or the current player does not own that territory."), actor.id)
        false
      } else if (!validAttack) {
        callback.send(RequestReply(RequestResponse.Rejected,
          s"Invalid attack request; the defending territory is not adjacent"
            + " to the attacking territory."), actor.id)
        false
      } else if (invalidAmount) {
        callback.send(RequestReply(RequestResponse.Rejected,
          s"Invalid attack request; the attacking troop amount must be non-zero and lower"
            + " than the troop amount in the attacking territory"), actor.id)
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
  @Impure.SideEffects
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
}
