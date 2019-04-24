package game.mode.skirmish

import actors.PlayerWithActor
import common.{Impure, Pure}
import controllers.{InPacket, RequestReply, RequestResponse}
import game.GameContext
import game.state.{GameState, TurnState}

/**
  * Sub-object of Progression Handler that processes incoming packets and
  * validates them depending on their content and the current game state
  */
object ValidationHandler {
  /**
    * Applies the validation pipeline to the incoming packet, having the
    * opportunity to send packets or mutate the game state in response
    * @param packet The incoming packet from the network to validate
    * @param context Incoming context wrapping current game state
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def validate(packet: InPacket)(implicit context: GameContext): ValidationResult =
    packet match {
      // TODO write/rewrite validation cases
      case _ => ValidationResult(result = true)
    }



  // TODO refactor
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
