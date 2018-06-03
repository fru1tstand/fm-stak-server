package me.fru1t.stak.server.components.database.json

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * Represents both the metadata and contents of a file-based JSON table. Tables are lazy loaded from
 * disk and must be manually saved on modification. A single [JsonTable] corresponds to a single
 * file on disk which is represented in Json with the root element as an array of [T].
 *
 * @constructor creates a new [JsonTable] located on disk at [tableFilePathAndName] (which accepts
 * both relative and absolute paths), storing [tableModelClass]es.
 */
class JsonTable<T : Any>(
    private val tableFilePathAndName: Path,
    private val tableModelClass: KClass<T>,
    private val gson: Gson) {

  val contents: MutableList<T> by lazy {
    if (!Files.exists(tableFilePathAndName)) {
      return@lazy ArrayList<T>()
    }

    try {
      @Suppress("UNCHECKED_CAST")
      return@lazy gson.fromJson<ArrayList<T>>(
          Files.newBufferedReader(tableFilePathAndName, JsonDatabase.CHARSET),
         TypeToken.getParameterized(ArrayList::class.java, tableModelClass.java).type)
    } catch (e: JsonParseException) {
      JsonDatabase.logger.error(e) {
        "Couldn't json decode ${tableModelClass.simpleName} table file at " +
            "${tableFilePathAndName.normalize()}."
      }
    }

    return@lazy ArrayList<T>()
  }
}
