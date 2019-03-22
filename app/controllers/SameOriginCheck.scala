package controllers

import play.api.Logger
import play.api.mvc.RequestHeader

/**
  * Performs a same origin check on request headers to only allow connections
  * with requests spawning from acceptable origins
  *
  * Sourced from [[https://github.com/playframework/play-scala-websocket-example]]
  * @author Will Sargent
  */
trait SameOriginCheck {
  /** Logger instance */
  def logger: Logger

  /**
    * Performs the same origin check against the request header (requires it)
    * @param rh The HTTP request header to examine
    * @return True if the header passes the check, false otherwise
    */
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

  /**
    * Tests whether the given path is a valid origin to respond to
    * @param origin The URL given by the client's origin header
    * @return True if the origin is acceptable, false otherwise
    */
  def validOrigin(origin: String): Boolean
}
