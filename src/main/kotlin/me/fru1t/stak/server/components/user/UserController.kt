package me.fru1t.stak.server.components.user

import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate

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
     * Failed to create user due to the [User.username] conflicting with an existing [User]. This
     * status implicitly states that a round-trip to the database completed successfully.
     */
    USERNAME_ALREADY_EXISTS,

    /**
     * Failed to create user due to a failure with the database. This could represent a temporary
     * failure (ie. network error) or a permanent one (ie. implementation error).
     */
    DATABASE_ERROR
  }

  /**
   * Attempts to create a new user from [userCreate]. Returns the resulting [User] on
   * [CreateUserStatus.SUCCESS], otherwise `null`.
   */
  fun createUser(userCreate: UserCreate): Result<User?, CreateUserStatus>
}
