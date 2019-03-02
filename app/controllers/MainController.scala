package controllers

import java.io.File

import actors.GameSupervisor.{CanHost, GameExists, MakeGame}
import actors._
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{FlowShape, OverflowStrategy}
import akka.util.Timeout
import common.Resources
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
  * Is the primary router for incoming network messages, whether they be over
  * HTTP or through the WebSocket protocol
  * @param cached The caching API endpoint
  * @param cc Implicit component helper
  * @param config The application's configuration
  * @param actorSystem The application's single actor system for managing state
  * @param gameSupervisor The application's root actor, used to manage games
  * @param ec Implicit context
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

  // This is the entry point for the host; this loads the page
  // at which a new game is made
  // GET /
  def index: EssentialAction = cached("indexPage") {
    Action.apply {
      implicit request =>
        // send landing page to the player (host)
        Ok(views.html.index(Resources.UserForm, Resources.Colors, Resources.MakeUrl))
    }
  }

  // POST /lobby/make
  def make: Action[AnyContent] = Action.async { implicit request =>
    val formValidationResult: Form[PlayerSettings] = Resources.UserForm.bindFromRequest
    formValidationResult.fold(
      _ => Future[Result](BadRequest("Form submission failed")),
      userData => {
        if (!PlayerSettings.isValid(userData)) {
          Future[Result](BadRequest(PlayerSettings.formatInvalid(userData)))
        } else {
          val hostInfo = PlayerSettings(userData.name, userData.ordinal)
          (gameSupervisor ? MakeGame(hostInfo)).mapTo[String].map { id =>
            Redirect(s"/lobby/host/$id")
          }
        }
      }
    )
  }

  // Obtains the corresponding main page after a host has created
  // GET /lobby/host/:id
  def host(id: String): Action[AnyContent] = Action.async { implicit request =>
    (gameSupervisor ? CanHost(id)).mapTo[CanHost.Value].map {
      case CanHost.Yes =>
        Ok.sendFile(new File("vue/dist/index.html"))
        .withCookies(makePlayerIdCookie)
      case CanHost.InvalidId => BadRequest(s"Invalid app id $id")
      case _ => Redirect("/")
    }
  }

  // *******************
  // NON-HOST HTTP CALLS
  // *******************

  // This is the entry points for *non-hosts*; it gives them
  // the page responsible for them setting their name & color
  // and then joining the existing game
  // GET /lobby/:id
  def lobby(id: String): Action[AnyContent] = Action.async { implicit request =>
    (gameSupervisor ? GameExists(id)).mapTo[Boolean].map {
      case true =>
        Ok.sendFile(new File("vue/dist/index.html"))
        .withCookies(makePlayerIdCookie)
      case false => BadRequest(s"Invalid app id $id")
    }
  }

  // generates player Id cookies for the frontend to consume
  def makePlayerIdCookie(implicit request: RequestHeader): Cookie = {
    val id = Player.generateAndIssueId
    Cookie(Resources.PlayerIdCookie,
      id, httpOnly = false)
  }

  // **************
  // ERROR HANDLING
  // **************

  //Redirects user to host index page if they do /main by mistake
  def redirectIndex: Action[AnyContent] = Action {
    // redirect to landing page
    Redirect("/")
  }

  // ********************
  // EXPOSE PUBLIC CONFIG
  // ********************

  def publicConfig: Action[AnyContent] = Action {
    Ok.sendFile(new File(Resources.PublicConfigPath))
  }

  def routeFiles(path: String): Action[AnyContent] = Action {
    val file = path match {
      case p if p.startsWith("static") => new File("public" + path.substring(path.indexOf('/')))
      case _ => new File("vue/dist/" + path)
    }
    if (file.exists) Ok.sendFile(file) else NotFound(s"Can't find $path")
  }

  // TODO Add pages for error handling (404s, forbidden)
  // TODO Add page for invalid ID (either POST or GET)

  // ***********
  // WEB SOCKETS
  // ***********

  // webSocket/gameId/playerId
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
  implicit val messageFlowTransformer: MessageFlowTransformer[InPacket, OutPacket] =
    MessageFlowTransformer.jsonMessageFlowTransformer[InPacket, OutPacket]
  val playerActorSource: Source[OutPacket, ActorRef] =
    Source.actorRef[OutPacket](Resources.IncomingPacketBufferSize, OverflowStrategy.fail)

  // Builds a flow for each WebSocket connection
  def flow(gameId: String, playerId: String): Flow[InPacket, OutPacket, ActorRef] = {
    Flow.fromGraph(GraphDSL.create(playerActorSource) {
      implicit builder => playerActor =>
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

  override def validOrigin(path: String): Boolean = Resources.Origins.exists(path.contains(_))
}
