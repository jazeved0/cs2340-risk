package controllers

import play.api.Logger
import play.api.mvc.RequestHeader

/**
  * Sourced from https://github.com/playframework/play-scala-websocket-example/
  *     blob/2.7.x/app/controllers/HomeController.scala
  *
  * @author Will Sargent
  */
trait SameOriginCheck{
  def logger: Logger
  def sameOriginCheck(rh: RequestHeader): Boolean = {
    rh.headers.get("Origin") match {
      case Some(originValue) if validOrigin(originValue) =>
        logger.debug(s"[OriginCheck] OriginValue = $originValue")
        true
      case Some(badOrigin) =>
        logger.warn(s"[OriginCheck] Rejecting request because origin " +
          s"$badOrigin is invalid")
        false
      case None =>
        logger.warn("[OriginCheck] Rejecting request because no " +
          "Origin header found")
        false
    }
  }
  def validOrigin(origin: String): Boolean
}
