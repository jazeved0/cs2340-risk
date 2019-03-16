import actors.GameSupervisor
import com.google.inject.{AbstractModule, Inject}
import common.Resources.ConfigLoader
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment}

class Module @Inject()(environment: Environment, configuration: Configuration)
  extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    // Create root actor and bind it for DI
    bindActor[GameSupervisor]("game-supervisor")

    // Load configuration values to the Resources object
    Option(configuration).foreach { config =>
      ConfigLoader.load(config, environment)
    }
  }
}
