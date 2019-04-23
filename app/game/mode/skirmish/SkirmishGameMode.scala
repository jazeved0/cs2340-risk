package game.mode.skirmish

import actors.PlayerWithActor
import common.{Impure, Pure, Resources, Util}
import controllers._
import game.mode.GameMode
import game.mode.skirmish.ProgressionHandler.handle
import game.state.{GameState, TurnState}
import game.{GameContext, Gameboard}
import models.{OwnedArmy, Player}

/**
  * Concrete implementation of GameMode bound for DI at runtime. Defines the rules
  * for the Skirmish mode from GOT Risk. See
  * [[https://theop.games/wp-content/uploads/2019/02/got_risk_rules.pdf]] for the rules
  */
class SkirmishGameMode extends GameMode {
  /** GameMode-specific gameboard, loaded through Resource injection */
  lazy val gameboard: Gameboard = Resources.SkirmishGameboard

  @Impure.Nondeterministic
  override def hookInitializeGame(implicit context: GameContext): GameContext =
    // Use sub-object to handle game initialization
    InitializationHandler.apply

  @Impure.Nondeterministic
  override def hookPacket(packet: InGamePacket)
                         (implicit context: GameContext): GameContext =
  // Use sub-object to handle packet validation
    ValidationHandler.process(packet) match {
      case ValidationResult(false, ctx) => ctx
      // Use sub-object to handle packet processing
      case ValidationResult(true, ctx) => ProgressionHandler.apply(packet)(ctx)
    }

  // TODO implement/refactor
  @Pure
  override def hookPlayerDisconnect(actor: PlayerWithActor)
                                   (implicit context: GameContext): GameContext = pass

  @Pure
  override def createGameState(turnOrder: IndexedSeq[PlayerWithActor]): GameState =
    GameState(turnOrder, Vector(), Vector(), gameboard)


  // TODO refactor
  @Impure.SideEffects
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
    state.clearPayloads()

    if (state.gameSize != 0 && state.stateOf(state.currentPlayer).get.turnState.state == TurnState.Idle) {
      state.advanceTurnState(None, "amount" -> calculateReinforcement(state.currentPlayer))
    }

    // Notify game of changes (no need to send to the disconnecting player)
    callback.broadcast(UpdateBoardState(state), Some(actor.id))
    callback.broadcast(UpdatePlayerState(state), Some(actor.id))
  }
}
