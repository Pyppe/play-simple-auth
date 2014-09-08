package controllers

import fi.pyppe.simpleauth.{UserResponse, Auth}
import play.api._
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  def authenticate(provider: String) = Action.async { implicit req =>
    Auth.initialize(provider)
  }

  def authenticateCallback(provider: String) = Action.async { implicit req =>
    Auth.callback(provider) { case UserResponse(user, userJson) =>
      /*
       HERE WE WOULD TYPICALLY SAVE THE USER DETAILS TO A DATABASE
       */
      val session = Session(Seq(
        Some("user.identity" -> user.identity.toString),
        Some("user.name" -> user.name),
        user.email.map(e => "user.email" -> e),
        user.image.map(i => "user.image" -> i)
      ).flatten.toMap)
      Redirect("/").
        withSession(session).
        flashing("userJson" -> userJson.toString)
    }
  }

}