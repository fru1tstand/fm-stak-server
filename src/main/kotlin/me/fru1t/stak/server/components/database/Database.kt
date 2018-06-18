package me.fru1t.stak.server.components.database

import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserId

/** Defines database operations available. */
interface Database {
  /** Fetches a [User] by their [userId]. */
  fun getUserById(userId: UserId): DatabaseResult<User>

  /** Creates a new [User] within the database if the [User.userId] is unique. */
  fun createUser(user: User): DatabaseOperationResult

  /** Deletes an existing [User] within the database if it exists. */
  fun deleteUser(userId: UserId): DatabaseOperationResult

  /** Updates an existing user in a single transaction (including their user id if changed). */
  fun updateUser(userId: UserId, updatedUser: User): DatabaseOperationResult
}
