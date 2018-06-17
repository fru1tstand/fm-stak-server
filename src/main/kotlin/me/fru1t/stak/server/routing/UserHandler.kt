package me.fru1t.stak.server.routing

import io.ktor.application.ApplicationCall
import io.ktor.auth.UserPasswordCredential
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.components.session.SessionController.LoginStatus
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.components.user.UserController.CreateUserStatus
import me.fru1t.stak.server.ktor.receiveOrBadRequest
import me.fru1t.stak.server.ktor.respondEmpty
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import mu.KLogging
import javax.inject.Inject

/** User routing. */
fun Route.user(userHandler: UserHandler) {
  route("user") {
    post { userHandler.createUser(context) }
  }
}

/** Handles REST operations for the "/user" space. */
class UserHandler @Inject constructor(
    private val userController: UserController,
    private val sessionController: SessionController) {
  companion object : KLogging()

  /**
   * Routing call for creating a new user. The following [HttpStatusCodes][HttpStatusCode] can be
   * returned:
   *
   * * [HttpStatusCode.BadRequest] - The [User] couldn't be created because the incoming request
   *   data was invalid. This might be caused due to bad client implementation, user error (or
   *   manipulation), or data corruption.
   * * [HttpStatusCode.Conflict] - The [User] couldn't be created because a [User] with the same
   *   [User.username] already exists. A retry should not be attempted as it will be expected to
   *   fail. The client should prompt the user to try with a different username.
   * * [HttpStatusCode.InternalServerError] - The [User] couldn't be created because a database
   *   error occurred. The client may retry as this is a temporary failure.
   * * [HttpStatusCode.ResetContent] - The [User] was successfully created, but the session failed
   *   to start due to login issues. This is usually indicative of a larger issue and should be
   *   treated as an error. This *should never* happen as it means user creation or login logic is
   *   incorrect.
   * * [HttpStatusCode.ServiceUnavailable] - The [User] was successfully created, but the session
   *   failed to start due to database issues. A retry should not be attempted as the [User] has
   *   already been created. Instead, the client should prompt to log in.
   * * [HttpStatusCode.Created] - The [User] was created successfully, a new session was started,
   *   and the session token is returned in the response body as [ContentType.Text.Plain]. The
   *   client should accept and use the token to access privileged content.
   */
  suspend fun createUser(call: ApplicationCall) {
    // BadRequest
    val userCreate = call.receiveOrBadRequest<UserCreate>() ?: return

    val createUserResult = userController.createUser(userCreate)
    when (createUserResult.status) {
      // Good. Continue to starting a session.
      CreateUserStatus.SUCCESS -> Unit
      // Conflict
      CreateUserStatus.USERNAME_ALREADY_EXISTS ->
        return call.respondEmpty(HttpStatusCode.Conflict)
      // InternalServerError
      CreateUserStatus.DATABASE_ERROR ->
        return call.respondEmpty(HttpStatusCode.InternalServerError)
    }

    val sessionResult =
      sessionController.login(UserPasswordCredential(userCreate.username, userCreate.password))
    return when (sessionResult.status) {
      // Created - Success!
      LoginStatus.SUCCESS ->
        call.respondText(
            sessionResult.value!!.token, ContentType.Text.Plain, HttpStatusCode.Created)
      // Service unavailable
      LoginStatus.DATABASE_ERROR ->
        call.respondEmpty(HttpStatusCode.ServiceUnavailable)
      // Reset content
      LoginStatus.BAD_USERNAME_OR_PASSWORD ->
        call.respondEmpty(HttpStatusCode.ResetContent)
    }
  }
}
