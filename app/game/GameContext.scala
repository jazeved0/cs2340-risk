package game

import common.{Impure, Pure}
import controllers.OutPacket
import game.GameContext.{Broadcast, PacketContext, Send}
import game.state.GameState

import scala.annotation.tailrec

object GameContext {
  /** Used as a generic identifier for the send packet functionality to use. */
  sealed trait PacketFlag
  /** A entire-game-lobby broadcast, with the option to exclude one actor */
  case object Broadcast extends PacketFlag
  /** A targeted message send */
  case object Send extends PacketFlag
  /**
    * Wraps a single packet send with contextual information
    * @param packet The packet to send
    * @param targetContext For send, the Id of the recipient; for broadcast,
    *                      the optional Id of the player to exclude
    * @param flag The type of packet send
    */
  case class PacketContext(packet: OutPacket, targetContext: Option[String], flag: PacketFlag)

  /**
    * Default factory method for instantiating a game context from a state object
    * @param state The game state instance
    * @return New instance of GameContext with an empty packet order
    */
  def apply(state: GameState): GameContext = new GameContext(state, Nil)
}

/**
  * Wraps the return value for game state processing functions that allows for
  * the latent sending of network packets following processing
  * @param state The game state instance
  * @param packetOrder A list of packet contexts in reverse sending order
  */
case class GameContext(state: GameState, private val packetOrder: List[PacketContext]) {
  /**
    * Creates a new GameContext object, appending a Send packet context to the
    * packet order
    * @param packet The packet to send
    * @param recipientId The player Id of the actor to send the packet to
    * @return A new game context instance
    */
  @Pure
  def thenSend(packet: OutPacket, recipientId: String): GameContext =
    this(state, PacketContext(packet, Some(recipientId), Send) :: packetOrder)

  /**
    * Creates a new GameContext object, appending a Broadcast packet to the
    * packet order
    * @param packet The packet to broadcast
    * @param recipientId An optional player Id of the player to exclude from the
    *                    broadcast
    * @return A new game context instance
    */
  @Pure
  def thenBroadcast(packet: OutPacket, recipientId: String = ""): GameContext = recipientId match {
    case "" => this(state, PacketContext(packet, None,     Broadcast) :: packetOrder)
    case id => this(state, PacketContext(packet, Some(id), Broadcast) :: packetOrder)
  }

  /**
    * Creates a new GameContext object, adding each packet of the source context
    * to the packet order of the current instance
    * @param source The other context object with the packet order to use
    * @return A new game context instance with the state preserved
    */
  @Pure
  def thenSendAll(source: GameContext): GameContext =
    this(state, source.packetOrder ::: packetOrder)

  /**
    * Creates a new GameContext object, modifying the internal game state
    * instance to be a different one
    * @param newState The new game state to wrap
    * @return A new game context instance with the old packet order preserved
    */
  @Pure
  def withState(newState: GameState): GameContext = this(newState, packetOrder)

  /**
    * Creates a new GameContext object, mapping the internal game state
    * instance to be a different one
    * @param func The mapping function to apply
    * @return A new game context instance with the old packet order preserved
    */
  @Pure
  def map(func: GameState => GameState): GameContext = this(func(state), packetOrder)

  /**
    * Sends all packets in the current packet order, calling the given broadcast
    * functions as appropriate
    * @param sendFunc The function to called to send a directed packet
    * @param broadcastFunc The function called to broadcast a packet
    */
  @Impure.SideEffects
  def sendPackets(sendFunc: (OutPacket, String) => Unit,
                  broadcastFunc: (OutPacket, Option[String]) => Unit): Unit = {
    @tailrec
    def sendNext(packets: List[PacketContext]): Unit = packets match {
      case Nil => // base case
      case (last: PacketContext) :: Nil       => send(last)
      case (next: PacketContext) :: remainder =>
        send(next)
        sendNext(remainder)
    }

    def send(ctx: PacketContext): Unit = ctx match {
      case PacketContext(_, Some(target), Send) => sendFunc(ctx.packet, target)
      case PacketContext(_, _, Broadcast) => broadcastFunc(ctx.packet, ctx.targetContext)
      case _ => // do nothing
    }

    // Process packets in order of addition
    sendNext(this.packetOrder.reverse)
  }

  /**
    * Consumes the GameContext and returns the GameState. Designed to be used
    * at the end of the processing pipeline
    * @param sendFunc The function to called to send a directed packet
    * @param broadcastFunc The function called to broadcast a packet
    * @return The inner game state object
    */
  @Impure.SideEffects
  def consume(sendFunc: (OutPacket, String) => Unit,
              broadcastFunc: (OutPacket, Option[String]) => Unit): GameState = {
    sendPackets(sendFunc, broadcastFunc)
    state
  }
}
