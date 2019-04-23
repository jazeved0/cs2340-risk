package game.mode.skirmish

import common.Pure
import controllers.InPacket
import game.GameContext

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
}
