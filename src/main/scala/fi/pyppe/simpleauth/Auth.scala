package fi.pyppe.simpleauth

import play.api.Application
import play.api.libs.json.JsValue
import play.api.mvc.{Result, Request}
import play.api.mvc.Results.Redirect

import scala.concurrent.{ExecutionContext, Future}

case class Identity(userId: String, provider: String)
case class User(identity: Identity, name: String, email: Option[String], image: Option[String])
case class UserResponse(user: User, json: JsValue)

class ConfigurationException(msg: String) extends RuntimeException(msg)

object Auth {

  def initialize(provider: String)
                (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    provider match {
      case "facebook" => Facebook.initialize()
      case "google"   => Google.initialize()
    }
  }

  def callback(provider: String)(handle: UserResponse => Result)
              (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    provider match {
      case "facebook" => Facebook.callback(handle)
      case "google"   => Google.callback(handle)
    }
  }

}

case class ProviderSettings(clientId: String, clientSecret: String, scope: String)

trait Auth {

  def encodeUrlParam(s: String) = java.net.URLEncoder.encode(s, "utf-8")
  def percentEncode(s: String) = encodeUrlParam(s).replace("+", "%20")

  def redirect(baseUrl: String, params: (String, String)*): Result = {
    val url = baseUrl + (params.toList match {
      case Nil => ""
      case params =>
        "?" + params.map { case (key, value) => key + "=" + encodeUrlParam(value) }.mkString("&")
    })
    Redirect(url)
  }

  def settings(provider: String)(implicit app: Application): ProviderSettings = {
    val c = app.configuration.getConfig(s"simple-auth.$provider").get
    ProviderSettings(c.getString("clientId").get, c.getString("clientSecret").get, c.getString("scope").get)
  }

  def redirectUri(provider: String)(implicit app: Application, r: Request[_]): String = {
    val key = "simple-auth.redirectUri"
    app.configuration.getString(key) match {
      case Some(redirectUri) =>
        val providerRedirectUri = redirectUri.replace(":provider", provider)
        if (redirectUri.startsWith("/")) {
          val protocol = if (r.secure) "https://" else "http://"
          s"$protocol${r.host}$providerRedirectUri"
        } else {
          providerRedirectUri
        }
      case None =>
        throw new ConfigurationException(s"$key not defined in application.conf")
    }
  }

  def parseParams(body: String): Map[String, String] = try {
    body.split("&").map { param =>
      param.split("=") match {
        case Array(key, value) => key -> value
      }
    }.toMap
  } catch {
    case t: Throwable =>
      throw new RuntimeException(s"Error parsing callback parameters from $body", t)
  }

}