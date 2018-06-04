package me.fru1t.stak.server.components.database.json

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.DatabaseOperationResult
import me.fru1t.stak.server.components.database.DatabaseResult
import me.fru1t.stak.server.models.User
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

  override fun getUserByUsername(username: String): DatabaseResult<User> =
    userTable.contents[username]
        ?.let { DatabaseResult(result = it) }
        ?: DatabaseResult(error = "User $username not found.")

  override fun createUser(user: User): DatabaseOperationResult {
    if (userTable.contents.containsKey(user.username)) {
      return DatabaseOperationResult(error = "User ${user.username} already exists.")
    }

    userTable.contents[user.username] = user

    return userTable.writeToDisk()
  }

  override fun deleteUser(username: String): DatabaseOperationResult {
    if (!userTable.contents.containsKey(username)) {
      return DatabaseOperationResult(error = "User $username doesn't exist.")
    }

    userTable.contents.remove(username)

    return userTable.writeToDisk()
  }

  override fun updateUser(oldUser: User, newUser: User): DatabaseOperationResult {
    if (!userTable.contents.containsKey(oldUser.username)) {
      return DatabaseOperationResult(error = "User ${oldUser.username} doesn't exist.")
    }

    // Don't do any operations if the user data is the same
    if (oldUser == newUser) {
      return DatabaseOperationResult(didSucceed = true)
    }

    if (oldUser.username != newUser.username) {
      // Username change, check that the new username doesn't already exist before adding
      if (userTable.contents.containsKey(newUser.username)) {
        return DatabaseOperationResult(
            error = "Can't change ${oldUser.username}'s username as the requested username " +
                "${newUser.username} already exists.")
      }
      userTable.contents[newUser.username] = newUser
      userTable.contents.remove(oldUser.username)
    } else {
      // Username didn't change, just do a replacement
      userTable.contents.replace(newUser.username, newUser)
    }

    return userTable.writeToDisk()
  }

  /** Creates a [JsonTable] for the given [tableFileName] and [tableModelClass]. */
  private fun <T : Any> getJsonTable(tableFileName: String, tableModelClass: KClass<T>) =
    JsonTable(Paths.get(databaseLocation, tableFileName), tableModelClass, gson)
}
