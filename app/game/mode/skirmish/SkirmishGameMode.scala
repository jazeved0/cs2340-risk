package game.mode.skirmish

import actors.PlayerWithActor
import common.{Impure, Pure, Resources, Util}
import controllers._
import game.mode.GameMode
import game.mode.skirmish.SkirmishGameContext._
import game.state._
import game.{GameContext, Gameboard}
import models.NeutralPlayer

/**
  * Concrete implementation of GameMode bound for DI at runtime. Defines the rules
  * for the Skirmish mode from GOT Risk. See
  * [[https://theop.games/wp-content/uploads/2019/02/got_risk_rules.pdf]] for the rules
  */
class SkirmishGameMode extends GameMode {
  /** GameMode-specific gameboard, loaded through Resource injection */
  lazy val gameboard: Gameboard = Resources.SkirmishGameboard

  @Impure.Nondeterministic
  override def hookInitializeGame(implicit context: GameContext): GameContext = {
    import game.mode.skirmish.InitializationHandler._
    val territoryCount = context.state.gameboard.nodeCount
    val allocations = calculateAllocations(territoryCount, context.state.size)
    val territoryAssignments = assignTerritories(allocations, territoryCount)
    context.map(state =>
      state.copy(boardState   = makeBoardState(territoryAssignments),
                 playerStates = makePlayerStates(territoryAssignments)))
  }

  @Impure.Nondeterministic
  override def hookPacket(packet: InGamePacket)(implicit context: GameContext): GameContext =
    ValidationHandler.validate(packet) match {
      case ValidationResult(false, ctx) => ctx
      case ValidationResult(true,  ctx) => ProgressionHandler.handle(packet)(ctx)
    }

  @Pure
  override def hookPlayerDisconnect(actor: PlayerWithActor)
                                   (implicit context: GameContext): GameContext = {
    // Release all owned territories
    val DisconnectingPlayer = actor.player
    val boardState = context.state.boardState.map {
      case TerritoryState(army, DisconnectingPlayer) => TerritoryState(army, NeutralPlayer)
      case territoryState                            => territoryState
    }

    // Remove from turn order
    val newTurnOrder = Util.remove(actor, context.state.turnOrder)
    val oldStates = context.state.turnOrder.zipWithIndex.toMap
    val newPlayerStates = newTurnOrder.map(actor => context.state.playerStates(oldStates(actor)))

    val processedContext = processDisconnectAttackState(actor)
      .map(gs => gs.copy(
        turnOrder    = newTurnOrder,
        playerStates = newPlayerStates,
        turn         = gs.turnUponDisconnect(actor),
        boardState   = boardState
      ))
      .clearPayloads

    // Move state forward if necessary
    (if (processedContext.state.isEmpty) {
      context.state.stateOf(context.state.currentPlayer.player) match {
        case Some(PlayerState(_, _, TurnState(TurnState.Idle, _))) =>
          processedContext
            .advanceTurnState

        case _ => processedContext // pass
      }
    } else {
      processedContext
    })
      // Notify game of changes (no need to send to the disconnecting player)
      .thenBroadcastBoardState(actor.id)
      .thenBroadcastPlayerState(actor.id)
  }

  /**
    * Processes the current attack state upon a player disconnect, ensuring the
    * attack state gets resolved if the disconnecting player is one of the players
    * involved
    * @param actor The disconnecting player
    * @param context Incoming context wrapping current game state
    * @return An updated game context object
    */
  @Pure
  def processDisconnectAttackState(actor: PlayerWithActor)
                                  (implicit context: GameContext): GameContext = {
    if (context.state.isInDefense) {
      context.state.stateOf(actor.player) match {
        // Puts defender in Idle
        case Some(PlayerState(_, _, TurnState(TurnState.Defense, _))) =>
          context
            .advanceAttackTurnState(actor.player)
            .map(gs => gs.copy(
              currentAttack = None
            ))

        // Puts defender in Idle
        case Some(PlayerState(_, _, TurnState(TurnState.Attack,  _))) =>
          context.state.currentAttack match {
            case Some(AttackState(_, defendingIndex, _)) =>
              val defender = context.state.boardState(defendingIndex).owner
              context
                .advanceAttackTurnState(defender)
                .map(gs => gs.copy(
                  currentAttack = None
                ))
            case _ => context // pass
          }

        case _ => context // pass
      }
    } else {
      context // pass
    }
  }

  @Pure
  override def createGameState(turnOrder: IndexedSeq[PlayerWithActor]): GameState =
    GameState(turnOrder, Vector(), Vector(), gameboard)
}
