package me.fru1t.stak.server.components.database

import me.fru1t.stak.server.models.User

/** Defines database operations available. */
interface Database {
  /** Fetches a [User] by their [User.username]. */
  fun getUserByUsername(username: String): DatabaseResult<User>

  /** Creates a new [User] within the database if the [User.username] is unique. */
  fun createUser(user: User): DatabaseOperationResult

  /** Deletes an existing [User] within the database if it exists. */
  fun deleteUser(username: String): DatabaseOperationResult

  /**
   * Updates an existing [User] within the database if it exists, replacing [oldUser] with
   * [newUser] by matching [oldUser]'s [User.username].
   */
  fun updateUser(oldUser: User, newUser: User): DatabaseOperationResult
}
