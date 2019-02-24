package actors

import akka.actor.ActorRef
import models.Client

/**
  * Client actor that is supervised by a Lobby
  * @param client Client DTO
  * @param actor Actor reference
  */
case class ClientWithActor(client: Client, actor: ActorRef)