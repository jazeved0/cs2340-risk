package game.mode

import actors.PlayerWithActor
import controllers.{InGamePacket, OutPacket, SendGameboard, UpdatePlayerState}
import game.{GameState, Gameboard}

import scala.collection.mutable

trait GameMode {
  def makeGameState(turnOrder: Seq[PlayerWithActor]): GameState =
    new GameState(IndexedSeq() ++ turnOrder, gameboard.nodeCount)

  def initializeGame(joinOrder: List[PlayerWithActor],
                     broadcastCallback: (OutPacket, Option[String]) => Unit,
                     sendCallback: (OutPacket, String) => Unit): GameState = {
    // Use latent sending callbacks to preserve send order
    sealed trait CallbackFlag
    case object Broadcast extends CallbackFlag
    case object Send extends CallbackFlag
    val sends: mutable.MutableList[(OutPacket, CallbackFlag, Option[String])] = mutable.MutableList()
    val latentBroadcast: (OutPacket, Option[String]) => Unit = (p, o) => sends += ((p, Broadcast, o))
    val latentSend: (OutPacket, String) => Unit = (p, o) => sends += ((p, Send, Some(o)))

    // default game initialization behavior:
    //    1. Broadcast the proper gameboard
    //    2. Call assignTurnOrder to determine turn order
    //    3. Call makeGameState with the previously assigned turn order
    //    4. Call initializeGameState to perform any initialization actions
    //    5. Broadcast the generated player state
    //    6. Broadcast and send any other OutPackets from the latent callbacks
    broadcastCallback(SendGameboard(gameboard), None)
    val turnOrder: Seq[PlayerWithActor] = assignTurnOrder(joinOrder)
    val gameState = makeGameState(turnOrder)
    initializeGameState(gameState, latentBroadcast, latentSend)
    broadcastCallback(UpdatePlayerState(gameState.playerStates), None)

    // Consume earlier built latent callbacks
    sends.foreach((t: (OutPacket, CallbackFlag, Option[String])) => t._2 match {
      case Broadcast => broadcastCallback.apply(t._1, t._3)
      case Send => sendCallback.apply(t._1, t._3.getOrElse(""))
    })

    gameState
  }

  // Abstract getter (implement with lazy val if possible)
  def gameboard: Gameboard

  // Abstract methods
  def assignTurnOrder(players: Seq[PlayerWithActor]): Seq[PlayerWithActor]
  def initializeGameState(state: GameState,
                          broadcastCallback: (OutPacket, Option[String]) => Unit,
                          sendCallback: (OutPacket, String) => Unit): Unit
  def handlePacket(packet: InGamePacket,
                   state: GameState,
                   broadcastCallback: (OutPacket, Option[String]) => Unit,
                   sendCallback: (OutPacket, String) => Unit): Unit
  def playerDisconnect(player: PlayerWithActor,
                       s: GameState,
                       broadcastCallback: (OutPacket, Option[String]) => Unit,
                       sendCallback: (OutPacket, String) => Unit): Unit
}
