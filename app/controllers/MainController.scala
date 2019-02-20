package controllers

import javax.inject.Inject
import models._
import play.api.Logger
import play.api.data.Form
import play.api.mvc._

import scala.collection.mutable

class MainController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
  val logger: Logger = Logger(this.getClass)
  val lobbies: mutable.HashMap[String, Lobby] = mutable.HashMap()
  private val makeURL = routes.MainController.make()

  // Host HTTP calls

  /** This is the entry point for the host; this loads the page
    * at which a new game is made
    */
  // GET /
  def index : Action[AnyContent] = Action { implicit request =>
    // send landing page to the client (host)
    Ok(views.html.index(Resources.UserForm, Resources.Colors, makeURL))
  }

  // POST /lobby/make
  def make(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    val formValidationResult: Form[UserData] = Resources.UserForm.bindFromRequest
    formValidationResult.fold(
      userData => {
        logger.debug(s"Form submission for $userData failed")
        // this is the bad case, where the form had validation errors.
        // show the user the form again, with the errors highlighted.
        BadRequest("Form submission failed")
      },
      userData => {
        if (userData.name.length > Player.MaxNameLength)
          BadRequest(s"Length of name ${userData.name.length} too long (max: ${Player.MaxNameLength})")
        if (userData.colorIndex >= Resources.Colors.size || userData.colorIndex < 0)
          BadRequest(s"Color index ${userData.colorIndex} out of bounds (max: ${Resources.Colors.size})")

        val newLobby = Lobby.make(userData.name, Resources.Colors(userData.colorIndex))
        lobbies.put(newLobby.id, newLobby)
        logger.debug(s"Lobby id=${newLobby.id} created")
        Redirect(s"/lobby/host/${newLobby.id}")
      }
    )
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
