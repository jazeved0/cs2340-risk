package controllers

import javax.inject.Inject
import models.{Color, Resources, UserData}
import play.api.data.Form
import play.api.mvc._

class MainController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {

  // Host HTTP calls

  private val logger = play.api.Logger(this.getClass)
  private val makeURL = routes.MainController.make()

  /** This is the entry point for the host; this loads the page
    * at which a new game is made
    */
  // GET /
  def index : Action[AnyContent] = Action {
    // send landing page to the client (host)
    // colors from https://flatuicolors.com/palette/defo
    implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.index(Resources.userForm, Resources.COLORS, makeURL))
  }

  // POST /lobby/make
  def make(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    // makes the lobby with the following fields in the request:
    // name (String, hostname) and color (unsigned int, host color)
    // sends redirect to /lobby/host and generates main ID
    // TODO implement

    val errorFunction = { formWithErrors: Form[UserData] =>
      logger.debug("CAME INTO errorFunction")
      // this is the bad case, where the form had validation errors.
      // show the user the form again, with the errors highlighted.
      BadRequest("lmao don't be bad 4head")
    }

    var returnString = "pewds did an oopsie"

    val successFunction = { data: UserData =>
      logger.debug("CAME INTO successFunction")
      // this is the SUCCESS case, where the form was successfully parsed as a BlogPost
      val formData = UserData (
        data.name,
        data.colorIndex
      )
      returnString = formData.toString
    }

    val formValidationResult: Form[UserData] = Resources.userForm.bindFromRequest

    formValidationResult.fold(
      errorFunction,   // sad case
      successFunction  // happy case
    )

    Ok(returnString)
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
