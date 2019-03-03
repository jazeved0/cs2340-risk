package actors

import akka.actor.ActorRef
import models.Player

/**
  * Player actor that is supervised by a Game
  *
  * @param player Player DTO
  * @param actor  Actor reference
  */
case class PlayerWithActor(player: Player, actor: ActorRef)
