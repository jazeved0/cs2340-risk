package common

import java.util

import com.typesafe.config.{Config, ConfigObject}
import controllers.routes
import game.Gameboard
import game.mode.GameMode
import game.state.GameState
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.Status
import play.api.mvc.Call
import play.api.{Configuration, Environment}

import scala.collection.JavaConverters._
import scala.collection.immutable.Range.Inclusive
import scala.concurrent.duration.FiniteDuration

/**
  * General resources for the application loaded from the configuration file
  */
object Resources {
  /** User form object used for initial host settings collection on the index page */
  val UserForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "ordinal" -> number
    )(PlayerSettings.apply)(PlayerSettings.unapply)
  )
  /** Utility object providing common status codes */
  object StatusCodes extends Status

  /**
    * Contains configuration paths for each config-loaded value in the class
    */
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
    val InitialFormPostUrl = "app.controllers.initialFormPostUrl"
    val DocsIconPath = "app.controllers.docsIconPath"

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
  var InitialFormPostUrl: Call = _
  var DocsIconPath: String = _

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

  /**
    * Performs the task of loading all mutable value members of Resources from their
    * associated configuration values according to the paths from <code>ConfigKeys</code>
    */
  object ConfigLoader {
    /**
      * Loads the values (called from <code>Module</code>
      * @param config The application's configuration object, bound by DI
      * @param environment The environment context object, bound by DI
      * @throws ConfigLoadException if parsing fails
      */
    @throws[ConfigLoadException]
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
      Resources.InitialFormPostUrl = Call("POST", config.get[String](Resources.ConfigKeys.InitialFormPostUrl))
      Resources.DocsIconPath = config.get[String](Resources.ConfigKeys.DocsIconPath)

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
      Resources.SkirmishGameboard = loadGameboard(unwrapOrThrow(
        config.getOptional[Configuration](Resources.ConfigKeys.SkirmishGameboard),
        Resources.ConfigKeys.SkirmishGameboard))
      Resources.SkirmishReinforcementDivisor = config.get[Int](Resources.ConfigKeys.SkirmishReinforcementDivisor)
      Resources.SkirmishReinforcementBase = config.get[Int](Resources.ConfigKeys.SkirmishReinforcementBase)
    }
  }

  /**
    * Parses a Gameboard object from its sub-config
    * @param configuration The sub-config with all gameboard keys at root level
    * @return A deserialized Gameboard object
    * @throws ConfigLoadException if parsing fails
    */
  @throws[ConfigLoadException]
  def loadGameboard(configuration: Configuration): Gameboard = {
    implicit val config: Config = configuration.underlying
    val waterConnections: Seq[Connection] = configList("waterConnections").map(parseConnection)
    val regions: Seq[Inclusive] = getAbTuples("regions").map { case (a, b) => a to b }
    val edgeList: Seq[(Int, Int)] = getAbTuples("edges")
    val nodes: Seq[Node] = configList("nodes")
      .sortWith((c1, c2) => c1.getInt("node") < c2.getInt("node"))
      .map(parseNode(_, edgeList))
    val size: Location = getLocation("size")
    Gameboard(nodes, regions, waterConnections, size)
  }

  /**
    * Parses a single node object from its specific sub-config
    * @param config The sub-config with all node keys at root level
    * @param edges The list of node connections used to include in node metadata
    * @return A deserialized Node object
    * @throws ConfigLoadException if parsing fails
    */
  @throws[ConfigLoadException]
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

  /**
    * Parses a single connection object from its specific sub-config
    * @param config The sub-config with all connection keys at root level
    * @return A deserialized Connection object
    */
  def parseConnection(config: Config): Connection = {
    val wrapper = Configuration(config)
    val midpoints: Seq[(Float, Float)] = getFlatMap("midpoints", l =>
      (toFloat(l.get(0)).getOrElse(0f), toFloat(l.get(1)).getOrElse(0f)))(config)
    val bezier: Boolean = wrapper.getOptional[Boolean]("bz").getOrElse(false)
    val tension: Double = wrapper.getOptional[Double]("tension").getOrElse(0)
    Connection(
      config.getInt("a"), config.getInt("b"),
      midpoints, bezier, tension.toFloat)
  }

  /**
    * Attempts to convert the input of Any type to a Float
    * @param in The input object
    * @return Some(Float) if parsed, None otherwise
    */
  def toFloat(in: Any): Option[Float] =
    in match {
      case d: Double => Some(d.toFloat)
      case s: String => Some(s.toFloat)
      case f: Float => Some(f)
      case _ => None
    }

  /**
    * Safely attempts to get the value from a config according to a mapping function
    * which converts the config object to the found value. This is only called if
    * they given key is valid
    * @param key The String key for the value to load
    * @param map A function defining a way of extracting the value from the config (given the path)
    * @param config The config object to use
    * @tparam B The type of the value to load
    * @return Some(B) if loaded properly, None otherwise
    */
  def get[B](key: String, map: (Config, String) => B)(implicit config: Config): Option[B] =
    if (config.hasPath(key)) Some(map(config, key)) else None

  /**
    * Creates a scala List instance of the type from the config according to a mapping
    * function which converts an ArrayList to a single object of the type (flattens and maps)
    * @param key The String key for the value to load
    * @param map A function defining a way of extracting the value from a List of Any
    * @param config The config object to use
    * @tparam B The type of value to load and extract from the config object
    * @return A scala list of the flat mapped B values
    */
  def getFlatMap[B](key: String, map: util.ArrayList[_] => B)(implicit config: Config): Seq[B] =
    get(key, (c, k) =>
      c.getAnyRefList(k).asScala.toList.flatMap {
        case l: util.ArrayList[_] => Some(map(l))
        case _ => None
      })(config).getOrElse(Nil)

  /**
    * Creates a Tuple2 from the config according to a mapping function that converts
    * a config to one of the values (with the config instance)
    * @param subConfig The config object to use with each value at its root level
    * @param subKeys The sub-paths to look for the value of each member of the pair
    * @param map A function defining a way of extracting the value from the Config object
    * @param config The config object to use
    * @tparam B The type of the value to load and extract from the config object
    * @return A Tuple two of (B, B), or throws an exception of it fails
    * @throws ConfigLoadException if parsing fails
    */
  @throws[ConfigLoadException]
  def getPair[B](subConfig: Config, subKeys: (String, String), map: (Config, String) => B)(implicit config: Config): (B, B) =
    toTuple2(List(subKeys._1, subKeys._2).map { subKey => map(subConfig, subKey) })

  /**
    * Creates a Tuple2 from a Seq of minimum length 2
    * @param list The list two pull objects from
    * @tparam B The type of the member of the list & the resulting tuple
    * @return A tuple2 of the values, or throws an exception if it fails
    * @throws ConfigLoadException if parsing fails
    */
  @throws[ConfigLoadException]
  def toTuple2[B](list: Seq[B]): (B, B) =
    unwrapOrThrow(if (list.lengthCompare(2) >= 0) Some((list.head, list(1))) else None, s"list $list")

  /**
    * Loads a list of {a: val, b: val} tuples in the form of a Seq[(Int, Int)]
    * @param key The config key of the outer list
    * @param config The config object to use
    * @return A list of integer Tuple2's containing the values in (a,b) pairs
    * @throws ConfigLoadException if parsing fails
    */
  @throws[ConfigLoadException]
  def getAbTuples(key: String)(implicit config: Config): Seq[(Int, Int)] =
    list(key, configObj => getPair(configObj.toConfig, ("a", "b"), (c: Config, k: String) => c.getInt(k))(config))

  /**
    * Parses a single Location object from a config object, using keys a,b by default
    * @param key The config key of the object containing the sub-keys at root level
    * @param subKeys The sub-keys to look for the values of the tuple
    * @param config The config object to use
    * @return A single Location dto containing two floating point numbers
    * @throws ConfigLoadException if parsing fails
    */
  @throws[ConfigLoadException]
  def getLocation(key: String, subKeys: (String, String) = ("a", "b"))(implicit config: Config): Location =
    Location.apply(getPair(config.getConfig(key), subKeys, (c: Config, k: String) => c.getDouble(k).toFloat))

  /**
    * Parses a generic scala list from a config object according to a mapping function
    * which converts the config object to a single value.
    * @param key The config key of the list
    * @param map A function defining a way of extracting the targeted values from the config object, given the path
    * @param config Tne config object to use
    * @tparam B The type of value to load and extract from the config object
    * @return A sequence of the parsed values
    */
  def list[B](key: String, map: ConfigObject => B)(implicit config: Config): Seq[B] =
    config.getObjectList(key).asScala.map(map)

  /**
    * Parses a list of sub-config objects from a parent config object
    * @param key The config key of the list
    * @param config The config object to use
    * @return A sequence of Config instances
    */
  def configList(key: String)(implicit config: Config): Seq[Config] =
    list(key, item => item.toConfig)

  /**
    * Attempts to unwrap an Option object, returning the inner object if the Option
    * is not empty, and throws an exception otherwise
    * @param oa The Option object to attempt to unwrap
    * @param key Optional key object to include in exception message
    * @tparam A The type of object contained within the Option
    * @throws ConfigLoadException if not found
    * @return The loaded object as A
    */
  @throws[ConfigLoadException]
  def unwrapOrThrow[A](oa: Option[A], key: String): A =
    oa.getOrElse(throw ConfigLoadException(key))

  /**
    * Represents an unrecoverable error that occurred during configuration parsing
    * @param key The config path that caused the error
    */
  case class ConfigLoadException(key: String) extends RuntimeException(s"Key $key not found")
}
