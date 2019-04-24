package game.mode.skirmish

import actors.PlayerWithActor
import common.Pure
import controllers._
import game.GameContext
import game.mode.skirmish.ValidationContext._
import game.state.AttackState
import game.state.TurnState._

/**
  * Sub-object of Progression Handler that processes incoming packets and
  * validates them depending on their content and the current game state
  */
object ValidationHandler {
  /**
    * Applies the validation pipeline to the incoming packet, having the
    * opportunity to send packets or mutate the game state in response
    *
    * @param packet  The incoming packet from the network to validate
    * @param context Incoming context wrapping current game state
    * @param sender  The player actor that initiated the request
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def validate(packet: InPacket)
              (implicit context: GameContext, sender: PlayerWithActor): ValidationResult =
      packet match {
        case RequestPlaceReinforcements(_, _, assignments) => requestPlaceReinforcements(assignments)
        case RequestAttack(_, _, attack)                   => requestAttack(attack)
        case DefenseResponse(_, _, defenders)              => defenseResponse(defenders)
        case RequestEndTurn(_, _)                          => requestEndTurn
        case _                                             => ValidationResult(result = false)
      }

  /**
    * Validates a RequestPlaceReinforcements packet
    *
    * @param assignments The proposed assignments Seq[(territory index -> amount)]
    * @param sender      The player actor that initiated the request
    * @param context     Incoming context wrapping current game state
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def requestPlaceReinforcements(assignments: Seq[(Int, Int)])
                                (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state       = context.state.stateOf(sender.player)
    val calculated  = PlayerStateHandler.calculateReinforcement(sender.player)
    val totalPlaced = assignments.map(tup => tup._2).sum
    begin("RequestPlaceReinforcements")
      .check("Player is out of turn") {
        context.state.currentPlayer == sender
      }
      .check(state.isDefined)
      .check(state.get.turnState.state == Reinforcement)
      .check("Player is placing wrong amount of territories") {
        calculated == totalPlaced
      }
      .checkFalse("Player does not own all territories") {
        assignments.exists {
          case (index, _) => context.state.boardState(index).owner != sender.player
        }
      }
      .consume(Reply)
  }

  /**
    * Validates a RequestAttack packet
    *
    * @param attackData Incoming data from the packet
    * @param sender     The player actor that initiated the request
    * @param context    Incoming context wrapping current game state
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def requestAttack(attackData: Seq[Int])
                   (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state = context.state.stateOf(sender.player)
    val attack = if (attackData.size == 3) Some(AttackState(attackData)) else None
    begin("RequestAttack")
      .check("Player is out of turn") {
        context.state.currentPlayer == sender
      }
      .check(state.isDefined)
      .check(state.get.turnState.state == Attack)
      .check(attack.isDefined)
      .checkFalse("There is already an ongoing attack") {
        context.state.isInDefense
      }
      .check("Attacker doesn't own attacking territory") {
        val attackingTerritoryState = context.state.boardState(attack.get.attackingIndex)
        attackingTerritoryState.owner == sender.player
      }
      .check("Target territory is not adjacent") {
        val attackingTerritoryDTO = context.state.gameboard.nodes(attack.get.attackingIndex).dto
        attackingTerritoryDTO.connections.contains(attack.get.defendingIndex)
      }
      .check("Attack amount is invalid") {
        val attackingTerritoryState = context.state.boardState(attack.get.attackingIndex)
        val amount = attack.get.attackAmount
        amount >= 1 && amount <= attackingTerritoryState.size
      }
      .consume(Reply)
  }

  /**
    * Validates a DefenseResponse packet
    *
    * @param defenders The number of defenders committed (from the packet)
    * @param sender    The player actor that initiated the request
    * @param context   Incoming context wrapping current game state
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def defenseResponse(defenders: Int)
                     (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state = context.state.stateOf(sender.player)
    begin("DefenseResponse")
      .check(state.isDefined)
      .check(state.get.turnState.state == Defense)
      .check("There is not an ongoing attack") {
        context.state.isInDefense
      }
      .check("Invalid defender amount") {
        val currentAttack = context.state.currentAttack.get
        val defendingTerritory = context.state.boardState(currentAttack.defendingIndex)
        defenders >= 1 && defenders <= defendingTerritory.size
      }
      .consume(Reply)
  }

  /**
    * Validates a RequestEndTurn packet
    *
    * @param sender    The player actor that initiated the request
    * @param context   Incoming context wrapping current game state
    * @return A context object wrapping the updated game context and the result
    */
  def requestEndTurn(implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state = context.state.stateOf(sender.player)
    begin("RequestEndTurn")
      .check("Player is out of turn") {
        context.state.currentPlayer == sender
      }
      .check(state.isDefined)
//      .check(state.get.turnState.state == Maneuver)
      .consume(Reply)
  }
}
