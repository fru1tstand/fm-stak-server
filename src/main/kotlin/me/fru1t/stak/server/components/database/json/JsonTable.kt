package me.fru1t.stak.server.components.database.json

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import me.fru1t.stak.server.components.database.DatabaseOperationResult
import me.fru1t.stak.server.components.database.DatabaseResult
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * Represents both the metadata and contents of a file-based JSON table. Tables are lazy loaded from
 * disk and must be manually saved on modification. A single [JsonTable] corresponds to a single
 * file on disk which is represented in Json with the root element as a map of a [String] primary
 * key (arbitrarily defined) mapped to [T].
 *
 * @constructor creates a new [JsonTable] located on disk at [tableFilePathAndName] (which accepts
 * both relative and absolute paths), storing [tableModelClass]es.
 */
class JsonTable<T : Any>(
    private val tableFilePathAndName: Path,
    private val tableModelClass: KClass<T>,
    private val gson: Gson) {

  val contents: MutableMap<String, T> by lazy {
    if (!Files.exists(tableFilePathAndName)) {
      return@lazy HashMap<String, T>()
    }

    try {
      @Suppress("UNCHECKED_CAST")
      return@lazy gson.fromJson<HashMap<String, T>>(
          Files.newBufferedReader(tableFilePathAndName, JsonDatabase.CHARSET),
          TypeToken.getParameterized(
              HashMap::class.java, String::class.java, tableModelClass.java).type)
    } catch (e: JsonParseException) {
      JsonDatabase.logger.error(e) {
        "Couldn't json decode ${tableModelClass.simpleName} table file at " +
            "${tableFilePathAndName.normalize()}."
      }
    }

    return@lazy HashMap<String, T>()
  }

  /** Attempts to commit the current state of this [JsonTable] to disk. */
  fun writeToDisk(): DatabaseOperationResult {
    try {
      Files.write(tableFilePathAndName, gson.toJson(contents).toByteArray(JsonDatabase.CHARSET))
    } catch(e: IOException) {
      return DatabaseOperationResult(error = e.message, isDatabaseError = true)
    }
    return DatabaseOperationResult(didSucceed = true)
  }
}
