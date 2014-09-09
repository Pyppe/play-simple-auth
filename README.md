# play-simple-auth

Provides **simple** authentication via OAuth. Supported providers: *Facebook*, *Google*, *Github* and *Twitter*.

See demo application running in [http://play-simple-auth.pyppe.fi](http://play-simple-auth.pyppe.fi)
(source code in [Pyppe/play-simple-auth-example.git](https://github.com/Pyppe/play-simple-auth-example))


## Usage
#### 1. Add dependency in `build.sbt`
```scala
libraryDependencies ++= Seq(
  ws, // play-simple-auth uses play-ws as a provided dependecy
  "fi.pyppe" %% "play-simple-auth" % "1.0"
)
```
#### 2. Create two actions in a `Controller`
For example:
```scala
import fi.pyppe.simpleauth.{Auth, UserResponse}

def authenticate(provider: String) = Action.async { implicit req =>
  Auth.initialize(provider) // returns Future[Result]; concretely a redirect to given provider
}

def authenticateCallback(provider: String) = Action.async { implicit req =>
  Auth.callback(provider) { case UserResponse(user, userJson) =>
    // Do anything you want with the user. Typically save/update to DB, and set a session.
    // We will also provider the userJson JsValue for any specific needs.
    val internalUserId = DB.insertOrUpdate(user) // imaginary save
    Redirect("/").withSession("user.id" -> internalUserId, "user.name" -> user.name)
  }
}
```

FYI: The signatures for the above-mentioned functions, and the UserResponse model, are as follows:
```scala
def initialize(provider: String)
              (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result]

def callback(provider: String)(handle: UserResponse => Result)
            (implicit app: Application, ec: ExecutionContext, req: Request[_]): Future[Result]

case class Identity(userId: String, provider: String) // provider, such as "facebook", or "twitter"
case class User(identity: Identity, name: String, email: Option[String], image: Option[String])
case class UserResponse(user: User, json: JsValue)
```

#### 3. Define the above-mentioned actions in the `routes` file
For example:
```
GET     /authenticate/:provider            controllers.Application.authenticate(provider: String)
GET     /authenticate/:provider/callback   controllers.Application.authenticateCallback(provider: String)
```


Example configuration (defined in `application.conf`):
```
simple-auth {
  # Relative or absolute callback uri (same as you have defined in routes file)
  redirectUri = "/authenticate/:provider/callback"
  facebook {
    clientId = "${FB_CLIENT_ID}"
    clientSecret = "${FB_SECRET}"
    scope = "email"
  }
  github {
    clientId = "${GITHUB_CLIENT_ID}"
    clientSecret = "${GITHUB_SECRET}"
    scope = "user:email"
  }
  google {
    clientId = "${GOOGLE_CLIENT_ID}"
    clientSecret = "${GOOGLE_SECRET}"
    scope = "openid profile email"
  }
  twitter {
    consumerKey = "${TWITTER_CONSUMER_KEY}"
    consumerSecret = "${TWITTER_CONSUMER_SECRET}"
  }
}
```
