package me.fru1t.stak.server.components.user.impl

import io.ktor.http.HttpStatusCode
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.models.LegacyResult
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import javax.inject.Inject

/** The default implementation of [UserController]. */
class UserControllerImpl @Inject constructor(
    private val database: Database,
    private val security: Security) : UserController {
  override fun createUser(userCreate: UserCreate): LegacyResult<User> {
    val newUser =
      User(
          username = userCreate.username,
          passwordHash = security.hash(userCreate.password),
          displayName = userCreate.displayName)

    val createUserResult = database.createUser(newUser)
    if (!createUserResult.didSucceed) {
      return LegacyResult(
          httpStatusCode =
          if (createUserResult.isDatabaseError)
            HttpStatusCode.InternalServerError else HttpStatusCode.Conflict)
    }

    return LegacyResult(newUser, HttpStatusCode.Created)
  }
}
