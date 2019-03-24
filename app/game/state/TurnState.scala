package game.state

import game.state.TurnState.State

object TurnState {
  object State {
    /**
      * Turns a State object singleton into its string mapping
      * @param arg The State object singleton to serialize
      * @return A unique lowercase string identifier for the state
      */
    def unapply(arg: State): Option[String] = Some(arg match {
      case Reinforcement => "reinforcement"
      case Attack => "attack"
      case Maneuver => "maneuver"
      case Idle => "idle"
    })
  }
  /** Base trait for the turn state machine enumeration */
  sealed trait State
  /** Reinforcement turn phase, player picks troop locations */
  case object Reinforcement extends State
  /** Attack turn phase, player chooses to attack other territories */
  case object Attack extends State
  /** Maneuver phase, player can choose to redistribute troops */
  case object Maneuver extends State
  /** Default idle state, active whenever it isn't the player's "turn" */
  case object Idle extends State
}

/**
  * Turn state wrapper object holding both the state machine information
  * as well as any other serializable information
  * @param state The state machine value
  * @param payload An optional varargs of key -> value mappings
  */
case class TurnState(state: State, payload: (String, Any)*)
