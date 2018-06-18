package me.fru1t.stak.server.components.user.impl

import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.components.user.UserController.CreateUserStatus
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import me.fru1t.stak.server.models.UserId
import javax.inject.Inject

/** The default implementation of [UserController]. */
class UserControllerImpl @Inject constructor(
    private val database: Database,
    private val security: Security) : UserController {
  override fun createUser(userCreate: UserCreate): Result<User?, CreateUserStatus> {
    val newUser =
      User(
          userId = UserId(userCreate.username),
          passwordHash = security.hash(userCreate.password),
          displayName = userCreate.displayName)

    val createUserResult = database.createUser(newUser)
    if (!createUserResult.didSucceed) {
      return Result(
          null,
          if (createUserResult.isDatabaseError)
            CreateUserStatus.DATABASE_ERROR
          else
            CreateUserStatus.USERNAME_ALREADY_EXISTS)
    }

    return Result(newUser, CreateUserStatus.SUCCESS)
  }
}
