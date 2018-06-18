package me.fru1t.stak.server.components.database

import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserId

/** Defines database operations available. */
interface Database {
  /** Status states for [getUserById]. */
  enum class GetUserByIdStatus {
    /**
     * A [User] was successfully fetched from the database. This is the only status that returns
     * a value.
     */
    SUCCESS,

    /**
     * A [User] with the given [UserId] was not found within the database. This status implies a
     * round-trip to the database was successfully carried out.
     */
    USER_ID_NOT_FOUND,

    /**
     * An error within the database occurred. This can imply a temporary failure (ie. network issue)
     * or a permanent failure (ie. implementation issue).
     */
    DATABASE_ERROR
  }

  /** Status states for [createUser]. */
  enum class CreateUserStatus {
    /** The [User] was successfully stored in the database. */
    SUCCESS,

    /**
     * The [User] was unable to be created as the [User.userId] conflicted with an existing one.
     * This status implies a round-trip to the database was successfully completed.
     */
    USER_ID_ALREADY_EXISTS,

    /**
     * An error within the database occurred. this can imply a temporary failure (ie. network issue)
     * or a permanent failure (ie. implementation issue).
     */
    DATABASE_ERROR
  }

  /** Status states for [deleteUser]. */
  enum class DeleteUserStatus {
    /** The [User] with [UserId] was successfully removed from the database. */
    SUCCESS,

    /**
     * The [User] with [UserId] was not found within the database, and thus could not be removed.
     * This status implies a round-trip to the database was successfully completed.
     */
    USER_ID_NOT_FOUND,

    /**
     * An error within the database occurred. this can imply a temporary failure (ie. network issue)
     * or a permanent failure (ie. implementation issue).
     */
    DATABASE_ERROR
  }

  /** Status states for [updateUser]. */
  enum class UpdateUserStatus {
    /** The update completed successfully. */
    SUCCESS,

    /**
     * The [UserId] for the existing user was not found and thus could not be updated. This status
     * implies a round-trip to the database completed successfully.
     */
    USER_ID_NOT_FOUND,

    /**
     * The new [UserId] provided already exists and thus no updates to the [User] were completed.
     * This status implies a round-trip to the database completed successfully.
     */
    NEW_USER_ID_ALREADY_EXISTS,

    /**
     * An error within the database occurred. this can imply a temporary failure (ie. network issue)
     * or a permanent failure (ie. implementation issue).
     */
    DATABASE_ERROR
  }

  /** Fetches a [User] by their [userId]. */
  fun getUserById(userId: UserId): Result<User?, GetUserByIdStatus>

  /** Creates a new [User] within the database if the [User.userId] is unique. */
  fun createUser(user: User): Status<CreateUserStatus>

  /** Deletes an existing [User] within the database if it exists. */
  fun deleteUser(userId: UserId): Status<DeleteUserStatus>

  /** Updates an existing user in a single transaction (including their user id if changed). */
  fun updateUser(userId: UserId, updatedUser: User): Status<UpdateUserStatus>
}
