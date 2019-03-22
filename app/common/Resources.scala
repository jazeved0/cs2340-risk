package common

import java.util

import com.typesafe.config.{Config, ConfigObject}
import controllers.routes
import game.Gameboard.{Location, Node}
import game.mode.GameMode
import game.{Connection, Gameboard, Territory}
import models.{Color, PlayerSettings}
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.Status
import play.api.mvc.Call
import play.api.{Configuration, Environment}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

/**
  * General resources for the application loaded from the configuration file
  */
object Resources {
  val UserForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "ordinal" -> number
    )(PlayerSettings.apply)(PlayerSettings.unapply)
  )
  val MakeUrl: Call = routes.MainController.make()
  val NonHostSubmitURL: Call = routes.MainController.make()
  object StatusCodes extends Status

  // Consumed in Module
  object ConfigKeys {
    val OriginsConfig = "app.controllers.origins"
    val PlayerIdCookie = "app.controllers.playerIdCookie"
    val BaseUrl = "app.controllers.baseUrl"
    val IncomingPacketBufferSize = "app.controllers.incomingPacketBufferSize"
    val InitialPingDelay = "app.controllers.initialPingDelay"
    val PingDelay = "app.controllers.pingDelay"
    val PingTimeout = "app.controllers.pingTimeout"
    val PingTimeoutCheckDelay = "app.controllers.pingTimeoutCheckDelay"
    val PingTimeoutCheckInterval = "app.controllers.pingTimeoutCheckInterval"
    val PublicConfigPath = "app.controllers.publicConfigPath"
    val SpaFileRoot = "app.controllers.spaFileRoot"
    val SpaEntryPoint = "app.controllers.spaEntryPoint"
    val DocsEnabled = "app.controllers.docsEnabled"
    val DocsRoot = "app.controllers.docsRoot"

    val Colors = "app.settings.colors"
    val GameIdChars = "app.settings.gameIdChars"
    val NameRegex = "app.settings.nameRegex"
    val GameIdLength = "app.settings.gameIdLength"
    val PlayerIdLength = "app.settings.playerIdLength"
    val MinNameLength = "app.settings.minNameLength"
    val MaxNameLength = "app.settings.maxNameLength"

    val GameMode = "app.gameplay.gameMode"
    val MinimumPlayers = "app.gameplay.minPlayers"
    val MaximumPlayers = "app.gameplay.maxPlayers"
    val SkirmishInitialArmy = "app.gameplay.skirmish.initialArmy"
    var SkirmishGameboard = "app.gameplay.skirmish.gameboard"
    val SkirmishReinforcementDivisor = "app.gameplay.skirmish.reinforcementDivisor"
    val SkirmishReinforcementBase = "app.gameplay.skirmish.reinforcementBase"
  }

  // ********************
  // CONFIG LOADED VALUES
  // ********************

  var Origins: Seq[String] = _
  var PlayerIdCookie: String = _
  var BaseUrl: String = _
  var IncomingPacketBufferSize: Int = _
  var InitialPingDelay: FiniteDuration = _
  var PingDelay: FiniteDuration = _
  var PingTimeout: FiniteDuration = _
  var PingTimeoutCheckDelay: FiniteDuration = _
  var PingTimeoutCheckInterval: FiniteDuration = _
  var PublicConfigPath: String = _
  var SpaFileRoot: String = _
  var SpaEntryPoint: String = _
  var DocsEnabled: Boolean = _
  var DocsRoot: String = _

  var Colors: Seq[Color] = _
  var GameIdChars: Seq[Char] = _
  var NameRegex: String = _
  var GameIdLength: Int = _
  var PlayerIdLength: Int = _
  var MinNameLength: Int = _
  var MaxNameLength: Int = _

  var GameMode: GameMode = _
  var MinimumPlayers: Int = _
  var MaximumPlayers: Int = _
  var SkirmishInitialArmy: Int = _
  var SkirmishGameboard: Gameboard = _
  var SkirmishReinforcementDivisor: Int = _
  var SkirmishReinforcementBase: Int = _

  // *************
  // CONFIG LOADER
  // *************

  object ConfigLoader {
    def load(config: Configuration, environment: Environment): Unit = {
      Resources.Origins = config.get[Seq[String]](Resources.ConfigKeys.OriginsConfig)

      Resources.PlayerIdCookie = config.get[String](Resources.ConfigKeys.PlayerIdCookie)
      Resources.BaseUrl = config.get[String](Resources.ConfigKeys.BaseUrl)
      Resources.IncomingPacketBufferSize = config.get[Int](Resources.ConfigKeys.IncomingPacketBufferSize)
      Resources.InitialPingDelay = config.get[FiniteDuration](Resources.ConfigKeys.InitialPingDelay)
      Resources.PingDelay = config.get[FiniteDuration](Resources.ConfigKeys.PingDelay)
      Resources.PingTimeout = config.get[FiniteDuration](Resources.ConfigKeys.PingTimeout)
      Resources.PingTimeoutCheckDelay = config.get[FiniteDuration](Resources.ConfigKeys.PingTimeoutCheckDelay)
      Resources.PingTimeoutCheckInterval = config.get[FiniteDuration](Resources.ConfigKeys.PingTimeoutCheckInterval)
      Resources.PublicConfigPath = config.get[String](Resources.ConfigKeys.PublicConfigPath)
      Resources.SpaFileRoot = config.get[String](Resources.ConfigKeys.SpaFileRoot)
      Resources.SpaEntryPoint = config.get[String](Resources.ConfigKeys.SpaEntryPoint)
      Resources.DocsRoot = config.get[String](Resources.ConfigKeys.DocsRoot)
      Resources.DocsEnabled = config.get[Boolean](Resources.ConfigKeys.DocsEnabled)

      Resources.Colors = config.get[Seq[String]](Resources.ConfigKeys.Colors).map(Color)
      Resources.GameIdChars = config.get[String](Resources.ConfigKeys.GameIdChars).toLowerCase.toList
      Resources.NameRegex = config.get[String](Resources.ConfigKeys.NameRegex)
      Resources.GameIdLength = config.get[Int](Resources.ConfigKeys.GameIdLength)
      Resources.PlayerIdLength = config.get[Int](Resources.ConfigKeys.PlayerIdLength)
      Resources.MinNameLength = config.get[Int](Resources.ConfigKeys.MinNameLength)
      Resources.MaxNameLength = config.get[Int](Resources.ConfigKeys.MaxNameLength)
      Resources.MinimumPlayers = config.get[Int](Resources.ConfigKeys.MinimumPlayers)
      Resources.MaximumPlayers = config.get[Int](Resources.ConfigKeys.MaximumPlayers)

      Resources.SkirmishInitialArmy = config.get[Int](Resources.ConfigKeys.SkirmishInitialArmy)
      Resources.GameMode = environment.classLoader.loadClass(
        config.get[String](Resources.ConfigKeys.GameMode))
        .asSubclass(classOf[GameMode])
        .getDeclaredConstructor().newInstance()
      Resources.SkirmishGameboard = loadGameboard(loadOrThrow(
        config.getOptional[Configuration](Resources.ConfigKeys.SkirmishGameboard),
        Resources.ConfigKeys.SkirmishGameboard))
      Resources.SkirmishReinforcementDivisor = config.get[Int](Resources.ConfigKeys.SkirmishReinforcementDivisor)
      Resources.SkirmishReinforcementBase = config.get[Int](Resources.ConfigKeys.SkirmishReinforcementBase)
    }
  }

  def loadGameboard(configuration: Configuration): Gameboard = {
    implicit val config: Config = configuration.underlying
    val waterConnections: Seq[Connection] = configList("waterConnections").map(parseConnection)
    val regions: Seq[Range] = getAbTuples("regions").map { case (a, b) => a to b }
    val edgeList: Seq[(Int, Int)] = getAbTuples("edges")
    val nodes: Seq[Node] = configList("nodes")
      .sortWith((c1, c2) => c1.getInt("node") < c2.getInt("node"))
      .map(parseNode(_, edgeList))
    val size: Location = getLocation("size")
    Gameboard(nodes, regions, waterConnections, size)
  }

  def parseNode(config: Config, edges: Seq[(Int, Int)]): Node = {
    val i = config.getInt("node")
    val data = config.getString("data")
    val iconData = config.getString("iconData")
    val center = getLocation("center", ("x", "y"))(config)
    val dto = Territory(edges
      .filter(t => t._1 == i || t._2 == i)
      .map(t => if (i == t._1) t._2 else t._1)
      .toSet,
      get("castle", (c, k) => {
        Location.apply(toTuple2(c.getAnyRefList(k).asScala.toList.flatMap(toFloat)))
      })(config))
    Node(data, iconData, center, dto)
  }

  def parseConnection(config: Config): Connection = {
    val wrapper = Configuration(config)
    val midpoints: Seq[(Float, Float)] = getList("midpoints", l =>
      (toFloat(l.get(0)).getOrElse(0f), toFloat(l.get(1)).getOrElse(0f)))(config)
    val bezier: Boolean = wrapper.getOptional[Boolean]("bz").getOrElse(false)
    val tension: Double = wrapper.getOptional[Double]("tension").getOrElse(0)
    Connection(
      config.getInt("a"), config.getInt("b"),
      midpoints, bezier, tension.toFloat)
  }

  def toFloat(in: Any): Option[Float] =
    in match {
      case d: Double => Some(d.toFloat)
      case s: String => Some(s.toFloat)
      case f: Float => Some(f)
      case _ => None
    }

  def get[B](key: String, map: (Config, String) => B)(implicit config: Config): Option[B] =
    if (config.hasPath(key)) Some(map(config, key)) else None

  def getList[B](key: String, map: util.ArrayList[_] => B)(implicit config: Config): Seq[B] =
    get(key, (c, k) =>
      c.getAnyRefList(k).asScala.toList.flatMap {
        case l: util.ArrayList[_] => Some(map(l))
        case _ => None
      })(config).getOrElse(Nil)

  def getPair[B](subConfig: Config, subKeys: (String, String), map: (Config, String) => B)(implicit config: Config): (B, B) =
    toTuple2(List(subKeys._1, subKeys._2).map { subKey => map(subConfig, subKey) })

  def toTuple2[B](list: Seq[B]): (B, B) =
    loadOrThrow(if (list.lengthCompare(2) >= 0) Some((list.head, list(1))) else None, s"list $list")

  def getAbTuples(key: String)(implicit config: Config): Seq[(Int, Int)] =
    list(key, configObj => getPair(configObj.toConfig, ("a", "b"), (c: Config, k: String) => c.getInt(k))(config))

  def getLocation(key: String, subKeys: (String, String) = ("a", "b"))(implicit config: Config): Location =
    Location.apply(getPair(config.getConfig(key), subKeys, (c: Config, k: String) => c.getDouble(k).toFloat))

  def list[B](key: String, map: ConfigObject => B)(implicit config: Config): Seq[B] =
    config.getObjectList(key).asScala.map(map)

  def configList(key: String)(implicit config: Config): Seq[Config] =
    list(key, item => item.toConfig)

  def loadOrThrow[A](oa: Option[A], key: String): A =
    oa.getOrElse(throw new RuntimeException(s"Key $key not found"))
}
