package me.fru1t.stak.server.components.database

import me.fru1t.stak.server.models.User

/** Defines database operations available. */
interface Database {
  /** Fetches a [User] by their [User.username]. */
  fun getUserByUsername(username: String): DatabaseResult<User>
}
