package actors

import java.io.FileInputStream

import actors.Game.{CanBeHosted, CanBeJoined}
import actors.GameSupervisor.{CanHost, CanJoin}
import akka.actor.{Actor, ActorRef, Cancellable, PoisonPill, Props}
import common.{Resources, UniqueIdProvider, UniqueValueManager, Util}
import controllers._
import game.GameState
import game.mode.GameMode
import models.GameLobbyState.State
import models.{GameLobbyState, Player, PlayerSettings}
import play.api.Logger
import play.api.libs.json.Json

import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object Game extends UniqueIdProvider {
  // Methods for UniqueIdProvider
  override def idLength: Int = Resources.GameIdLength
  private val IdCharsSet: Set[Char] = HashSet() ++ Resources.GameIdChars
  override protected def generateId(len: Int): String =
    Util.randomString(len, Resources.GameIdChars)
  override protected def isIdChar(c: Char): Boolean = IdCharsSet.contains(c)

  // Actor factory methods
  def props: Props = Props[Game]
  def apply(id: String, hostInfo: PlayerSettings): Props =
    Props({
      new Game(Resources.GameMode, id, hostInfo)
    })

  case class CanBeHosted()
  case class CanBeJoined()
}

/**
  * Game actor that supervises a collection of connected players that
  * may or may not have actually joined
  *
  * @param gameMode The current game mode of the game lobby
  * @param id       The unique game Id
  * @param hostInfo The PlayerSettings of the host
  */
