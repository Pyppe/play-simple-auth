package fi.pyppe.simpleauth

import play.api.libs.ws.WS
import play.api.mvc.{Result, Request}
import play.api.{Configuration, Application}

import scala.concurrent.{ExecutionContext, Future}

object Facebook extends Auth {

  def initialize()(implicit app: Application, ec: ExecutionContext, r: Request[_]): Future[Result] = {
    val ProviderSettings(clientId, _, scope) = settings("facebook")
    Future.successful(redirect("https://www.facebook.com/dialog/oauth",
      "response_type" -> "code",
      "client_id" -> clientId,
      "redirect_uri" -> redirectUri("facebook"),
      "scope" -> scope
    ))
  }

  def callback(handle: UserResponse => Result)
              (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    val ProviderSettings(clientId, clientSecret, scope) = settings("facebook")
    WS.url("https://graph.facebook.com/oauth/access_token").withQueryString(
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "redirect_uri" -> redirectUri("facebook"),
      "code" -> req.getQueryString("code").get
    ).get.flatMap { response =>
      val accessToken = parseParams(response.body)("access_token")
      val userFuture = WS.url("https://graph.facebook.com/me").withQueryString(
        "access_token" -> accessToken
      ).get.map(_.json).map { js =>
        val id = (js \ "id").as[String]
        val email = (js \ "email").asOpt[String]
        val user = User(Identity(id, "facebook"), (js \ "name").as[String], email , None)
        UserResponse(user, js)
      }
      userFuture.map(handle)
    }
  }

}
