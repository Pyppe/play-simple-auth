package fi.pyppe.simpleauth

import play.api.Application
import play.api.libs.json.JsValue
import play.api.mvc.{Result, Request}
import play.api.mvc.Results.Redirect

import scala.concurrent.{ExecutionContext, Future}

case class Identity(userId: String, provider: String)
case class User(identity: Identity, name: String, email: Option[String], image: Option[String])
case class UserResponse(user: User, json: JsValue)

object Auth {

  def initialize(provider: String)
                (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    provider match {
      case "facebook" => Facebook.initialize()
    }
  }

  def callback(provider: String)(handle: UserResponse => Result)
              (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    provider match {
      case "facebook" => Facebook.callback(handle)
    }
  }

}

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

  def parseParams(body: String) =
    body.split("&").map { param =>
      param.split("=") match {
        case Array(key, value) => key -> value
      }
    }.toMap

}