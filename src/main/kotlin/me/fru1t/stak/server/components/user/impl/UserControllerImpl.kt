package me.fru1t.stak.server.components.user.impl

import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserModify
import javax.inject.Inject

/** The default implementation of [UserController]. */
class UserControllerImpl @Inject constructor(
    private val database: Database,
    private val security: Security,
    private val sessionController: SessionController) : UserController {
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

  override fun deleteUser(userId: UserId): Status<UserController.DeleteUserStatus> {
    // Delete from database
    val databaseDeleteStatus = database.deleteUser(userId)
    when (databaseDeleteStatus.status) {
      Database.DeleteUserStatus.SUCCESS -> Unit
      Database.DeleteUserStatus.USER_ID_NOT_FOUND ->
        return Status(UserController.DeleteUserStatus.USER_ID_NOT_FOUND)
      Database.DeleteUserStatus.DATABASE_ERROR ->
        return Status(UserController.DeleteUserStatus.DATABASE_ERROR)
    }

    // Terminate sessions
    val sessionStopStatus = sessionController.stopAllSessionsForUserId(userId)
    return when (sessionStopStatus.status) {
      SessionController.StopAllSessionsForUserIdStatus.DATABASE_ERROR ->
        Status(UserController.DeleteUserStatus.DATABASE_ERROR_ON_SESSION_DELETE)
      SessionController.StopAllSessionsForUserIdStatus.SUCCESS ->
        Status(UserController.DeleteUserStatus.SUCCESS)
    }
  }

  override fun modifyUser(
    userId: UserId,
    delta: UserModify): Result<User?, UserController.ModifyUserStatus> {
    val existingUserResult = database.getUserById(userId)
    val existingUser = when (existingUserResult.status) {
      Database.GetUserByIdStatus.SUCCESS ->
        existingUserResult.value!!
      Database.GetUserByIdStatus.USER_ID_NOT_FOUND ->
        return Result(null, UserController.ModifyUserStatus.USER_ID_NOT_FOUND)
      Database.GetUserByIdStatus.DATABASE_ERROR ->
        return Result(null, UserController.ModifyUserStatus.DATABASE_ERROR)
    }

    val updatedUserId = delta.username?.let { UserId(username = it) } ?: userId
    val updatedPasswordHash = delta.password?.let { security.hash(it) } ?: existingUser.passwordHash
    val updateDisplayName = delta.displayName ?: existingUser.displayName
    val updatedUser =
      User(
        userId = updatedUserId,
        passwordHash = updatedPasswordHash,
        displayName = updateDisplayName)

    return when (database.updateUser(userId, updatedUser).status) {
      Database.UpdateUserStatus.SUCCESS ->
        Result(updatedUser, UserController.ModifyUserStatus.SUCCESS)
      Database.UpdateUserStatus.USER_ID_NOT_FOUND ->
        Result(null, UserController.ModifyUserStatus.USER_ID_NOT_FOUND)
      Database.UpdateUserStatus.NEW_USER_ID_ALREADY_EXISTS ->
        Result(null, UserController.ModifyUserStatus.NEW_USER_ID_ALREADY_EXISTS)
      Database.UpdateUserStatus.DATABASE_ERROR ->
        Result(null, UserController.ModifyUserStatus.DATABASE_ERROR)
    }
  }
}
