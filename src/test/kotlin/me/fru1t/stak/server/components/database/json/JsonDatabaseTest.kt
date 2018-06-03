package me.fru1t.stak.server.components.database.json

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import me.fru1t.stak.server.models.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JsonDatabaseTest {
  companion object {
    private const val TEST_DATABASE_FOLDER = "test/json-database"

    private val GSON = Gson()
    private val TEST_USER_TABLE_PATH = Paths.get(TEST_DATABASE_FOLDER, JsonDatabase.USER_TABLE_FILE)

    private fun writeToTestTable(table: Path, contents: String) {
      Files.write(table, contents.toByteArray(JsonDatabase.CHARSET))
    }
  }

  private lateinit var jsonDatabase: JsonDatabase

  @BeforeEach internal fun setUp() {
    Files.createDirectories(Paths.get(TEST_DATABASE_FOLDER))
    jsonDatabase = JsonDatabase(TEST_DATABASE_FOLDER, GSON)
  }

  @Test fun getUserByUsername() {
    val testUser = User("TestUsername", "", "Test Display Name")
    val users = mutableListOf(testUser)
    writeToTestTable(TEST_USER_TABLE_PATH, GSON.toJson(users))

    val result = jsonDatabase.getUserByUsername(testUser.username).result!!

    assertThat(result).isEqualTo(testUser)
  }

  @Test fun getUserByUsername_notFound() {
    jsonDatabase = JsonDatabase(TEST_DATABASE_FOLDER + "does-not-exist", GSON)

    val result = jsonDatabase.getUserByUsername("doesn't exist")

    assertThat(result.result).isNull()
    assertThat(result.isDatabaseError).isFalse()
    assertThat(result.error).contains("not found")
  }
}
