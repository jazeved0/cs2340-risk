package controllers

import javax.inject.Inject
import models.{Color, Resources}
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}

class MainController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

  // Host HTTP calls

  /** This is the entry point for the host; this loads the page
    * at which a new game is made
    */
  // GET /
  def index : Action[AnyContent] = Action {
    // send landing page to the client (host)
    // colors from https://flatuicolors.com/palette/defo
    Ok(views.html.index(Resources.COLORS))
  }

  // POST /lobby/make
  def make(name: String, colorIndex: Int): Action[AnyContent] = Action { implicit request =>
    // makes the lobby with the following fields in the request:
    // name (String, hostname) and color (unsigned int, host color)
    // sends redirect to /lobby/host and generates main ID
    // TODO implement
    var color = Resources.COLORS(colorIndex)
    Ok("not implemented")
  }

  // Obtains the corresponding main page after a host has created
  // a main
  // GET /lobby/host/:id
  def host(id: String): Action[AnyContent] = Action { implicit request =>
    // send lobby host page to the client
    // (address should get rewritten to normal main url on the
    // front end immediately upon load)
    // TODO implement
    Ok(views.html.main())
  }

  //This is the entry points for *non-hosts*;it gives them
  //the page responsible for them setting their name & color
  //and then joining the existing game
  // GET /lobby/:id
  def lobby(id: String): Action[AnyContent] = Action { implicit request =>
    // send main page to the client
    // TODO implement
    Ok(views.html.main())
  }

  //ERROR HANDLING

  //Redirects user to host index page if they do /main by mistake
  def redirectIndex: Action[AnyContent] = Action {
    // redirect to landing page
    Redirect("/")
  }

}
