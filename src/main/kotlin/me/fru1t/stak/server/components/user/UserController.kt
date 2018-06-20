package me.fru1t.stak.server.components.user

import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import me.fru1t.stak.server.models.UserId

/** Handles manipulation of [User]s like creation, modification, and deletion. */
interface UserController {
  /** Possible status states for [createUser]. */
  enum class CreateUserStatus {
    /**
     * A [User] was successfully created and inserted into the database. Returns the resulting
     * [User] object.
     */
    SUCCESS,

    /**
     * Failed to create user due to the [User.userId] conflicting with an existing [User]. This
     * status implicitly states that a round-trip to the database completed successfully.
     */
    USER_ID_ALREADY_EXISTS,

    /**
     * Failed to create user due to a failure with the database. This could represent a temporary
     * failure (ie. network error) or a permanent one (ie. implementation error).
     */
    DATABASE_ERROR
  }

  enum class DeleteUserStatus {
    /**
     * The [User] with [UserId] was successfully deleted from the database and all active sessions
     * with the [User] were terminated.
     */
    SUCCESS,

    /**
     * The [User] with [UserId] could not be deleted as the [UserId] was not found. This status
     * implies a round-trip to the database was successfully completed.
     */
    USER_ID_NOT_FOUND,

    /**
     * The [User] with [UserId] could not be deleted as a database error occurred. This could
     * represent a temporary error (ie. network failure) or a permanent error (ie. implementation
     * bug).
     */
    DATABASE_ERROR,

    /**
     * The [User] with [UserId] was successfully deleted from the user database, but sessions
     * could not be terminated due to a database error. See
     * [SessionController.StopAllSessionsForUserIdStatus.DATABASE_ERROR].
     */
    DATABASE_ERROR_ON_SESSION_DELETE
  }

  /**
   * Attempts to create a new user from [userCreate]. Returns the resulting [User] on
   * [CreateUserStatus.SUCCESS], otherwise `null`.
   */
  fun createUser(userCreate: UserCreate): Result<User?, CreateUserStatus>

  /** Deletes the [User] with the given [userId]. See [DeleteUserStatus]. */
  fun deleteUser(userId: UserId): Status<DeleteUserStatus>
}
