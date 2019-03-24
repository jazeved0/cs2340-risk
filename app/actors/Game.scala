package actors

import java.io.FileInputStream

import actors.Game.{CanBeHosted, CanBeJoined}
import actors.GameSupervisor.{CanHost, CanJoin}
import akka.actor.{Actor, ActorRef, Cancellable, PoisonPill, Props}
import common.{Resources, UniqueIdProvider, UniqueValueManager, Util}
import controllers._
import game.mode.GameMode
import game.mode.GameMode._
import game.state.GameState
import models.{Player, PlayerSettings}
import play.api.libs.json.Json

import scala.collection.immutable.{HashSet, WrappedString}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.control.Exception._

object Game extends UniqueIdProvider[WrappedString] {
  // Methods for UniqueIdProvider
  override def idLength: Int = Resources.GameIdLength
  private val IdCharsSet: Set[Char] = HashSet() ++ Resources.GameIdChars
  override protected def generateId(len: Int): WrappedString =
    Util.randomString(len, Resources.GameIdChars)
  override protected def isIdChar(c: Char): Boolean = IdCharsSet.contains(c)

  // Actor factory methods
  /** Defines default actor factory props for a Game instance */
  def props: Props = Props[Game]
  /** Creates actor factory props given the settings and id */
  def apply(id: String, hostInfo: PlayerSettings): Props =
    Props({
      new Game(Resources.GameMode, id, hostInfo)
    })

  /** Forwarded message asking whether the game can be hosted, expecting a
    * GameSupervisor.CanHost in response */
  case class CanBeHosted()
  /** Forwarded message asking whether the game can be joined, expecting a
    * GameSupervisor.CanJoin in response */
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

  // *************
  // Mutable state
  // *************

  /** The PlayerSettings of the initial host (submitted through the form
    * on the landing page) */
  var initialHostSettings: Option[PlayerSettings] = Some(hostInfo)
  /** List of players who have joined the game */
  val players: mutable.LinkedHashMap[String, PlayerWithActor] =
    mutable.LinkedHashMap[String, PlayerWithActor]()
  /** List of players who have established a ws connection, but not in lobby) */
  val connected: mutable.LinkedHashMap[String, PlayerWithActor] =
    mutable.LinkedHashMap[String, PlayerWithActor]()
  /** String containing the serialized config file, or empty if parsing failed */
  val configData: String = {
    val configStream = new FileInputStream(Resources.PublicConfigPath)
    (allCatch opt {
      Json.parse(configStream).toString
    }).getOrElse("")
  }

  /** Optional object here stores the scheduler that checks ping times; needs to
    * be here so cancelling it is an option when all players have disconnected */
  var pingCheckingTask: Option[Cancellable] = None
  /** Maps PlayerWithActor to millis time of last ping */
  val currentResponseTimes: mutable.LinkedHashMap[String, Long] =
    mutable.LinkedHashMap[String, Long]()
  /** Always a member of players, can be empty if and only if the lobby is empty */
  var host: Option[PlayerWithActor] = None
  /** Current state of the game as an enum value (used for the state machine) */
  var state: StateMachine.Type = StateMachine.Lobby
  /** Current gamestate as a data object (only used when the state machine is in game) */
  var gameState: Option[GameState] = None
  /** Whether or not the initial host joined the game and cleared the initial
    * host settings */
  def hasInitialHostJoined: Boolean = initialHostSettings.isEmpty

  /**
    * Moves the state machine to in game and initialized the gamestate object
    * according to the current gameMode
    */
  def startGame(): Unit = {
    // Update GameLobbyState enum and delegate to GameMode
    this.state = StateMachine.InGame
    gameState = Some(gameMode.initializeGame(
      players.values.toList,
      Callback(broadcastCallback, sendCallback)))
  }

