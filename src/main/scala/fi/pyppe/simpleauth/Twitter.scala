package fi.pyppe.simpleauth

import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Base64
import play.api.Application
import play.api.libs.ws.{WS, WSResponse}
import play.api.mvc.{Result, Request}

import scala.concurrent.{Future, ExecutionContext}

object Twitter extends Auth {

  private val Provider = "twitter"

  case class Keys(consumerKey: String, consumerSecret: String)

  def initialize()(implicit app: Application, ec: ExecutionContext, r: Request[_]): Future[Result] = {
    signedRequest("https://api.twitter.com/oauth/request_token", "POST", None,
                  "oauth_callback" -> redirectUri(Provider)).map { response =>
      val token = parseParams(response.body)("oauth_token")
      redirect("https://api.twitter.com/oauth/authenticate", "oauth_token" -> token)
    }
  }

  def callback(handle: UserResponse => Result)
              (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result] = {
    val token = req.getQueryString("oauth_token").get
    val verifier = req.getQueryString("oauth_verifier").get

    signedRequest("https://api.twitter.com/oauth/access_token", "POST", Some(token),
                  "oauth_token" -> token,
                  "oauth_verifier" -> verifier).flatMap { response =>
      val (token, tokenSecret) = {
        val p = parseParams(response.body)
        p("oauth_token") -> p("oauth_token_secret")
      }
      signedRequest(
        "https://api.twitter.com/1.1/account/verify_credentials.json", "GET", Some(tokenSecret),
        "oauth_token" -> token,
        "oauth_token_secret" -> tokenSecret).map(_.json).map { js =>

        val id = (js \ "id_str").as[String]
        val name = (js \ "name").as[String]
        val image = (js \ "profile_image_url_https").asOpt[String]
        val user = User(Identity(id, Provider), name, None, image)
        handle(UserResponse(user, js))
      }
    }
  }

  private def signedRequest(url: String, method: String, token: Option[String],
                            extraParams: (String, String)*)
                           (implicit app: Application): Future[WSResponse] = {
    val (consumerKey, consumerSecret) = consumerKeys()
    val params = twitterParams(consumerKey, extraParams: _*).sortBy(_._1)
    val signature = {
      val paramStr = params.map { case(key, value) =>
        percentEncode(key) + "=" + percentEncode(value)
      }.mkString("&")
      val signatureBase = s"${method.toUpperCase}&${percentEncode(url)}&${percentEncode(paramStr)}"
      val signingKey = percentEncode(consumerSecret) + "&" + token.map(percentEncode).getOrElse("")
      hmacSha1Base64(signatureBase, signingKey)
    }
    val requestParams = (params :+ ("oauth_signature" -> signature))
    if (method.toUpperCase == "POST")
      WS.url(url).post(requestParams.toMap.mapValues(Seq(_)))
    else
      WS.url(url).withQueryString(requestParams: _*).get
  }

  private def consumerKeys()(implicit app: Application): (String, String) = {
    val c = app.configuration.getConfig(s"simple-auth.twitter").get
    c.getString("consumerKey").get -> c.getString("consumerSecret").get
  }

  private def twitterParams(consumerKey: String, params: (String, String)*) =
    Seq(
      "oauth_consumer_key" -> consumerKey,
      "oauth_nonce" -> UUID.randomUUID.toString.replace("-",""),
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp" -> (System.currentTimeMillis / 1000).toString,
      "oauth_version" -> "1.0"
    ) ++ params

  private def hmacSha1Base64(value: String, key: String) = {
    val keySpec = new SecretKeySpec(key.getBytes, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(keySpec)
    val result = mac.doFinal(value.getBytes)
    Base64.encodeBase64String((result))
  }

}
