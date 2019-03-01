import actors.GameSupervisor
import com.google.inject.{AbstractModule, Inject}
import common.Resources
import game.mode.GameMode
import models.Color
import play.api.{Configuration, Environment}
import play.api.libs.concurrent.AkkaGuiceSupport

import scala.concurrent.duration.FiniteDuration

class Module @Inject()(environment: Environment, configuration: Configuration)
  extends AbstractModule with AkkaGuiceSupport {
  override def configure() {
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
      Resources.GameInfoDelay = config.get[FiniteDuration](Resources.ConfigKeys.GameInfoDelay)

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
      val initialArmiesSubConfig: Configuration = configuration.getOptional[Configuration](Resources.ConfigKeys.InitialArmies)
        .getOrElse(Configuration.empty)
      Resources.InitialArmies = initialArmiesSubConfig
        .subKeys
        .map(s => Integer.parseInt(s) -> initialArmiesSubConfig.get[Int](s))
        .toMap
    }
  }
}
