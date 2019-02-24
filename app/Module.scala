import actors.LobbySupervisor
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    // Create root actor and bind it for DI
    bindActor[LobbySupervisor]("lobby-supervisor")
  }
}