package fi.pyppe.simpleauth

import java.util.UUID

import fi.pyppe.simpleauth.Facebook._
import play.api.Application
import play.api.libs.ws.WS
import play.api.mvc.{Result, Request}

import scala.concurrent.{Future, ExecutionContext}

object Github extends Auth {

  private val AuthorizeUrl = "https://github.com/login/oauth/authorize"
  private val AccessTokenUrl = "https://github.com/login/oauth/access_token"
  private val UserApiUrl = "https://api.github.com/user"
  private val Provider = "github"

  def initialize()(implicit app: Application, ec: ExecutionContext, r: Request[_]): Future[Result] = {
    val ProviderSettings(clientId, _, scope) = settings(Provider)
    Future.successful(redirect(AuthorizeUrl,
      "client_id" -> clientId,
      "redirect_uri" -> redirectUri(Provider),
      "scope" -> scope,
      "state" -> UUID.randomUUID.toString
    ))
  }

  def callback(handle: UserResponse => Result)
              (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    val code = req.getQueryString("code").get
    val ProviderSettings(clientId, clientSecret, scope) = settings(Provider)
    WS.url(AccessTokenUrl).post(Map(
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "code" -> code
    ).mapValues(Seq(_))).flatMap { response =>
      val accessToken = parseParams(response.body)("access_token")
      WS.url(UserApiUrl).withQueryString("access_token" -> accessToken).
        get.map(_.json).map { js =>
          println(js)
          val id = (js \ "id").as[Long].toString
          val name = (js \ "name").as[String]
          val email = (js \ "email").asOpt[String]
          val image = (js \ "avatar_url").asOpt[String]
          val user = User(Identity(id, Provider), name, email, image)
          handle(UserResponse(user, js))
        }
    }
  }

}
