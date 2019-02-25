import actors.LobbySupervisor
import com.google.inject.{AbstractModule, Inject}
import common.Resources
import models.Color
import play.api.{Configuration, Environment}
import play.api.libs.concurrent.AkkaGuiceSupport

class Module @Inject()(unused: Environment, configuration: Configuration) extends AbstractModule with AkkaGuiceSupport {
  override def configure() {
    // Create root actor and bind it for DI
    bindActor[LobbySupervisor]("lobby-supervisor")

    // Load configuration values to the Resources object
    Option(configuration).foreach { config =>
      Resources.Origins = config.get[Seq[String]](Resources.ConfigKeys.OriginsConfig)
      Resources.Colors = config.get[Seq[String]](Resources.ConfigKeys.Colors).map(Color(_))
      Resources.ClientIdCookie = config.get[String](Resources.ConfigKeys.ClientIdCookie)
      Resources.BaseUrl = config.get[String](Resources.ConfigKeys.BaseUrl)
      Resources.LobbyIdChars = config.get[String](Resources.ConfigKeys.LobbyIdChars).toLowerCase.toList
      Resources.NameRegex = config.get[String](Resources.ConfigKeys.NameRegex)

      Resources.LobbyIdLength = config.get[Int](Resources.ConfigKeys.LobbyIdLength)
      Resources.ClientIdLength = config.get[Int](Resources.ConfigKeys.ClientIdLength)
      Resources.MinNameLength = config.get[Int](Resources.ConfigKeys.MinNameLength)
      Resources.MaxNameLength = config.get[Int](Resources.ConfigKeys.MaxNameLength)
      Resources.MinimumPlayers = config.get[Int](Resources.ConfigKeys.MinimumPlayers)
      Resources.MaximumPlayers = config.get[Int](Resources.ConfigKeys.MaximumPlayers)
    }
  }
}
