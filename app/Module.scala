import actors.GameSupervisor
import com.google.inject.{AbstractModule, Inject}
import common.Resources.ConfigLoader
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.{Configuration, Environment}

/**
  * Root-module level singleton object for managing any application-level
  * tasks such as binding DI targets
  * @param environment The environment context, bound by DI
  * @param configuration The application's configuration instance, bound by DI
  */
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
