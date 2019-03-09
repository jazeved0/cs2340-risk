import java.util

import actors.GameSupervisor
import com.google.inject.{AbstractModule, Inject}
import com.typesafe.config.{Config, ConfigObject}
import common.Resources
import game.mode.GameMode
import game.{Connection, Gameboard, Territory}
import models.Color
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment}

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

class Module @Inject()(environment: Environment, configuration: Configuration)
  extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    // Create root actor and bind it for DI
    bindActor[GameSupervisor]("game-supervisor")

    // Load configuration values to the Resources object
    Option(configuration).foreach { config =>
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
      Resources.SpaEntryPoint = config.get[String](Resources.ConfigKeys.SpaEntryPoint)

      Resources.Colors = config.get[Seq[String]](Resources.ConfigKeys.Colors).map(Color)
      Resources.GameIdChars = config.get[String](Resources.ConfigKeys.GameIdChars).toLowerCase.toList
      Resources.NameRegex = config.get[String](Resources.ConfigKeys.NameRegex)
      Resources.GameIdLength = config.get[Int](Resources.ConfigKeys.GameIdLength)
      Resources.PlayerIdLength = config.get[Int](Resources.ConfigKeys.PlayerIdLength)
      Resources.MinNameLength = config.get[Int](Resources.ConfigKeys.MinNameLength)
      Resources.MaxNameLength = config.get[Int](Resources.ConfigKeys.MaxNameLength)
      Resources.MinimumPlayers = config.get[Int](Resources.ConfigKeys.MinimumPlayers)
      Resources.MaximumPlayers = config.get[Int](Resources.ConfigKeys.MaximumPlayers)

      Resources.GameMode = environment.classLoader.loadClass(
        config.get[String](Resources.ConfigKeys.GameMode))
        .asSubclass(classOf[GameMode])
        .getDeclaredConstructor().newInstance()
      val initialArmiesSubConfig: Configuration = configuration
        .getOptional[Configuration](Resources.ConfigKeys.SkirmishInitialArmies)
        .getOrElse(Configuration.empty)
      Resources.SkirmishInitialArmies = initialArmiesSubConfig
        .subKeys
        .map(s => Integer.parseInt(s) -> initialArmiesSubConfig.get[Int](s))
        .toMap
      Resources.SkirmishGameboard = loadGameboard(loadOrThrow(
        configuration.getOptional[Configuration](Resources.ConfigKeys.SkirmishGameboard),
        Resources.ConfigKeys.SkirmishGameboard))
    }
  }

  def loadGameboard(configuration: Configuration): Gameboard = {
    val nodeCount: Int = configuration.get[Int]("nodeCount")

    val waterConnections: Seq[Connection] = toList(configuration.underlying.getObjectList("waterConnections"))
      .map(parseConnection)
    val regions: Seq[Range] = toList(configuration.underlying.getObjectList("regions"))
      .map { configObject =>
        configObject.getInt("a") to configObject.getInt("b")
      }
    val nodeList: Seq[Config] = toList(configuration.underlying.getObjectList("nodes")).sortWith(
      (c1: Config, c2: Config) => c1.getInt("node") < c2.getInt("node"))
    val nodeData: Seq[String] = nodeList.map { configObject =>
      configObject.getString("data")
    }
    val nodeIconData: Seq[String] = nodeList.map { configObject =>
      configObject.getString("iconData")
    }
    val centers: Seq[(Float, Float)] = nodeList.map { configObject =>
      (configObject.getDouble("center.x").toFloat, configObject.getDouble("center.y").toFloat)
    }
    val edgeList: Seq[(Int, Int)] = getAbTuples(configuration, "edges")
    val territories: Seq[Territory] = (0 until nodeCount).map { i =>
      Territory(edgeList
        .filter(t => t._1 == i || t._2 == i)
        .map(t => if (i == t._1) t._2 else t._1)
        .toSet)
    }
    val size: (Int, Int) = (configuration.get[Int]("size.a"), configuration.get[Int]("size.b"))
    Gameboard(nodeCount, nodeData, nodeIconData, centers, regions, waterConnections, territories, size)
  }

  def parseConnection(configObject: Config): Connection = {
    val midpoints: Seq[(Float, Float)] =
      if (configObject.hasPath("midpoints")) {
        configObject.getAnyRefList("midpoints").asScala.toList.map {
          case l: util.ArrayList[_] =>
            (toFloatOrElse(l.get(0)), toFloatOrElse(l.get(1)))
          case _ => (0f, 0f)
        }
      } else {
        Nil
      }
    val bezier: Boolean =
      if (configObject.hasPath("bz")) {
        configObject.getBoolean("bz")
      }
      else {
        false
      }
    val tension: Float =
      if (configObject.hasPath("tension")) {
        configObject.getDouble("tension").toFloat
      } else {
        0
      }

    Connection(
      configObject.getInt("a"), configObject.getInt("b"),
      midpoints, bezier, tension)
  }

  def toFloatOrElse(in: Any, defaultVal: Float = 0): Float = {
    in match {
      case d: Double => d.toFloat
      case s: String => s.toFloat
      case f: Float => f
      case _ => defaultVal
    }
  }

  def getAbTuples(configuration: Configuration, key: String): Seq[(Int, Int)] = {
    toList(configuration.underlying.getObjectList(key)).map { configObject =>
      (configObject.getInt("a"), configObject.getInt("b"))
    }
  }

  def toList(jList: java.util.List[_ <: ConfigObject]): Seq[Config] = {
    val l = jList.asScala.toList
    l.map(item => {
      item.toConfig
    })
  }

  def loadOrThrow[A](oa: Option[A], key: String): A =
    oa.getOrElse(throw new RuntimeException(s"Key $key not found"))
}
