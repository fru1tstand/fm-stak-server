package me.fru1t.stak.server.components.database.json

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonParseException
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.DatabaseResult
import me.fru1t.stak.server.components.database.json.models.UserTable
import me.fru1t.stak.server.models.User
import mu.KLogging
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Named

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

    /* Table files constants. */
    @VisibleForTesting internal const val USER_TABLE_FILE = "users.json"
  }

  private val userTableFilePath = Paths.get(databaseLocation, USER_TABLE_FILE)

  private val userTable: UserTable by lazy {
    fetchTableFromDisk(userTableFilePath, { UserTable(ArrayList()) })
  }

  override fun getUserByUsername(username: String): DatabaseResult<User> =
    userTable.users.singleOrNull { it.username == username }
        ?.let { DatabaseResult(result = it) }
        ?: DatabaseResult(error = "User $username not found.")

  /**
   * Fetches the table at [tablePath] returning [default] if the table doesn't exist or couldn't
   * be read.
   */
  private inline fun <reified T> fetchTableFromDisk(tablePath: Path, default: () -> T): T {
    if (!Files.exists(userTableFilePath)) {
      return default()
    }

    try {
      return gson.fromJson(Files.newBufferedReader(userTableFilePath, CHARSET), T::class.java)
    } catch (e: JsonParseException) {
      logger.error(e) {
        "Couldn't json decode ${T::class.simpleName} table file at ${tablePath.normalize()}."
      }
    }

    return default()
  }
}
