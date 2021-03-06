package me.fru1t.stak.server.components.session

import io.ktor.auth.UserPasswordCredential
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserPrincipal

/**
 * Handles synchronizing state between requests by the same device. This component specifically
 * handles login security and any other persistent data within a single session.
 */
interface SessionController {
  /** Status states for [login]. */
  enum class LoginStatus {
    /**
     * Returned when an unexpected database error occurred (for example, disk read/write failure,
     * network failure, query failure, etc). This status encapsulates both temporary and permanent
     * failures (ie. as simple as network timeout, or as complex as implementation bug). Provides
     * no return value.
     */
    DATABASE_ERROR,

    /**
     * Returned when either the user id or the password (or both) are incorrect for this login
     * attempt. This status implies the database successfully queried and returned a result
     * (including a result of length zero in the case if the user id was incorrect). Provides no
     * return value.
     */
    BAD_USERNAME_OR_PASSWORD,

    /**
     * Both user id and password matched a single user on file and as such, the caller should
     * assume a valid log in sequence. At this point, a new session has been created for the
     * user, and the [login] method will return a valid [UserPrincipal].
     */
    SUCCESS
  }

  /** Status states for [getActiveSession]. */
  enum class GetActiveSessionStatus {
    /** An unexpected database error occurred. Returns `null`. */
    DATABASE_ERROR,

    /** The session wasn't found within the database. Returns `null`. */
    SESSION_NOT_FOUND,

    /** Successfully found the session. Returns the associated [UserPrincipal]. */
    SUCCESS
  }

  /** Status states for [stopAllSessionsForUserId]. */
  enum class StopAllSessionsForUserIdStatus {
    /**
     * No sessions were stopped because an error occurred when contacting the database. This could
     * mean a temporary failure (ie. network failure) or a permanent failure (ie. implementation
     * error).
     */
    DATABASE_ERROR,

    /**
     * All sessions with the [UserId] were stopped successfully. This could also mean no sessions
     * were stopped if the [UserId] had no active sessions or doesn't exist.
     */
    SUCCESS
  }

  /** Status states for [replaceSession]. */
  enum class ReplaceSessionStatus {
    /**
     * No session was replaced because an error occurred when contacting the database. This could
     * mean a temporary failure or a permanent one.
     */
    DATABASE_ERROR,

    /**
     * No session was replaced because there was no active session with the given token. This
     * implies a round-trip to the database was successfully carried out.
     */
    TOKEN_NOT_FOUND,

    /**
     * The session with the passed in token string was successfully replaced by the provided
     * [UserId].
     */
    SUCCESS
  }

  /**
   * Attempts to start a new session by validating a user's [userPasswordCredential]. On successful
   * validation, this method will generate and store a session token and return the [UserPrincipal]
   * for the session. See [LoginStatus] details on return values.
   */
  fun login(userPasswordCredential: UserPasswordCredential): Result<UserPrincipal?, LoginStatus>

  /**
   * Ends an existing session by its [token] or does nothing. Returns whether or not the session was
   * terminated.
   */
  fun logout(token: String): Boolean

  /**
   * Attempts to retrieve an existing session by its [token]. Logs any internal errors and returns
   * an empty [Result] if no session at [token] was found. See [GetActiveSessionStatus].
   */
  fun getActiveSession(token: String): Result<UserPrincipal?, GetActiveSessionStatus>

  /** Stops all sessions for a [userId]. See [StopAllSessionsForUserIdStatus]. */
  fun stopAllSessionsForUserId(userId: UserId): Status<StopAllSessionsForUserIdStatus>

  /** Replaces the session identified by [token] with the [newUserId]. */
  fun replaceSession(token: String, newUserId: UserId): Status<ReplaceSessionStatus>
}
