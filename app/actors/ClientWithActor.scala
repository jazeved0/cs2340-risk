package actors

import akka.actor.ActorRef
import models.Client

case class ClientWithActor(client: Client, actor: ActorRef)