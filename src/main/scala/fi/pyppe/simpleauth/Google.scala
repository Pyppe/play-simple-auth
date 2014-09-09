package fi.pyppe.simpleauth

import play.api.Application
import play.api.libs.ws.WS
import play.api.mvc.{Result, Request}

import scala.concurrent.{ExecutionContext, Future}

object Google extends Auth {

  private val Provider = "google"

  def initialize()(implicit app: Application, ec: ExecutionContext, r: Request[_]): Future[Result] = {
    val ProviderSettings(clientId, _, scope) = settings(Provider)
    Future.successful(redirect("https://accounts.google.com/o/oauth2/auth",
      "response_type" -> "code",
      "client_id" -> clientId,
      "redirect_uri" -> redirectUri(Provider),
      "scope" -> scope
    ))
  }

  def callback(handle: UserResponse => Result)
              (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    val ProviderSettings(clientId, clientSecret, scope) = settings(Provider)
    WS.url("https://accounts.google.com/o/oauth2/token").post(Map(
      "client_id" -> clientId,
      "redirect_uri" -> redirectUri(Provider),
      "client_secret" -> clientSecret,
      "code" -> req.getQueryString("code").get,
      "grant_type" -> "authorization_code"
    ).mapValues(Seq(_))).flatMap { response =>
      val json = response.json
      val accessToken = (json \ "access_token").as[String]
      val userResponseFuture =
        WS.url("https://www.googleapis.com/plus/v1/people/me/openIdConnect").
          withQueryString("access_token" -> accessToken).get.map(_.json).map { js =>
          val id = (js \ "sub").as[String]
          val email = (js \ "email").asOpt[String]
          val picture = (js \ "picture").asOpt[String]
          val user = User(Identity(id, Provider), (js \ "name").as[String], email, picture)
          UserResponse(user, js)
        }
      userResponseFuture.map(handle)
    }
  }


}
