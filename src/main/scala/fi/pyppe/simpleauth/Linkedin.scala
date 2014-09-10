package fi.pyppe.simpleauth

import java.util.UUID

import play.api.Application
import play.api.libs.ws.WS
import play.api.mvc.{Result, Request}

import scala.concurrent.{Future, ExecutionContext}

object Linkedin extends Auth {

  private val Provider = "linkedin"

  private val AuthorizeUrl = "https://www.linkedin.com/uas/oauth2/authorization"
  private val AccessTokenUrl = "https://www.linkedin.com/uas/oauth2/accessToken"
  private val UserUrl = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,email-address,picture-url)"

  def initialize()(implicit app: Application, ec: ExecutionContext, r: Request[_]): Future[Result] = {
    val ProviderSettings(clientId, _, scope) = settings(Provider)
    Future.successful(redirect(AuthorizeUrl,
      "response_type" -> "code",
      "client_id" -> clientId,
      "redirect_uri" -> redirectUri(Provider),
      "scope" -> scope,
      "state" -> UUID.randomUUID.toString
    ))
  }

  def callback(handle: UserResponse => Result)
              (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    val ProviderSettings(clientId, clientSecret, scope) = settings(Provider)
    val code = req.getQueryString("code").get
    WS.url(AccessTokenUrl).post(Map(
      "grant_type" -> "authorization_code",
      "code" -> code,
      "redirect_uri" -> redirectUri(Provider),
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).mapValues(Seq(_))).flatMap { response =>
      val js = response.json
      val accessToken = (js \ "access_token").as[String]
      WS.url(UserUrl).withQueryString("format" -> "json").withHeaders(
        "Authorization" -> s"Bearer $accessToken"
      ).get.map(_.json).map { js =>
        val id = (js \ "id").as[String]
        val name = Seq((js \ "firstName").asOpt[String], (js \ "lastName").asOpt[String]).flatten.mkString(" ")
        val email = (js \ "emailAddress").asOpt[String]
        val image = (js \ "pictureUrl").asOpt[String]
        val user = User(Identity(id, Provider), name, email, image)
        handle(UserResponse(user, js))
      }
    }
  }

}
