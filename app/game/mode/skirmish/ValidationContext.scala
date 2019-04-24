package game.mode.skirmish

import actors.PlayerWithActor
import common.Pure
import controllers.{RequestReply, RequestResponse}
import game.GameContext
import game.mode.skirmish.ValidationContext.{Drop, Reply}

object ValidationContext {
  sealed trait Mode
  case object Drop  extends Mode
  case object Reply extends Mode

  def begin: ValidationContext = ValidationContext(failed = false)
}

case class ValidationContext(failed: Boolean, message: Option[String] = None) {
  protected def pass: ValidationContext = ValidationContext(failed = false)
  protected def fail: ValidationContext = ValidationContext(failed = true)
  protected def fail(message: String): ValidationContext = ValidationContext(failed = true, Some(message))

  @Pure
  def check(test: => Boolean): ValidationContext =
    if (failed) {
      // no need to test
      this
    } else {
      // fail if expression evaluates to false
      if (test) pass else fail
    }

  @Pure
  def check(message: String)(test: => Boolean): ValidationContext =
    if (failed) {
      // no need to test
      this
    } else {
      // fail if expression evaluates to false
      if (test) pass else fail(message)
    }

  @Pure
  def checkFalse(test: => Boolean): ValidationContext =
    if (failed) {
      // no need to test
      this
    } else {
      // fail if expression evaluates to true
      if (test) fail else pass
    }

  @Pure
  def checkFalse(message: String)(test: => Boolean): ValidationContext =
    if (failed) {
      // no need to test
      this
    } else {
      // fail if expression evaluates to true
      if (test) fail(message) else pass
    }

  @Pure
  def consume(mode: ValidationContext.Mode)
             (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = mode match {
    case Drop => ValidationResult(!this.failed)
    case Reply => this match {
      case ValidationContext(true, messageOption) =>
        val msg = messageOption.getOrElse("")
        ValidationResult(result = false)(
          context
            .thenSend(RequestReply(RequestResponse.Rejected, msg), sender.id))

      case ValidationContext(true, _) =>
        ValidationResult(result =  true)(
          context
            .thenSend(RequestReply(RequestResponse.Accepted), sender.id))
    }
  }
}
