package game

import game.PlayerState.TurnState
import models.Player

object PlayerState {
  sealed trait State
  case object Reinforcement extends State
  case object Attack extends State
  case object Maneuver extends State
  case object Idle extends State
  object State {
    def unapply(arg: State): Option[String] = Some(arg match {
      case Reinforcement => "reinforcement"
      case Attack => "attack"
      case Maneuver => "maneuver"
      case Idle => "idle"
    })
  }

  case class TurnState(state: State, payload: (String, Any)*)
}

case class PlayerState(player: Player, units: Army, turnState: TurnState)