  /**
    * Receive incoming packets or incoming messages and turn them into internal state
    * changes and/or outgoing packets/reply messages
    * @return a partial function defining the behavior upon message reception
    */
  override def receive: Receive = {
    case _: CanBeHosted =>
      if (this.state == StateMachine.InGame) {
        sender() ! CanHost.Started
      } else if (hasInitialHostJoined) {
        sender() ! CanHost.Hosted
      } else {
        sender() ! CanHost.Yes
      }
    case _: CanBeJoined =>
      if (this.state == StateMachine.InGame) {
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

  /**
    * Handle incoming packets in either state
    * @param globalPacket The packet object
    */
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

  /**
    * Handle incoming packets in the lobby state (according to the state machine)
    * @param lobbyPacket The packet object
    */
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


  /**
    * Handle incoming packets during the InGame state (according to the state machine)
    * @param inGamePacket The packet object
    */
  def receiveInGame(inGamePacket: InGamePacket): Unit = {
    inGamePacket match {
      case p if gameState.isDefined =>
        gameMode.handlePacket(p, Callback(broadcastCallback, sendCallback))(gameState.get)
      case p =>
        badPacket(p)
    }
  }

  /**
    * Callback function to broadcast an OutPacket to the entire game of connected
    * players, optionally excluding a certain player (gets hoisted to a function
    * literal)
    * @param packet The packet object to broadcast
    * @param exclude An optional value of a player Id to exclude
    */
  def broadcastCallback(packet: OutPacket, exclude: Option[String] = None): Unit =
    notifyGame(packet, exclude
      .filter(id => players.isDefinedAt(id))
      .map(id => players(id).actor))

  /**
    * Callback function to send an OutPacket to a specific player by their Id
    * (gets hoisted to a function literal)
    * @param packet The packet object to send
    * @param target The player id of the client to send this packet to
    */
  def sendCallback(packet: OutPacket, target: String): Unit =
    players.get(id)
      .map(p => p.actor)
      .foreach(a => a ! packet)

  /**
    * Handles an incoming PlayerConnect message. If the host has already joined,
    * simply adds the player to the list of connected players and sends them:
    *
    * > the config data from the public config json file, a game update with all
    * players who have joined the lobby, and a ping packet (after a short delay)
    *
    * If the host hasn't joined (and this is the host), then it initializes the
    * lobby and its host in addition to initializing the ping timeout watchdog
    * @param playerId The player Id of the connecting player
    * @param actor The indirect actor reference used to later send them packets
    */
  def playerConnect(playerId: String, actor: ActorRef): Unit = {
    if (players.get(playerId).isEmpty && connected.get(playerId).isEmpty) {
      actor ! SendConfig(configData)

      if (!hasInitialHostJoined) {
        // Add the initial host to the list of players
        val player = PlayerWithActor(playerId, Player(initialHostSettings), actor)
        players += playerId -> player
        host = Some(player)
        add(initialHostSettings.get)
        initialHostSettings = None

        initializePingChecker()
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

  /**
    * Initializes the ping timeout watchdog object. Gets triggered every
    * <code>Resources.PingTimeoutCheckInterval</code>
    */
  def initializePingChecker(): Unit = {
    pingCheckingTask = Some[Cancellable](
      this.context.system.scheduler.schedule(
        initialDelay = Resources.PingTimeoutCheckDelay,
        interval = Resources.PingTimeoutCheckInterval) {
        currentResponseTimes.foreach(
          pair => {
            val playerId = pair._1
            val playerOption = players.get(playerId) orElse connected.get(playerId)
            playerOption.foreach { p =>
              if (Math.abs(pair._2 - System.currentTimeMillis()) >
                  Resources.PingTimeout.toMillis) {
                p.actor ! PoisonPill
                playerDisconnect(p.id)
                currentResponseTimes -= pair._1
              }
            }
          }
        )
      }
    )
  }

  /**
    * Validates a player requesting to join the game after putting in their settings,
    * and if letting them join, adds them to the list of players and notifies the game
    * of the new lobby state
    * @param playerId The player id of the player attempting to join
    * @param withSettings The PlayerSettings dto constructed from form data
    */
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

  /**
    * Handles a RequestStartGame packet. Validates its source as being the game
    * host and makes sure there are sufficient players in the game to start. If
    * both of these checks pass, notifies the game of a StartGame() packet and
    * calls the <code>startGame</code> function to finish state transition
    * @param playerId The source player id of this packet
    */
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

  /**
    * Handles the internal message spawned when a player disconnects. Stops
    * the ping checking task and removes them from the list of connected/joined.
    * If they were the host, appoints a new one if possible
    * @param playerId The player id that is being disconnected
    */
  def playerDisconnect(playerId: String): Unit = {
    currentResponseTimes -= playerId
    this.state match {
      case StateMachine.Lobby =>
        if (players.isDefinedAt(playerId)) {
          removePotentialHost(playerId)
        } else {
          // Player hadn't actually joined, silently remove them
          connected -= playerId
        }

      case StateMachine.InGame =>
        if (players.isDefinedAt(playerId)) {
          gameState.foreach(s => gameMode.playerDisconnect(
            players(playerId), Callback(broadcastCallback, sendCallback))(s))
          removePotentialHost(playerId)
        }
    }
  }

  /**
    * Removes a player, appointing a new one to be the host as necessary and if
    * possible
    * @param playerId The player id to remove
    */
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
    * AND players
    * @param packet The packet object to send
    * @param exclude Optional player ActorRef to exclude sending the
    *                message to (used when accepting RequestPlayerJoins)
    */
  def notifyGame(packet: OutPacket, exclude: Option[ActorRef] = None): Unit = {
    (players.valuesIterator ++ connected.valuesIterator)
      .filter(exclude.isEmpty || _.actor != exclude.get)
      .foreach(_.actor ! packet)
  }

  /**
    * Constructs a Game Update packet including information about those
    * who have joined as well as the lobby's host (if it has one)
    * @return
    */
  def constructGameUpdate: OutPacket = {
    val playerList = players.valuesIterator
      .map(_.player.settings)
      .filter(_.isDefined)
      .map(_.get).toList
    GameLobbyUpdate(playerList,
      host.fold(-1)(_.player.settings.fold(-1)(playerList.indexOf(_))))
  }

  /**
    * Constructs a packet in response to one sent in an invalid state according
    * to the state machine
    * @param p The original packet
    */
  def packetInvalidState(p: InPacket): Unit = {
    connected.get(p.playerId).orElse(players.get(p.playerId)).foreach(
      actor => actor.actor ! BadPacket(s"Bad/unknown InPacket received: $p")
    )
  }

  /**
    * Constructs a packet in response to an otherwise bad packet(malformed or
    * otherwise invalid)
    * @param p The original packet
    */
  def badPacket(p: InPacket): Unit = {
    connected.get(p.playerId).orElse(players.get(p.playerId)).foreach(
      actor => actor.actor ! BadPacket(s"Bad/unknown InPacket received: $p")
    )
  }

  /**
    * Directly removes a player from the list of connected players as well as
    * releases their settings from the Unique Value Manager
    * @param id The player Id to remove
    */
  def removePlayer(id: String): Unit = {
    players.get(id).foreach(_.player.settings.foreach(remove))
    players -= id
  }

  /**
    * Possible values for the state machine defining the current state
    * (InGame/Lobby of the game actor)
    */
  object StateMachine extends Enumeration {
    type Type = Value
    val Lobby, InGame = Value
  }
}
