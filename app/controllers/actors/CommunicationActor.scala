package controllers.actors

import akka.actor._

object CommunicationActor {
  def props(out: ActorRef) = Props(new CommunicationActor(out))
}

class CommunicationActor(out: ActorRef) extends Actor {
  def receive: PartialFunction[Any, Unit] = {
    // TODO implement packet mapping
    case msg: String =>
      out ! s"message received: $msg"
  }

  override def postStop(): Unit = {
    // TODO implement
  }
}