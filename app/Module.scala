import actors.GameSupervisor
import com.google.inject.{AbstractModule, Inject}
import common.Resources
import gameplay.GameMode
import models.Color
import play.api.{Configuration, Environment}
import play.api.libs.concurrent.AkkaGuiceSupport

class Module @Inject()(environment: Environment, configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {
  override def configure() {
    // Create root actor and bind it for DI
    bindActor[GameSupervisor]("lobby-supervisor")

    // Load configuration values to the Resources object
    Option(configuration).foreach { config =>
      Resources.Origins = config.get[Seq[String]](Resources.ConfigKeys.OriginsConfig)
      Resources.Colors = config.get[Seq[String]](Resources.ConfigKeys.Colors).map(Color)
      Resources.PlayerIdCookie = config.get[String](Resources.ConfigKeys.PlayerIdCookie)
      Resources.BaseUrl = config.get[String](Resources.ConfigKeys.BaseUrl)
      Resources.GameIdChars = config.get[String](Resources.ConfigKeys.GameIdChars).toLowerCase.toList
      Resources.NameRegex = config.get[String](Resources.ConfigKeys.NameRegex)
      Resources.GameMode = environment.classLoader.loadClass(
          config.get[String](Resources.ConfigKeys.GameMode))
            .asSubclass(classOf[GameMode])
            .newInstance()
      Resources.IncomingPacketBufferSize = config.get[Int](Resources.ConfigKeys.IncomingPacketBufferSize)
      Resources.GameIdLength = config.get[Int](Resources.ConfigKeys.GameIdLength)
      Resources.PlayerIdLength = config.get[Int](Resources.ConfigKeys.PlayerIdLength)
      Resources.MinNameLength = config.get[Int](Resources.ConfigKeys.MinNameLength)
      Resources.MaxNameLength = config.get[Int](Resources.ConfigKeys.MaxNameLength)
      Resources.MinimumPlayers = config.get[Int](Resources.ConfigKeys.MinimumPlayers)
      Resources.MaximumPlayers = config.get[Int](Resources.ConfigKeys.MaximumPlayers)
      val initialArmiesSubConfig: Configuration = configuration.getOptional[Configuration](Resources.ConfigKeys.InitialArmies)
        .getOrElse(Configuration.empty)
      Resources.InitialArmies = initialArmiesSubConfig
        .subKeys
        .map(s => Integer.parseInt(s) -> initialArmiesSubConfig.get[Int](s))
        .toMap
    }
  }
}
