package controllers

import javax.inject.{Inject, Provider, Singleton}
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}

import scala.concurrent._

/**
  * Renders error HTML pages for the application
  * @param env the Environment given by DI
  * @param config the Configuration object given by DI
  * @param sourceMapper the source mapper object given by DI
  * @param router the Router provider given by DI
  */
@Singleton
class ErrorHandler @Inject() (env: Environment,
                              config: Configuration,
                              sourceMapper: OptionalSourceMapper,
                              router: Provider[Router])
    extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {
  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      ErrorHandler.renderErrorPage(statusCode, message)
    )
  }

  override def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }
}

object ErrorHandler {
  /**
    * Generates an error page for the given status code with the error message
    * @param statusCode The response status code
    * @param message The accompanying error message
    * @return A Result object containing the rendered page
    */
  def renderErrorPage(statusCode: Int, message: String): Result = {
    Status(statusCode)(views.html.error(message)(statusToString(statusCode)))
  }

  /**
    * Formats the status code with a map of pre-translated human-readable
    * names for the status codes
    * @param code The status code to translate
    * @return A string like "404 (NOT_FOUND)" or 404 if not found in the map
    */
  def statusToString(code: Int): String = {
    val map: Map[Int, String] = Map(
      404 -> "NOT_FOUND",
      401 -> "UNAUTHORIZED",
      400 -> "BAD_REQUEST",
      301 -> "MOVED_PERMANENTLY")
    code.toString + map.get(code).map(s => s" ($s)").getOrElse("")
  }
}
