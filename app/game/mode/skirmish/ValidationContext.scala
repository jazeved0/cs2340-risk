package game.mode.skirmish

import actors.PlayerWithActor
import common.{Impure, Pure}
import controllers.{RequestReply, RequestResponse}
import game.GameContext
import game.mode.skirmish.ValidationContext.{Drop, Reply}
import org.slf4j
import play.api.Logger

object ValidationContext {
  // logging members
  val logger: slf4j.Logger = Logger(this.getClass).logger

  /**
    * Logs a failed validation to the debug logger level
    *
    * @param logName The name of the class in the logging output
    * @param message The reason of the failed validation
    * @param sender  The player actor that initiated the request
    */
  @Impure.SideEffects
  def log(logName: String, message: String)(implicit sender: PlayerWithActor): Unit = {
    val name = sender.player.settings.map(ps => ps.name).getOrElse("")
    val id   = sender.id
    logger.warn(s"Validation failed when handling <$logName> packet sent by '$name' [id=$id]: $message")
  }

  sealed trait Mode
  case object Drop  extends Mode
  case object Reply extends Mode

  /**
    * Begins a validation pipeline
    *
    * @param name [Optional]: The name of the packet
    * @return A blank validation context object
    */
  def begin(name: String = ""): ValidationContext = ValidationContext(failed = false, name)
}

/**
  * Represents an intermediate object in the validation pipeline that allows checks
  * to be performed consecutively, skipping later ones once the validation fails
  * @param failed  Whether or not the current validation has already failed
  * @param name    [Optional]: The name of the packet
  * @param message [Optional]: The reason of the failed validation
  */
case class ValidationContext(failed: Boolean, name: String = "",  message: Option[String] = None) {
  protected def pass: ValidationContext = ValidationContext(failed = false, this.name)
  protected def fail: ValidationContext = ValidationContext(failed = true, this.name)
  protected def fail(message: String): ValidationContext = ValidationContext(failed = true, this.name, Some(message))

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

  /**
    * Consumes the validation context object and turns it into outgoing packets
    * as necessary
    *
    * @param mode    The consumption mode to use
    * @param context Incoming context wrapping current game state
    * @param sender  The player actor that initiated the request
    * @return A ValidationResult object that wraps the updated game context/result
    */
  @Pure
  def consume(mode: ValidationContext.Mode)
             (implicit context: GameContext, sender: PlayerWithActor): ValidationResult = {
    val logName = name match {
      case "" => "ValidationContext"
      case s  => s
    }
    // Log failure
    if (this.failed) ValidationContext.log(logName, message.getOrElse(""))
    mode match {
      case Drop => ValidationResult(!this.failed)
      case Reply => this match {
        case ValidationContext(true, _, messageOption) =>
          val msg = messageOption
            .map(s => s"[$logName] Validation failed: $s")
            .getOrElse("[$logName] Validation failed")
          ValidationResult(result = false)(
            context
              .thenSend(RequestReply(RequestResponse.Rejected, msg), sender.id))

        case ValidationContext(false, _, _) =>
          ValidationResult(result =  true)(
            context
              .thenSend(RequestReply(RequestResponse.Accepted), sender.id))
      }
    }
  }
}
