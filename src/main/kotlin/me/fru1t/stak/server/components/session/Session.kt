package me.fru1t.stak.server.components.session

import io.ktor.auth.UserPasswordCredential
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.UserPrincipal

/**
 * Handles synchronizing state between requests by the same device. This component specifically
 * handles login security and any other persistent data within a single session.
 */
interface Session {
  /**
   * Attempts to start a new session by validating a user's [userPasswordCredential]. On successful
   * validation, this method will generate and store a session token and return the [UserPrincipal]
   * for the session. On failure, this method will log any internal error, and return an empty
   * [Result].
   */
  fun login(userPasswordCredential: UserPasswordCredential): Result<UserPrincipal>

  /**
   * Ends an existing session by its [token] or does nothing. Returns whether or not the session was
   * terminated.
   */
  fun logout(token: String): Boolean

  /**
   * Attempts to retrieve an existing session by its [token]. Logs any internal errors and returns
   * an empty [Result] if no session at [token] was found.
   */
  fun getActiveSession(token: String): Result<UserPrincipal>
}
