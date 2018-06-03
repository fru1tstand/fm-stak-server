package me.fru1t.stak.server.components.database.json

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JsonTableTest {
  companion object {
    private const val TEST_DATABASE_FOLDER = "test/json-database"

    private val GSON = Gson()
    private val TEST_TABLE_PATH = Paths.get(TEST_DATABASE_FOLDER, "json-table-test.json")

    private fun writeToTestTable(table: Path, contents: String) {
      Files.write(table, contents.toByteArray(JsonDatabase.CHARSET))
    }
  }

  /** Test class used as the data of a table. */
  private data class TestModel(val column: String)

  /** Creates a JsonTable for [TestModel] given the file path. */
  private fun createJsonTable(tableFilePathAndName: Path): JsonTable<TestModel> =
    JsonTable(tableFilePathAndName, TestModel::class, GSON)


  @Test fun contents_fileNotExists() {
    val table = createJsonTable(Paths.get(TEST_DATABASE_FOLDER, "doesnt-exist.json"))

    assertThat(table.contents).isEmpty()
  }

  @Test fun contents_jsonError() {
    writeToTestTable(TEST_TABLE_PATH, "invalid { json")

    val table = createJsonTable(TEST_TABLE_PATH)

    assertThat(table.contents).isEmpty()
  }

  @Test fun contents() {
    val testContents = listOf(TestModel("contents 1"), TestModel("contents 2"))
    writeToTestTable(TEST_TABLE_PATH, GSON.toJson(testContents))

    val table = createJsonTable(TEST_TABLE_PATH)

    assertThat(table.contents).containsExactlyElementsIn(testContents)
  }

  @Test fun writeToDisk() {
    val preContents = listOf(TestModel("contents 1"), TestModel("contents 2"))
    writeToTestTable(TEST_TABLE_PATH, GSON.toJson(preContents))

    val table = createJsonTable(TEST_TABLE_PATH)
    table.contents.add(TestModel("contents 3"))
    table.contents.removeAt(0)
    val writeResult = table.writeToDisk()
    assertThat(writeResult.result!!).isTrue()

    val resultTable = createJsonTable(TEST_TABLE_PATH)
    assertThat(resultTable.contents)
        .containsExactlyElementsIn(listOf(TestModel("contents 2"), TestModel("contents 3")))
  }
}
