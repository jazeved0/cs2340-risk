package controllers

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}

class LobbyController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
  // GET /
  def index: Action[AnyContent] = Action {
    // send landing page to the client
    Ok(views.html.index())
  }

  // GET /lobby
  def redirectIndex: Action[AnyContent] = Action {
    // redirect to landing page
    Redirect("/")
  }

  // POST /lobby/make
  def make: Action[AnyContent] = Action { implicit request =>
    // makes the lobby with the following fields in the request:
    // name (String, hostname) and color (unsigned int, host color)
    // sends redirect to /lobby/host and generates lobby ID
    // TODO implement
    Ok("not implemented")
  }

  // GET /lobby/host/:id
  def host(id: String): Action[AnyContent] = Action { implicit request =>
    // send lobby host page to the client
    // (address should get rewritten to normal lobby url on the
    // front end immediately upon load)
    // TODO implement
    Ok(views.html.lobby())
  }

  // GET /lobby/getId
  def getId: Action[AnyContent] = Action { implicit request =>
    // sends the player ID of the host to them
    // TODO implement
    Ok("not implemented")
  }

  // GET /lobby/:id
  def lobby(id: String): Action[AnyContent] = Action { implicit request =>
    // send lobby page to the client
    // TODO implement
    Ok(views.html.lobby())
  }

  // POST /lobby/ping?pid=____
  def ping(playerId: String): Action[AnyContent] = Action { implicit request =>
    // TODO implement
    Ok("not implemented")
  }

  // POST /lobby/join?name=___&color=___
  def join(name: String, color: Int): Action[AnyContent] = Action { implicit request =>
    // TODO implement
    Ok("not implemented")
  }

  // POST /lobby/start?pid=____
  def start(playerId: String): Action[AnyContent] = Action { implicit request =>
    // TODO implement
    Ok("not implemented")
  }
}
