package me.fru1t.stak.server.components.user

import io.ktor.http.HttpStatusCode
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate

/** Handles manipulation of [User]s like creation, modification, and deletion. */
interface UserController {
  /**
   * Attempts to create a new user from [userCreate].
   *
   * @return The resulting [User] if successful, otherwise `null`. Possible
   * [Result.httpStatusCode]s:
   *
   * * [HttpStatusCode.Created] - A new [User] was created successfully and will be returned in the
   *   [Result.value]. This is the only response code which will have a non-null [Result.value].
   * * [HttpStatusCode.Conflict] - The [User.username] already exists so a new one with the same
   *   username cannot be created.
   * * [HttpStatusCode.InternalServerError] - A database error occurred.
   */
  fun createUser(userCreate: UserCreate): Result<User>
}
