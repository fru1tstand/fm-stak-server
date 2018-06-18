package me.fru1t.stak.server.components.user.impl

import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import me.fru1t.stak.server.models.UserId
import javax.inject.Inject

/** The default implementation of [UserController]. */
class UserControllerImpl @Inject constructor(
    private val database: Database,
    private val security: Security) : UserController {
  override fun createUser(userCreate: UserCreate): Result<User?, UserController.CreateUserStatus> {
    val newUser =
      User(
          userId = UserId(userCreate.username),
          passwordHash = security.hash(userCreate.password),
          displayName = userCreate.displayName)

    val createUserResult = database.createUser(newUser)
    return when (createUserResult.status) {
      Database.CreateUserStatus.SUCCESS -> Result(newUser, UserController.CreateUserStatus.SUCCESS)
      Database.CreateUserStatus.USER_ID_ALREADY_EXISTS ->
        Result(null, UserController.CreateUserStatus.USER_ID_ALREADY_EXISTS)
      Database.CreateUserStatus.DATABASE_ERROR ->
        Result(null, UserController.CreateUserStatus.DATABASE_ERROR)
    }
  }
}
