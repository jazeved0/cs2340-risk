package game.mode

import actors.PlayerWithActor
import controllers.{InGamePacket, OutPacket, SendGameboard, UpdatePlayerState}
import game.mode.GameMode._
import game.{GameState, Gameboard}

import scala.collection.mutable

object GameMode {
  // Callback parameter wrapper
  case class Callback(broadcast: (OutPacket, Option[String]) => Unit,
                      send: (OutPacket, String) => Unit) {
    def apply(payload: (OutPacket, Option[String]), flag: CallbackFlag): Unit = {
      flag match {
        case Broadcast => broadcast(payload._1, payload._2)
        case Send => send(payload._1, payload._2.getOrElse(""))
      }
    }
  }
  sealed trait CallbackFlag
  case object Broadcast extends CallbackFlag
  case object Send extends CallbackFlag
}

trait GameMode {
  def makeGameState(turnOrder: Seq[PlayerWithActor]): GameState =
    new GameState(IndexedSeq() ++ turnOrder, gameboard.nodeCount)

  def initializeGame(joinOrder: List[PlayerWithActor], callback: Callback): GameState = {
    // Use latent sending callbacks to preserve send order

    val messageQueue: mutable.Queue[(CallbackFlag, (OutPacket, Option[String]))] = mutable.Queue()
    val latentCallback = Callback(
      (p: OutPacket, s: Option[String]) => messageQueue += ((Broadcast, (p, s))),
      (p: OutPacket, s: String) => messageQueue += ((Send, (p, Some(s))))
    )

    // default game initialization behavior:
    //    1. Broadcast the proper gameboard
    //    2. Call assignTurnOrder to determine turn order
    //    3. Call makeGameState with the previously assigned turn order
    //    4. Call initializeGameState to perform any initialization actions
    //    5. Broadcast the generated player state
    //    6. Broadcast and send any other OutPackets from the latent callbacks
    callback.broadcast(SendGameboard(gameboard), None)
    val turnOrder: Seq[PlayerWithActor] = assignTurnOrder(joinOrder)
    val gameState = makeGameState(turnOrder)
    initializeGameState(gameState, latentCallback)
    callback.broadcast(UpdatePlayerState(gameState.playerStates), None)

    // Consume earlier built latent callbacks
    messageQueue.foreach { case (flag, payload) => callback(payload, flag) }
    gameState
  }

  // Abstract getter (implement with lazy val if possible)
  def gameboard: Gameboard

  // Abstract methods
  def assignTurnOrder(players: Seq[PlayerWithActor]): Seq[PlayerWithActor]
  def initializeGameState(state: GameState, callback: Callback): Unit
  def handlePacket(packet: InGamePacket, state: GameState, callback: Callback): Unit
  def playerDisconnect(player: PlayerWithActor, state: GameState, callback: Callback): Unit
}
