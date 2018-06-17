package me.fru1t.stak.server.routing

import com.google.common.annotations.VisibleForTesting
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.FormAuthChallenge
import io.ktor.auth.Principal
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.basic
import io.ktor.auth.form
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.post
import io.ktor.routing.route
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.models.UserPrincipal
import mu.KLogging
import javax.inject.Inject

/** Session routing. */
fun Route.session(sessionHandler: SessionHandler) {
  route("session") {
    authenticate(Constants.LOGIN_AUTH_NAME) {
      post { sessionHandler.login(call) }
    }
    authenticate(Constants.SESSION_AUTH_NAME) {
      delete { sessionHandler.logout(call) }
    }
  }
}

/** Handles session routing including logging in, logging out, and persistent session data. */
class SessionHandler @Inject constructor(private val sessionController: SessionController) {
  companion object : KLogging() {
    private const val REALM = "stak-user"

    private const val SESSION_AUTH_NAME = Constants.SESSION_AUTH_NAME
    @VisibleForTesting internal const val SESSION_USER_PARAM_NAME = "type"
    @VisibleForTesting internal const val SESSION_PASS_PARAM_NAME = "token"
    @VisibleForTesting internal const val SESSION_USER_PARAM_VALUE = "session-token"
  }

  fun registerAuthentication(configuration: Authentication.Configuration) {
    configuration.basic(Constants.LOGIN_AUTH_NAME) {
      realm = REALM
      validate { credentials -> sessionController.login(credentials).value }
    }
    configuration.form(SESSION_AUTH_NAME) {
      userParamName = SESSION_USER_PARAM_NAME
      passwordParamName = SESSION_PASS_PARAM_NAME
      challenge = FormAuthChallenge.Unauthorized
      validate { userPasswordCredential ->
        run<Principal?> {
          if (userPasswordCredential.name != SESSION_USER_PARAM_VALUE) {
            return@run null
          }

          val result = sessionController.getActiveSession(userPasswordCredential.password)
          if (result.status != SessionController.GetActiveSessionStatus.SUCCESS) {
            logger.debug {
              "Session validation failed for host ${request.origin.host}, gave " +
                  userPasswordCredential.password
            }
            return@run null
          }

          return@run result.value
        }
      }
    }
  }

  /**
   * Routing call for logging in using basic authentication. Returns the session token in plaintext
   * as the response body if the login attempt was successful. [registerAuthentication] handles the
   * validation of logging in where this method simply returns the session token as we've already
   * validated the user.
   *
   * Address: POST `/session`.
   * Response: session token in plaintext.
   */
  suspend fun login(call: ApplicationCall) {
    call.respondText(
        text = call.authentication.principal<UserPrincipal>()!!.token,
        contentType = ContentType.Text.Plain,
        status = HttpStatusCode.OK)
  }

  /**
   * Routing call for logout of an active session. Returns nothing.
   *
   * Address: DELETE `/session`.
   * Response: nothing.
   */
  suspend fun logout(call: ApplicationCall) {
    sessionController.logout(call.authentication.principal<UserPrincipal>()!!.token)
    call.respondText(
        text = "", contentType = ContentType.Text.Plain, status = HttpStatusCode.OK)
  }
}
