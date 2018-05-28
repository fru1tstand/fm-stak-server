package me.fru1t.stak.server.routing

import com.google.common.annotations.VisibleForTesting
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.UserPasswordCredential
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import javax.inject.Inject

/** Routes for user. */
fun Route.user() {
  route("/user") {
    authenticate {
      post("/login") {
        call.respondText("Test login", ContentType.Text.Html)
      }
    }
  }
}

/** Handles user routing; that is, authentication and user account access. */
class UserHandler @Inject constructor() {
  companion object {
    private const val REALM = "stak-user"
  }

  fun registerAuthentication(configuration: Authentication.Configuration) {
    configuration.basic {
      realm = REALM
      validate { userPasswordCredential -> validate(userPasswordCredential) }
    }
  }

  @VisibleForTesting
  internal fun validate(userPasswordCredential: UserPasswordCredential): UserIdPrincipal? {
    if (userPasswordCredential.name == "test") {
      return UserIdPrincipal("test account")
    }
    return null
  }
}
