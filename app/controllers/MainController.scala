package controllers

import actors.LobbySupervisor.{LobbyExists, MakeLobby}
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
  * @param lobbySupervisor The application's root actor, used to manage lobbies
  * @param ec Implicit context
  */
class MainController @Inject()(cached: Cached,
                               cc: MessagesControllerComponents,
                               config: Configuration,
                               actorSystem: ActorSystem,
                               @Named("lobby-supervisor") lobbySupervisor: ActorRef)
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
    Action {
      implicit request =>
        // send landing page to the client (host)
        Ok(views.html.index(Resources.UserForm, Resources.Colors, Resources.MakeUrl))
    }
  }

  // POST /lobby/make
  def make: Action[AnyContent] = Action.async { implicit request =>
    val formValidationResult: Form[ClientSettings] = Resources.UserForm.bindFromRequest
    formValidationResult.fold(
      _ => Future[Result](BadRequest("Form submission failed")),
      userData => {
        if (!ClientSettings.isValid(userData))
          Future[Result](BadRequest(ClientSettings.formatInvalid(userData)))
        else {
          val hostInfo = ClientSettings(userData.name, userData.ordinal)
          (lobbySupervisor ? MakeLobby(hostInfo)).mapTo[String].map { id =>
            Redirect(s"/lobby/host/$id")
          }
        }
      }
    )
  }

  // Obtains the corresponding main page after a host has created
  // GET /lobby/host/:id
  def host(id: String): Action[AnyContent] = Action.async { implicit request =>
    (lobbySupervisor ? LobbyExists(id)).mapTo[Boolean].map {
      case true => Ok(views.html.main(id, isHost = true, Resources.NonHostSubmitURL))
        .withCookies(makeClientIdCookie)
      case false => BadRequest(s"Invalid lobby id $id")
    }
  }

  // *******************
  // NON-HOST HTTP CALLS
  // *******************

  // This is the entry points for *non-hosts*; it gives them
  // the page responsible for them setting their name & color
  // and then joining the existing game
  // GET /lobby/:id
  def lobby(id: String): EssentialAction = cached(s"lobby-$id") {
    Action.async { implicit request =>
      (lobbySupervisor ? LobbyExists(id)).mapTo[Boolean].map {
        case true => Ok(views.html.main(id, isHost = false, Resources.NonHostSubmitURL))
          .withCookies(makeClientIdCookie)
        case false => BadRequest(s"Invalid lobby id $id")
      }
    }
  }

  // generates client Id cookies for the frontend to consume
  def makeClientIdCookie: Cookie = {
    val id = Client.generateAndIssueId
    Cookie(Resources.ClientIdCookieKey,
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

  // TODO Add pages for error handling (404s, forbidden)
  // TODO Add page for invalid ID (either POST or GET)

  // ***********
  // WEB SOCKETS
  // ***********

  // webSocket/lobbyId/clientId
  def webSocket(lobbyId: String, clientId: String): WebSocket = {
    WebSocket.acceptOrResult[InPacket, OutPacket] {
      // validate supplied ids
      case _ if !Client.isValidId(clientId) =>
        Future.successful {
          Left(BadRequest(s"Invalid client id $clientId supplied"))
        }
      case _ if !Lobby.isValidId(lobbyId) =>
        Future.successful {
          Left(BadRequest(s"Invalid lobby id $lobbyId supplied"))
        }
      case header if sameOriginCheck(header) =>
        Future.successful(flow(lobbyId, clientId)).map { flow =>
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
  val clientActorSource: Source[OutPacket, ActorRef] =
    Source.actorRef[OutPacket](5, OverflowStrategy.fail)

  // Builds a flow for each WebSocket connection
  def flow(lobbyId: String, clientId: String): Flow[InPacket, OutPacket, ActorRef] = {
    Flow.fromGraph(GraphDSL.create(clientActorSource) {
      implicit builder => clientActor =>
        import GraphDSL.Implicits._
        // TODO implement ping/pong

        // Join & Network entry points for InPackets
        val materialization = builder.materializedValue.map(clientActor =>
          ClientConnect(lobbyId, clientId, clientActor))
        val incomingRouter: FlowShape[InPacket, InPacket] = builder.add(Flow[InPacket])

        // Merge Join & Network sources
        val merge = builder.add(Merge[InPacket](2))
        materialization ~> merge
        incomingRouter ~> merge

        // Output for messages that don't get processed (dead connection)
        val lobbySink = Sink.actorRef[InPacket](lobbySupervisor,
          ClientDisconnect(lobbyId, clientId))
        merge ~> lobbySink

        // Set the WebSocket points of ingress and egress
        FlowShape(incomingRouter.in, clientActor.out)
    })
  }

  override def validOrigin(path: String): Boolean = config.get
    [Seq[String]](Resources.OriginsConfigKey).exists(path.contains(_))
}

/**
  * Sourced from https://github.com/playframework/play-scala-websocket-example/
  *     blob/2.7.x/app/controllers/HomeController.scala
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
