play-simple-auth
================

Provides simple authentication via Facebook, Google and Twitter.

Example configuration (defined in `application.conf`):
```
simple-auth {
  # Relative or absolute
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
