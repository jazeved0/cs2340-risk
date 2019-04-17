package controllers

import java.io.File

import actors.GameSupervisor.{CanHost, CanJoin, MakeGame}
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import common.Resources
import common.Resources.StatusCodes
import javax.inject.{Inject, Named}
import models._
import play.api.Configuration
import play.api.cache.Cached
import play.api.data.Form
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * The primary router for incoming network messages, whether they be over
  * HTTP/HTTPS or through the WebSocket protocol
  *
  * Performs a same origin check upon websocket handshake
  *
  * @param cached         The caching API endpoint
  * @param cc             Implicit component helper
  * @param config         The application's configuration
  * @param actorSystem    The application's single actor system for managing state
  * @param gameSupervisor The application's root actor, used to manage games
  * @param ec             Implicit context
  */
class MainController @Inject()(cached: Cached,
                               cc: MessagesControllerComponents,
                               config: Configuration,
                               actorSystem: ActorSystem,
                               @Named("game-supervisor") gameSupervisor: ActorRef)
                              (implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {
  val files = new FileController(cc)
  val webSocket = new WebSocketController(cc, gameSupervisor)
  implicit val timeout: Timeout = Resources.AskTimeout

  // ***************
  // Host HTTP calls
  // ***************

  /**
    * This is the entry point for the host; this loads the page at which
    * a new game is made. Additionally, displays general information
    *
    * GET /
    * @return the response to the router action
    */
  def index: EssentialAction = cached("indexPage") {
    Action.apply {
      implicit request =>
        // send landing page to the player (host)
        Ok(views.html.index(Resources.UserForm, Resources.Colors, Resources.InitialFormPostUrl))
    }
  }

  /**
    * Docs page
    *
    * GET /docs/
    * @return the response to the router action
    */
  def docs: EssentialAction = cached("docsPage") {
    Action.apply {
      implicit request =>
        // send docs page
        Ok(views.html.docs())
    }
  }

  /**
    * Request upon form submission of index page that requests to make and host
    * a new lobby
    *
    * POST /lobby/make
    * @return the response to the router action
    */
  def make: Action[AnyContent] = Action.async { implicit request =>
    val formValidationResult: Form[PlayerSettings] = Resources.UserForm.bindFromRequest
    formValidationResult.fold(
      _ => Future[Result](ErrorHandler.renderErrorPage(StatusCodes.BAD_REQUEST,
        "Form submission failed")),
      userData => {
        if (!PlayerSettings.isValid(userData)) {
          Future[Result](ErrorHandler.renderErrorPage(StatusCodes.BAD_REQUEST,
            PlayerSettings.formatInvalid(userData)))
        } else {
          val hostInfo = PlayerSettings(userData.name, userData.ordinal)
          (gameSupervisor ? MakeGame(hostInfo)).mapTo[String].map { id =>
            Redirect(s"/lobby/host/$id")
          }
        }
      }
    )
  }

  /**
    * Obtains the corresponding SPA game page after a host has created a
    * lobby (specific to the host, can only be loaded once)
    *
    * GET /lobby/host/:id
    * @param id The gameId to load the lobby page of (and to take over)
    * @return the response to the router action
    */
  def host(id: String): Action[AnyContent] = Action.async { implicit request =>
    (gameSupervisor ? CanHost(id)).mapTo[CanHost.Value].map {
      case CanHost.Yes => spaEntryPoint
      case CanHost.Started => ErrorHandler.renderErrorPage(StatusCodes.UNAUTHORIZED,
        "Game has already started")
      case CanHost.Hosted => Redirect(s"/lobby/$id")
      case CanHost.InvalidId => ErrorHandler.renderErrorPage(StatusCodes.NOT_FOUND,
        s"Invalid game id $id")
      case _ => Redirect("/")
    }
  }

  // *******************
  // NON-HOST HTTP CALLS
  // *******************

  /**
    * This is the entry points for *non-hosts*; it gives them the page
    * responsible for them setting their name & color and then joining the
    * existing game
    *
    * GET /lobby/:id
    * @param id The gameId to load the lobby page of
    * @return the response to the router action
    */
  def lobby(id: String): Action[AnyContent] = Action.async { implicit request =>
    (gameSupervisor ? CanJoin(id)).mapTo[CanJoin.Value].map {
      case CanJoin.Yes => spaEntryPoint
      case CanJoin.Started => ErrorHandler.renderErrorPage(StatusCodes.UNAUTHORIZED,
        "Game has already started")
      case CanJoin.InvalidId => ErrorHandler.renderErrorPage(StatusCodes.NOT_FOUND,
        s"Invalid game id $id")
      case _ => Redirect("/")
    }
  }

  /**
    * Creates a file response for the spa entry point, or an error if the file
    * doesn't exist
    * @param header HTTP request header
    * @return the response to the router action
    */
  def spaEntryPoint()(implicit header: RequestHeader): Result = {
    val f = new File(Resources.SpaEntryPoint)
    f match {
      case file if file.exists => Ok.sendFile(file).withCookies(makePlayerIdCookie)
      case _ => ErrorHandler.renderErrorPage(StatusCodes.NOT_FOUND,
        "Game application not found")
    }
  }

  /**
    * Generates player Id cookies for the frontend to consume- used as a
    * pseudo-secret between the client and server: should only be known to
    * the client and not the other players in the game, but is not sensitive
    * information and looses significance after the game
    * @param request HTTP request header
    * @return the generated cookie object
    */
  def makePlayerIdCookie()(implicit request: RequestHeader): Cookie = {
    val id = Player.generateAndIssueId
    Cookie(Resources.PlayerIdCookie,
      id, httpOnly = false)
  }

  /**
    * Redirects user to host index page if they do /lobby by mistake
    * @return a redirect to "/"
    */
  def redirectIndex: Action[AnyContent] = Action {
    // redirect to landing page
    Redirect("/")
  }

  /**
    * Router method to route file requests (default) to their proper targets
    * @param path The raw file path included with the HTTP request
    * @return either an error page in HTML, a redirect to another resource,
    *         or the actual file (if it was successfully resolved)
    */
  def routeFiles(path: String): Action[AnyContent] = files.route(path)

  /**
    * Builds a websocket connection and its associated flow graph if the
    * request was approved, and rejects the connection otherwise
    *
    * GET ws://[origin]/webSocket/gameId/playerId
    * @param gameId The game ID to request a websocket connection for
    * @param playerId The pseudo-secret unique ID of the connecting client
    * @return
    */
  def webSocket(gameId: String, playerId: String): WebSocket = webSocket.build(gameId, playerId)
}
