package me.fru1t.stak.server.components.database.json

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.Database.CreateUserStatus
import me.fru1t.stak.server.components.database.Database.DeleteUserStatus
import me.fru1t.stak.server.components.database.Database.GetUserByIdStatus
import me.fru1t.stak.server.components.database.Database.UpdateUserStatus
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserId
import mu.KLogging
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KClass

/**
 * A Json file-based database. This database will load and store the entirety of a table into memory
 * lazily on-demand. To keep memory and disk in-sync, this database will write to disk on every
 * command that modifies data. This database will also fail silently for any I/O related reasons.
 */
class JsonDatabase @Inject constructor(
    @Named(Constants.NAMED_DATABASE_FOLDER) private val databaseLocation: String,
    private val gson: Gson) : Database {
  companion object : KLogging() {
    /** Charset to use for encode/decoding json files. */
    @VisibleForTesting internal val CHARSET = StandardCharsets.UTF_8

    /* JsonTable files constants. */
    @VisibleForTesting internal const val USER_TABLE_FILE = "users.json"
  }

  private val userTable = getJsonTable(USER_TABLE_FILE, User::class)

  override fun getUserById(userId: UserId): Result<User?, GetUserByIdStatus> =
    userTable.contents[userId.username]
        ?.let { Result(it, GetUserByIdStatus.SUCCESS) }
        ?: Result(null, GetUserByIdStatus.USER_ID_NOT_FOUND)

  override fun createUser(user: User): Status<CreateUserStatus> {
    if (userTable.contents.containsKey(user.userId.username)) {
      return Status(CreateUserStatus.USER_ID_ALREADY_EXISTS)
    }

    userTable.contents[user.userId.username] = user

    return when (userTable.writeToDisk().status) {
      JsonTable.WriteToDiskStatus.SUCCESS -> Status(CreateUserStatus.SUCCESS)
      JsonTable.WriteToDiskStatus.IO_EXCEPTION -> Status(CreateUserStatus.DATABASE_ERROR)
    }
  }

  override fun deleteUser(userId: UserId): Status<DeleteUserStatus> {
    if (!userTable.contents.containsKey(userId.username)) {
      return Status(DeleteUserStatus.USER_ID_NOT_FOUND)
    }

    userTable.contents.remove(userId.username)

    return when (userTable.writeToDisk().status) {
      JsonTable.WriteToDiskStatus.SUCCESS -> Status(DeleteUserStatus.SUCCESS)
      JsonTable.WriteToDiskStatus.IO_EXCEPTION -> Status(DeleteUserStatus.DATABASE_ERROR)
    }
  }

  override fun updateUser(userId: UserId, updatedUser: User): Status<UpdateUserStatus> {
    val oldUser = userTable.contents[userId.username]
        ?: return Status(UpdateUserStatus.USER_ID_NOT_FOUND)

    // Don't do any operations if the user data is the same
    if (oldUser == updatedUser) {
      return Status(UpdateUserStatus.SUCCESS)
    }

    if (userId != updatedUser.userId) {
      // User id change, check that the new user id doesn't already exist before adding
      if (userTable.contents.containsKey(updatedUser.userId.username)) {
        return Status(UpdateUserStatus.NEW_USER_ID_ALREADY_EXISTS)
      }
      userTable.contents[updatedUser.userId.username] = updatedUser
      userTable.contents.remove(oldUser.userId.username)
    } else {
      // Username didn't change, just do a replacement
      userTable.contents.replace(updatedUser.userId.username, updatedUser)
    }

    return when (userTable.writeToDisk().status) {
      JsonTable.WriteToDiskStatus.SUCCESS -> Status(UpdateUserStatus.SUCCESS)
      JsonTable.WriteToDiskStatus.IO_EXCEPTION -> Status(UpdateUserStatus.DATABASE_ERROR)
    }
  }

  /** Creates a [JsonTable] for the given [tableFileName] and [tableModelClass]. */
  private fun <T : Any> getJsonTable(tableFileName: String, tableModelClass: KClass<T>) =
    JsonTable(Paths.get(databaseLocation, tableFileName), tableModelClass, gson)
}
