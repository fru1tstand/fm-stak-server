package me.fru1t.stak.server.components.database.json

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.database.Database
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

  /** Creates a [JsonTable] for the given [tableFileName] and [tableModelClass]. */
  private fun <T : Any> getJsonTable(tableFileName: String, tableModelClass: KClass<T>) =
    JsonTable(Paths.get(databaseLocation, tableFileName), tableModelClass, gson)
}
