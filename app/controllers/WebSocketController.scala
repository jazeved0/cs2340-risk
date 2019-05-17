package controllers

import actors.Game
import akka.actor.ActorRef
import akka.stream.{FlowShape, OverflowStrategy}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source}
import common.Resources
import models.Player
import play.api.Logger
import play.api.mvc.{MessagesBaseController, MessagesControllerComponents, WebSocket}
import play.api.mvc.WebSocket.MessageFlowTransformer

import scala.concurrent.{ExecutionContext, Future}

class WebSocketController(val controllerComponents: MessagesControllerComponents,
                          val gameSupervisor: ActorRef)
                         (implicit ec: ExecutionContext)
    extends MessagesBaseController with SameOriginCheck {
  val logger: Logger = Logger(this.getClass)
  override def validOrigin(path: String): Boolean = Resources.Origins.exists(path.contains(_))

  /**
    * Builds a websocket connection and its associated flow graph if the
    * request was approved, and rejects the connection otherwise
    *
    * GET ws://[origin]/webSocket/gameId/playerId
    * @param gameId The game ID to request a websocket connection for
    * @param playerId The pseudo-secret unique ID of the connecting client
    * @return
    */
  def build(gameId: String, playerId: String): WebSocket = {
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

  import controllers.format.JsonMarshallers._

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
}
