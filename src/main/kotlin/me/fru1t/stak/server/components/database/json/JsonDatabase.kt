package me.fru1t.stak.server.components.database.json

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.DatabaseOperationResult
import me.fru1t.stak.server.components.database.DatabaseResult
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

  override fun getUserById(userId: UserId): DatabaseResult<User> =
    userTable.contents[userId.username]
        ?.let { DatabaseResult(result = it) }
        ?: DatabaseResult(error = "User userId not found.")

  override fun createUser(user: User): DatabaseOperationResult {
    if (userTable.contents.containsKey(user.userId.username)) {
      return DatabaseOperationResult(error = "User ${user.userId.username} already exists.")
    }

    userTable.contents[user.userId.username] = user

    return userTable.writeToDisk()
  }

  override fun deleteUser(userId: UserId): DatabaseOperationResult {
    if (!userTable.contents.containsKey(userId.username)) {
      return DatabaseOperationResult(error = "User $userId doesn't exist.")
    }

    userTable.contents.remove(userId.username)

    return userTable.writeToDisk()
  }

  override fun updateUser(userId: UserId, updatedUser: User): DatabaseOperationResult {
    val oldUser = userTable.contents[userId.username]
        ?: return DatabaseOperationResult(error = "User $userId doesn't exist.")

    // Don't do any operations if the user data is the same
    if (oldUser == updatedUser) {
      return DatabaseOperationResult(didSucceed = true)
    }

    if (userId != updatedUser.userId) {
      // User id change, check that the new user id doesn't already exist before adding
      if (userTable.contents.containsKey(updatedUser.userId.username)) {
        return DatabaseOperationResult(
            error = "Can't change ${oldUser.userId}'s user id as the requested user id " +
                "${updatedUser.userId} already exists.")
      }
      userTable.contents[updatedUser.userId.username] = updatedUser
      userTable.contents.remove(oldUser.userId.username)
    } else {
      // Username didn't change, just do a replacement
      userTable.contents.replace(updatedUser.userId.username, updatedUser)
    }

    return userTable.writeToDisk()
  }

  /** Creates a [JsonTable] for the given [tableFileName] and [tableModelClass]. */
  private fun <T : Any> getJsonTable(tableFileName: String, tableModelClass: KClass<T>) =
    JsonTable(Paths.get(databaseLocation, tableFileName), tableModelClass, gson)
}
