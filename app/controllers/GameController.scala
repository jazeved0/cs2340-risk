package controllers

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}

class GameController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
  // GET /game
  def load: Action[AnyContent] = Action {
    // send game page to the client
    Ok(views.html.game())
  }

  // GET /game/data
  def getData: Action[AnyContent] = Action {
    // TODO implement
    Ok("not implemented")
  }
}