class Game(val gameMode: GameMode, val id: String, hostInfo: PlayerSettings)
  extends Actor with UniqueValueManager[PlayerSettings] {

  val logger: Logger = Logger(this.getClass)

  // *************
  // Mutable state
  // *************

  // The PlayerSettings of the initial host (submitted through the form
  // on the landing page)
  var initialHostSettings: Option[PlayerSettings] = Some(hostInfo)
  // List of players who have joined the game
  val players: mutable.LinkedHashMap[String, PlayerWithActor] =
    mutable.LinkedHashMap[String, PlayerWithActor]()
  // List of players who have established a ws connection, but not in lobby)
  val connected: mutable.LinkedHashMap[String, PlayerWithActor] =
    mutable.LinkedHashMap[String, PlayerWithActor]()

  val stream = new FileInputStream("conf/public.json")
  val config: String = try {
    Json.parse(stream).toString
  } finally {
    stream.close()
  }

  // Optional object here stores the scheduler that checks ping times; needs to
  // be here so cancelling it is an option when all players have disconnected
  // HashMap maps PlayerWithActor to millis time of last ping
  var pingCheckingTask: Option[Cancellable] = None
  val currentResponseTimes: mutable.LinkedHashMap[String, Long] =
    mutable.LinkedHashMap[String, Long]()
  // Always a member of players
  var host: Option[PlayerWithActor] = None
  // Current state of the game
  var state: State = GameLobbyState.Lobby
  var gameState: Option[GameState] = None

  def hasInitialHostJoined: Boolean = initialHostSettings.isEmpty
  def hasHost: Boolean = host.isEmpty

  def startGame(): Unit = {
    // Update GameLobbyState enum and delegate to GameMode
    this.state = GameLobbyState.InGame
    gameState = Some(gameMode.initializeGame(
      players.values.toList,
      broadcastCallback,
      sendCallback))
  }

  // Receive incoming packets and turn them into internal state
  // changes and/or outgoing packets (returns partial function)
  override def receive: Receive = {
    case _: CanBeHosted =>
      if (this.state == GameLobbyState.InGame) {
        sender() ! CanHost.Started
      } else if (hasInitialHostJoined) {
        sender() ! CanHost.Hosted
      } else {
        sender() ! CanHost.Yes
      }
    case _: CanBeJoined =>
      if (this.state == GameLobbyState.InGame) {
        sender() ! CanJoin.Started
      } else {
        sender() ! CanJoin.Yes
      }
    case p: GlobalPacket =>
      receiveGlobal(p)
    case p: LobbyPacket =>
      receiveLobby(p)
    case p: InGamePacket =>
      receiveInGame(p)
  }

  // Handle incoming packets in either state
  def receiveGlobal(globalPacket: GlobalPacket): Unit = {
    globalPacket match {
      case PlayerDisconnect(_, id: String) => playerDisconnect(id)
      case PingResponse(_, playerId: String) =>
        // Update the player's last ping to current time
        val playerOption = players.get(playerId) orElse connected.get(playerId)
        playerOption.foreach { player =>
          if (currentResponseTimes.get(playerId).isDefined) {
            currentResponseTimes += playerId -> System.currentTimeMillis()
          } else {
            currentResponseTimes -= playerId
            player.actor ! PoisonPill
            playerDisconnect(playerId)
          }
          this.context.system.scheduler.scheduleOnce(Resources.PingDelay) {
            player.actor ! PingPlayer()
          }
        }
      case p => badPacket(p)
    }
  }

  // Handle incoming packets in the lobby state
  def receiveLobby(lobbyPacket: LobbyPacket): Unit = {
    lobbyPacket match {
      case PlayerConnect(_, id: String, actor: ActorRef) =>
        playerConnect(id, actor)
      case RequestPlayerJoin(_, id: String, settings: PlayerSettings) =>
        requestPlayerJoin(id, settings)
      case RequestStartGame(_, id: String) =>
        requestStartGame(id)
      case p =>
        badPacket(p)
    }
  }

  // Handle incoming packets during the InGame state
  def receiveInGame(inGamePacket: InGamePacket): Unit = {
    inGamePacket match {
      case p if gameState.isDefined =>
        gameMode.handlePacket(p, gameState.get, broadcastCallback, sendCallback)
      case p =>
        badPacket(p)
    }
  }

  def broadcastCallback(packet: OutPacket, exclude: Option[String] = None): Unit =
    notifyGame(packet, exclude
      .filter(id => players.isDefinedAt(id))
      .map(id => players(id).actor))

  def sendCallback(packet: OutPacket, target: String): Unit =
    players.get(id)
      .map(p => p.actor)
      .foreach(a => a ! packet)

  def playerConnect(playerId: String, actor: ActorRef): Unit = {
    if (players.get(playerId).isEmpty && connected.get(playerId).isEmpty) {
      actor ! SendConfig(config)

      if (!hasInitialHostJoined) {
        // Add the initial host to the list of players
        val player = PlayerWithActor(playerId, Player(initialHostSettings), actor)
        players += playerId -> player
        host = Some(player)
        add(initialHostSettings.get)
        initialHostSettings = None

        pingCheckingTask = Some[Cancellable](
          this.context.system.scheduler.schedule(
            initialDelay = Resources.PingTimeoutCheckDelay,
            interval = Resources.PingTimeoutCheckInterval) {
            currentResponseTimes.foreach(
              pair => {
                val playerOption = players.get(playerId) orElse connected.get(playerId)
                playerOption.foreach { p =>
                  if (Math.abs(pair._2 - System.currentTimeMillis()) > Resources.PingTimeout.toMillis) {
                    p.actor ! PoisonPill
                    playerDisconnect(p.id)
                    currentResponseTimes -= pair._1
                  }
                }
              }
            )
          }
        )

        currentResponseTimes += playerId -> System.currentTimeMillis()

        notifyGame(constructGameUpdate)
      } else {
        // Send current lobby information
        connected += playerId -> PlayerWithActor(playerId, Player.apply, actor)
        currentResponseTimes += playerId -> System.currentTimeMillis()
        actor ! constructGameUpdate
      }

      // Send initial ping delay
      this.context.system.scheduler.scheduleOnce(Resources.InitialPingDelay) {
        actor ! PingPlayer()
      }
    }
  }

  def requestPlayerJoin(playerId: String, withSettings: PlayerSettings): Unit = {
    if (connected.isDefinedAt(playerId)) {
      if (!PlayerSettings.isValid(withSettings)) {
        // Reject with response
        connected(playerId).actor ! RequestReply(RequestResponse.Rejected,
          PlayerSettings.formatInvalid(withSettings))
      } else if (!isUnique(withSettings)) {
        // Reject with response
        connected(playerId).actor ! RequestReply(RequestResponse.Rejected,
          "Name and color must be unique: non-unique inputs " +
            s"{${nonUniqueElements(withSettings).mkString(", ")}}")
      } else if (players.size >= Resources.MaximumPlayers) {
        // Reject with response
        connected(playerId).actor ! RequestReply(RequestResponse.Rejected,
          "Lobby is full")
      } else {
        val player = connected(playerId)
        connected -= playerId
        val newPlayer = PlayerWithActor(playerId, Player(Some(withSettings)), player.actor)
        players += playerId -> newPlayer
        add(withSettings)
        if (host.isEmpty) host = Some(newPlayer)
        // Approve with response
        newPlayer.actor ! RequestReply(RequestResponse.Accepted)
        // Broadcast lobby update to all other players
        notifyGame(constructGameUpdate, Some(newPlayer.actor))
      }
    }
  }

  def requestStartGame(playerId: String): Unit = {
    if (host.exists(_.id == playerId)) {
      if (players.size >= Resources.MinimumPlayers) {
        // Request is coming from the host, start game
        connected.foreach(_._2.actor ! PoisonPill)
        currentResponseTimes.iterator.filter {
          case (s, _) => connected.keySet.contains(s)
        }.foreach(t => currentResponseTimes.remove(t._1))
        connected.empty

        notifyGame(StartGame())
        startGame()

      } else {
        // Reject with response
        connected.getOrElse(playerId, players(playerId)).actor !
          RequestReply(RequestResponse.Rejected, "Cannot start game: " +
            s"not enough players (min: ${Resources.MinimumPlayers})")
      }

    } else {
      // Reject with response
      connected.getOrElse(playerId, players(playerId)).actor !
        RequestReply(RequestResponse.Rejected, "Must be the host " +
          "of the game lobby to start it (invalid privileges)")
    }
  }

  def playerDisconnect(playerId: String): Unit = {
    currentResponseTimes -= playerId
    this.state match {
      case GameLobbyState.Lobby =>
        if (players.isDefinedAt(playerId)) {
          removePotentialHost(playerId)
        } else {
          // Player hadn't actually joined, silently remove them
          connected -= playerId
        }

      case GameLobbyState.InGame =>
        if (players.isDefinedAt(playerId)) {
          gameState.foreach(s => gameMode.playerDisconnect(
            players(playerId), s,
            broadcastCallback, sendCallback))
          removePotentialHost(playerId)
        }
    }
  }

  def removePotentialHost(playerId: String): Unit = {
    if (host.exists(_.id == playerId)) {
      // Host disconnecting
      removePlayer(playerId)
      // Promote the first-joined player to host if there is one
      host = if (players.isEmpty) None else Some(players.head._2)
      notifyGame(constructGameUpdate)
    } else {
      // Normal player disconnecting
      removePlayer(playerId)
      notifyGame(constructGameUpdate)
    }
  }

  /**
    * Sends a message to all players connected to the game (connected
    * AND players)
    *
    * @param exclude Optional player ActorRef to exclude sending the
    *                message to (used when accepting RequestPlayerJoins)
    */
  def notifyGame(packet: OutPacket, exclude: Option[ActorRef] = None): Unit = {
    (players.valuesIterator ++ connected.valuesIterator)
      .filter(exclude.isEmpty || _.actor != exclude.get)
      .foreach(_.actor ! packet)
  }

  def constructGameUpdate: OutPacket = {
    val playerList = players.valuesIterator
      .map(_.player.settings)
      .filter(_.isDefined)
      .map(_.get).toList
    GameLobbyUpdate(playerList,
      host.fold(-1)(_.player.settings.fold(-1)(playerList.indexOf(_))))
  }


  def packetInvalidState(p: InPacket): Unit = {
    connected.get(p.playerId).orElse(players.get(p.playerId)).foreach(
      actor => actor.actor ! BadPacket(s"Bad/unknown InPacket received: $p")
    )
  }

  def badPacket(p: InPacket): Unit = {
    connected.get(p.playerId).orElse(players.get(p.playerId)).foreach(
      actor => actor.actor ! BadPacket(s"Bad/unknown InPacket received: $p")
    )
  }

  def removePlayer(id: String): Unit = {
    players.get(id).foreach(_.player.settings.foreach(remove))
    players -= id
  }
}
