package me.fru1t.stak.server.routing

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.FormAuthChallenge
import io.ktor.auth.Principal
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
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
import me.fru1t.stak.server.ktor.auth.bearer
import me.fru1t.stak.server.ktor.respondEmpty
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
  private companion object : KLogging()

  /**
   * Sets up a login and bearer authentication routes with [Constants.LOGIN_AUTH_NAME] and
   * [Constants.SESSION_AUTH_NAME], respectively.
   */
  fun registerAuthentication(configuration: Authentication.Configuration) {
    configuration.form(Constants.LOGIN_AUTH_NAME) {
      userParamName = Constants.LOGIN_USER_PARAM_NAME
      passwordParamName = Constants.LOGIN_PASS_PARAM_NAME
      challenge = FormAuthChallenge.Unauthorized
      validate { credentials -> sessionController.login(credentials).value }
    }
    configuration.bearer(Constants.SESSION_AUTH_NAME) {
      validate { token ->
        run<Principal?> {
          val result = sessionController.getActiveSession(token)
          if (result.status != SessionController.GetActiveSessionStatus.SUCCESS) {
            logger.debug { "Invalid session token for host ${request.origin.host}, game: $token" }
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
    call.respondEmpty(HttpStatusCode.OK)
  }
}
