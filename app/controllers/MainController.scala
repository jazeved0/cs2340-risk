package controllers

import javax.inject.Inject
import models.{Lobby, Player, Resources}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import scala.collection.mutable

class MainController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
  val logger: Logger = Logger(this.getClass)
  val lobbies: mutable.HashMap[String, Lobby] = mutable.HashMap()

  // Host HTTP calls

  /** This is the entry point for the host; this loads the page
    * at which a new game is made
    */
  // GET /
  def index : Action[AnyContent] = Action { implicit request =>
    // send landing page to the client (host)
    Ok(views.html.index(Resources.Colors))
  }

  // POST /lobby/make
  def make(name: String, colorIndex: Int): Action[AnyContent] = Action {
    // handle errors in provided query params
    if (name.length > Player.MaxNameLength)
      BadRequest(s"Length of name ${name.length} too long (max: ${Player.MaxNameLength})")
    if (colorIndex >= Resources.Colors.size || colorIndex < 0)
      BadRequest(s"Color index $colorIndex out of bounds (max: ${Resources.Colors.size})")

    val newLobby = Lobby.make(name, Resources.Colors(colorIndex))
    lobbies.put(newLobby.id, newLobby)
    logger.debug(s"Lobby id=${newLobby.id} created")

    Redirect(s"/lobby/host/${newLobby.id}")
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