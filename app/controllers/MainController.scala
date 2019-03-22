package controllers

import java.io.File

import actors.GameSupervisor.{CanHost, CanJoin, MakeGame}
import actors._
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{FlowShape, OverflowStrategy}
import akka.util.Timeout
import common.Resources
import common.Resources.StatusCodes
import javax.inject.{Inject, Named}
import models._
import play.api.cache.Cached
import play.api.data.Form
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
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
  extends MessagesAbstractController(cc) with SameOriginCheck {
  val logger: Logger = Logger(this.getClass)
  implicit val timeout: Timeout = 5.seconds

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
        Ok(views.html.index(Resources.UserForm, Resources.Colors, Resources.MakeUrl))
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

  // **************
  // ERROR HANDLING
  // **************

  /**
    * Redirects user to host index page if they do /lobby by mistake
    * @return a redirect to "/"
    */
  def redirectIndex: Action[AnyContent] = Action {
    // redirect to landing page
    Redirect("/")
  }

  /**
    * Exposes the public JSON config file to the client
    * @return a file send action (Ok) with the config file as the payload
    */
  def publicConfig: Action[AnyContent] = Action {
    Ok.sendFile(new File(Resources.PublicConfigPath))
  }

  /**
    * Builds a path by removing the first folder if it exists
    * @param root The base folder to replace the old one with (gets prepended)
    * @param base The original string to process
    * @return A modified filepath wrapped in a RelativeFile object
    */
  def formatFilepath(root: String)(implicit base: String): RelativeFile =
    RelativeFile(root + (base.indexOf('/') match {
      case -1 => base
      case i => base.substring(i)
    }))

  /**
    * Builds a path for the docs file
    * @param root The base folder to replace the old one with ("docs")
    * @param base The original string to process
    * @return A heavily modified filepath wrapped in a RelativeFile object,
    *         or a UrlRedirect if a redirect is necessary
    */
  def formatDocsFilepath(root: String)(implicit base: String): InitialFileResponse = {
    val substr = formatFilepath("").path
    if (substr == "docs") {
      UrlRedirect("docs/")
    } else {
      RelativeFile(root + (if (substr.indexOf('.') == -1) {
        substr + (if (substr.last == '/') "index.html" else ".html")
      } else {
        substr
      }))
    }
  }

  /** Represents the result of the initial file resolution stage */
  sealed trait InitialFileResponse
  /** Represents the result of the final file resolution stage */
  sealed trait FileResponse
  /** Represents a parsed and transformed relative filepath */
  case class RelativeFile(path: String) extends InitialFileResponse
  /** Represents a redirect response */
  case class UrlRedirect(to: String) extends FileResponse with InitialFileResponse
  /** Represents an error response */
  case class Error(message: String, code: Int = StatusCodes.NOT_FOUND) extends FileResponse with InitialFileResponse
  /** Represents a file that exists; wraps a file object */
  case class ResolvedFile(obj: File) extends FileResponse

  /**
    * Transforms a raw file path to the initial result of file resolution
    * @param path The raw file path included with the HTTP request
    * @return the result
    */
  def resolveFilepath(path: String): InitialFileResponse = {
    implicit val filepath: String = path
    path match {
      case p if p.startsWith("static") => formatFilepath("public")
      case p if p.startsWith("docs") =>
        if (Resources.DocsEnabled) {
          formatDocsFilepath(Resources.DocsRoot)
        } else {
          Error("Docs are not enabled", StatusCodes.MOVED_PERMANENTLY)
        }
      case p => RelativeFile(Resources.SpaFileRoot + p)
    }
  }

  /**
    * Transforms a raw file path to the final result of a file resolution
    * by chaining a call with <code>resolveFilePath(...)</code>
    * @param path The raw file path included with the HTTP request
    * @return the result
    */
  def resolveFile(path: String): FileResponse = {
    resolveFilepath(path) match {
      case e: Error => e
      case r: UrlRedirect => r
      case RelativeFile(filename) =>
        val file = new File(filename)
        if (file.exists) ResolvedFile(file) else Error(s"Can't find $path")
    }
  }

  /**
    * Router method to route file requests (default) to their proper targets
    * @param path The raw file path included with the HTTP request
    * @return either an error page in HTML, a redirect to another resource,
    *         or the actual file (if it was successfully resolved)
    */
  def routeFiles(path: String): Action[AnyContent] = Action {
    resolveFile(path) match {
      case Error(m, c) => ErrorHandler.renderErrorPage(c, m)
      case ResolvedFile(file) => Ok.sendFile(file)
      case UrlRedirect(to) => Redirect(to)
    }
  }

  // ***********
  // WEB SOCKETS
  // ***********

  /**
    * Builds a websocket connection and its associated flow graph if the
    * request was approved, and rejects the connection otherwise
    *
    * GET ws://[origin]/webSocket/gameId/playerId
    * @param gameId The game ID to request a websocket connection for
    * @param playerId The pseudo-secret unique ID of the connecting client
    * @return
    */
  def webSocket(gameId: String, playerId: String): WebSocket = {
    WebSocket.acceptOrResult[InPacket, OutPacket] {
      // validate supplied ids
      case _ if !Player.isValidId(playerId) =>
        Future.successful {
          Left(BadRequest(s"Invalid player id $playerId supplied"))
        }
      case _ if !Game.isValidId(gameId) =>
        Future.successful {
          Left(BadRequest(s"Invalid app id $gameId supplied"))
        }
      case header if sameOriginCheck(header) =>
        Future.successful(flow(gameId, playerId)).map { flow =>
          Right(flow)
        }.recover {
          case _: Exception =>
            Left(InternalServerError("Cannot create websocket"))
        }
      case _ =>
        Future.successful {
          Left(Forbidden("forbidden"))
        }
    }
  }

  import controllers.JsonMarshallers._

  /**
    * Uses <code>JsonMarshallers</code> to transform incoming websocket JSON
    * to InPackets, and outgoing OutPackets to outgoing websocket JSON implicitly
    * within the actor flow
    */
  implicit val messageFlowTransformer: MessageFlowTransformer[InPacket, OutPacket] =
    MessageFlowTransformer.jsonMessageFlowTransformer[InPacket, OutPacket]
  /** Defines the creation method for ActorRefs that get attached to player DTOs */
  val playerActorSource: Source[OutPacket, ActorRef] =
    Source.actorRef[OutPacket](Resources.IncomingPacketBufferSize, OverflowStrategy.fail)

  /**
    * Builds a flow graph for each WebSocket connection, forming a closure with
    * the parameters for the graph creation & processing functions
    *
    * Graph diagram
    *
    * --------------------------------------------------------------------------
    *
    * connect ^^\
    *
    *            o merge o---> gameSupervisor.receive() --- (...) ---> OutPacket
    *
    * InPacket _/              [can degrade to PlayerDisconnect]
    *
    * --------------------------------------------------------------------------
    * @param gameId The game ID to create a websocket flow for
    * @param playerId The pseudo-secret unique ID of the connecting client
    * @return A flow graph taking in an InPacket, outputting an OutPacket, and
    *         using an ActorRef as the intermediate processing type
    */
  def flow(gameId: String, playerId: String): Flow[InPacket, OutPacket, ActorRef] = {
    Flow.fromGraph(GraphDSL.create(playerActorSource) {
      implicit builder =>
        playerActor =>
          import akka.stream.scaladsl.GraphDSL.Implicits._

          // Join & Network entry points for InPackets
          val materialization = builder.materializedValue.map(playerActor =>
            PlayerConnect(gameId, playerId, playerActor))
          val incomingRouter: FlowShape[InPacket, InPacket] = builder.add(Flow[InPacket])

          // Merge Join & Network sources
          val merge = builder.add(Merge[InPacket](2))
          materialization ~> merge
          incomingRouter ~> merge

          // Output for messages (with default for ones that don't get processed
          // (dead connection))
          val gameSupervisorSink = Sink.actorRef[InPacket](gameSupervisor,
            PlayerDisconnect(gameId, playerId))
          merge ~> gameSupervisorSink

          // Set the WebSocket points of ingress and egress
          FlowShape(incomingRouter.in, playerActor.out)
    })
  }

  override def validOrigin(path: String): Boolean =
    Resources.Origins.exists(path.contains(_))
}
