package game.state

import common.Pure
import game.state.TurnState.State

object TurnState {

  object State {
    /**
      * Turns a State object singleton into its string mapping
      *
      * @param arg The State object singleton to serialize
      * @return A unique lowercase string identifier for the state
      */
    def unapply(arg: State): Option[String] = Some(arg match {
      case Reinforcement => "reinforcement"
      case Attack => "attack"
      case Maneuver => "maneuver"
      case Idle => "idle"
      case Defense => "defense"
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
  /** Temporary state that a non-current player is placed in when someone is attacking them */
  case object Defense extends State

  /**
    * Returns the next logical State in the game for a player
    * given their previous state
    * Note: it is assumed that the player associated with the
    * prev state has the current turn
    *
    * @param prev previous State of the player
    * @return succeeding state of the player
    */
  @Pure
  def nextState(prev: State): State = prev match {
    case Idle => Reinforcement
    case Reinforcement => Attack
    case Attack => Maneuver
    case Maneuver => Idle
    case _ => Idle
  }

  @Pure
  def nextDefendingState(prev: State): State = prev match {
    case Idle => Defense
    case Defense => Idle
    case _ => Idle
  }
}

/**
  * Turn state wrapper object holding both the state machine information
  * as well as any other serializable information
  *
  * @param state   The state machine value
  * @param payload An optional varargs of key -> value mappings
  */
case class TurnState(state: State, payload: (String, Any)*) {
  @Pure
  def advanceState(payload: (String, Any)*): TurnState =
    TurnState(TurnState.nextState(this.state), payload: _*)

  @Pure
  def advanceDefenseState(payload: (String, Any)*): TurnState = {
    TurnState(TurnState.nextDefendingState(this.state), payload: _*)
  }

  @Pure
  def clearPayload: TurnState = TurnState(this.state)
}
