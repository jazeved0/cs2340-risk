package actors

import akka.actor.ActorRef
import models.Player

/**
  * Player actor that is supervised by a Game
  *
  * @param id Player ID (server secret)
  * @param player Player DTO
  * @param actor  Actor reference
  */
case class PlayerWithActor(id: String, player: Player, actor: ActorRef)
