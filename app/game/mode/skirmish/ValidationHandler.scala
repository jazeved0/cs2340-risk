package game.mode.skirmish

import actors.PlayerWithActor
import common.{Pure, Util}
import controllers._
import game.GameContext
import game.mode.skirmish.ValidationContext._
import game.state.TurnState._
import game.state.{AttackState, TerritoryState}

import scala.collection.immutable.Queue

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
        case RequestEndAttack(_, _)                        => requestEndAttack
        case RequestDoManeuver(_, _, origin, amt, dest)    => requestDoManeuver(origin, amt, dest)
        case _                                             => ValidationResult(result = false)
      }

  /**
    * Validates a RequestPlaceReinforcements packet
    *
    * @param assignments The proposed assignments Seq[(territory index -> amount)]
    * @param context     Incoming context wrapping current game state
    * @param sender      The player actor that initiated the request
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def requestPlaceReinforcements(assignments: Seq[(Int, Int)])
                                (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state       = context.state.stateOf(sender.player)
    val calculated  = PlayerStateHandler.calculateReinforcement(sender.player)
    val totalPlaced = assignments.map(tup => tup._2).sum
    begin("RequestPlaceReinforcements")
      .check("Player is out of turn")(inTurn)
      .check(state.isDefined)
      .check(state.get.turnState.state == Reinforcement)
      .check("Player is placing wrong amount of territories") {
        calculated == totalPlaced
      }
      .check("Player does not own all territories") {
        assignments.forall {
          case (index, _) => ownsTerritory(index)
        }
      }
      .consume(Reply)
  }

  /**
    * Validates a RequestAttack packet
    *
    * @param attackData Incoming data from the packet
    * @param context    Incoming context wrapping current game state
    * @param sender     The player actor that initiated the request
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def requestAttack(attackData: Seq[Int])
                   (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state = context.state.stateOf(sender.player)
    val attack = if (attackData.size == 3) Some(AttackState(attackData)) else None
    begin("RequestAttack")
      .check("Player is out of turn")(inTurn)
      .check(state.isDefined)
      .check(state.get.turnState.state == Attack)
      .check(attack.isDefined)
      .checkFalse("There is already an ongoing attack")(inBattle)
      .check("Attacker doesn't own attacking territory")(ownsTerritory(attack.get.attackingIndex))
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
    * @param context   Incoming context wrapping current game state
    * @param sender    The player actor that initiated the request
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def defenseResponse(defenders: Int)
                     (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state = context.state.stateOf(sender.player)
    begin("DefenseResponse")
      .check(state.isDefined)
      .check(state.get.turnState.state == Defense)
      .check("There is not an ongoing attack")(inBattle)
      .check("Invalid defender amount") {
        val currentAttack = context.state.currentAttack.get
        val defendingTerritory = context.state.boardState(currentAttack.defendingIndex)
        defenders >= 1 && defenders <= defendingTerritory.size
      }
      .consume(Reply)
  }

  /**
    * Validates a RequestEndAttack packet
    *
    * @param context Incoming context wrapping current game state
    * @param sender  The player actor that initiated the request
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def requestEndAttack(implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state = context.state.stateOf(sender.player)
    begin("RequestEndAttack")
      .check("Player is out of turn")(inTurn)
      .check(state.isDefined)
      .check(state.get.turnState.state == Attack)
      .checkFalse("Player is in the middle of an attack")(inBattle)
      .consume(Reply)
  }

  /**
    * Validates a RequestDoManeuver packet
    *
    * @param origin      The index of the origin territory
    * @param amount      The number of troops that the player is maneuvering
    * @param destination The index of the delineation territory
    * @param context     Incoming context wrapping current game state
    * @param sender      The player actor that initiated the request
    * @return A context object wrapping the updated game context and the result
    */
  @Pure
  def requestDoManeuver(origin: Int, amount: Int, destination: Int)
                       (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val state = context.state.stateOf(sender.player)
    begin("RequestDoManeuver")
      .check("Player is out of turn")(inTurn)
      .check(state.isDefined)
      .check(state.get.turnState.state == Maneuver)
      .check("Player does not own the origin territory")(ownsTerritory(origin))
      .check("Player does not own the destination territory")(ownsTerritory(destination))
      .check("Origin territory doesn't have enough troops") {
        context.state.boardState(origin).size > amount
      }
      .check(origin != destination)
      .check("Origin and destination territories aren't connected") {
        val owned = context.state.boardState.zipWithIndex.filter {
          case (TerritoryState(_, owner), _) => owner == sender.player
        }.map {
          case (_, index) => index
        }.toSet
        Util.bfs[Int](origin, index => Queue[Int]() ++
          (context.state.gameboard.nodes.lift(index) match {
            case Some(node) => node.dto.connections.intersect(owned)
            case None => Nil
          })
        ) contains destination
      }
      .consume(Reply)
  }

  // Helper methods
  @Pure
  def ownsTerritory(index: Int)(implicit context: GameContext, sender: PlayerWithActor): Boolean =
    context.state.boardState.lift(index) match {
      case Some(territoryState) => territoryState.owner == sender.player
      case None                 => false
    }

  @Pure
  def inBattle(implicit context: GameContext, sender: PlayerWithActor): Boolean =
    context.state.inBattle

  @Pure
  def inTurn(implicit context: GameContext, sender: PlayerWithActor): Boolean =
    context.state.currentPlayer == sender
}
