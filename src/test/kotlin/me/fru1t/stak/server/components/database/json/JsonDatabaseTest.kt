package me.fru1t.stak.server.components.database.json

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import me.fru1t.stak.server.models.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class JsonDatabaseTest {
  companion object {
    private const val TEST_DATABASE_FOLDER = "test/json-database"

    private val GSON = Gson()
    private val TEST_USER_TABLE_PATH = Paths.get(TEST_DATABASE_FOLDER, JsonDatabase.USER_TABLE_FILE)
    private val TEST_USER_1 = User("TestUsername", "test hash", "a display name")
    private val TEST_USER_2 = User("TestUsername2", "test hash2", "a display name2")

    /** Writes [contents] into the [table] file. */
    private fun writeToTestTable(table: Path, contents: String) {
      Files.write(table, contents.toByteArray(JsonDatabase.CHARSET))
    }

    /** Asserts each line in the [table] file matches each line in [contents]. */
    private fun verifyFileContents(table: Path, contents: String) {
      val fileContents = Files.newBufferedReader(table, JsonDatabase.CHARSET)
      val expectedContents = BufferedReader(StringReader(contents))

      expectedContents.forEachLine {
        assertThat(fileContents.readLine()).isEqualTo(it)
      }
    }

    /**
     * Creates the equivalent data structure of a `JsonTable<User>` mapping a [User]'s username to
     * their user info.
     */
    private fun createUserTableMap(vararg users: User): MutableMap<String, User> =
      mutableMapOf(*users.map { Pair(it.username, it) }.toTypedArray())
  }

  private lateinit var jsonDatabase: JsonDatabase

  @BeforeEach internal fun setUp() {
    // Create test dir and clear all tables
    Files.createDirectories(Paths.get(TEST_DATABASE_FOLDER))
    writeToTestTable(TEST_USER_TABLE_PATH, "")

    jsonDatabase = JsonDatabase(TEST_DATABASE_FOLDER, GSON)
  }

  @Test fun getUserByUsername() {
    val users = createUserTableMap(TEST_USER_1)
    writeToTestTable(TEST_USER_TABLE_PATH, GSON.toJson(users))

    val result = jsonDatabase.getUserByUsername(TEST_USER_1.username).result!!

    assertThat(result).isEqualTo(TEST_USER_1)
  }

  @Test fun getUserByUsername_notFound() {
    jsonDatabase = JsonDatabase(TEST_DATABASE_FOLDER + "does-not-exist", GSON)

    val result = jsonDatabase.getUserByUsername("doesn't exist")

    assertThat(result.result).isNull()
    assertThat(result.isDatabaseError).isFalse()
    assertThat(result.error).contains("not found")
  }

  @Test fun createUser() {
    val result1 = jsonDatabase.createUser(TEST_USER_1)
    val result2 = jsonDatabase.createUser(TEST_USER_2)

    assertThat(result1.didSucceed).isTrue()
    assertThat(result1.isDatabaseError).isFalse()
    assertThat(result1.error).isNull()
    assertThat(result2.didSucceed).isTrue()
    assertThat(result2.isDatabaseError).isFalse()
    assertThat(result2.error).isNull()
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))
  }

  @Test fun createUser_duplicateUser() {
    val user2 = TEST_USER_1.copy(
        passwordHash = "different password hash", displayName = "different display name")

    jsonDatabase.createUser(TEST_USER_1)
    val result = jsonDatabase.createUser(user2)

    assertThat(result.didSucceed).isFalse()
    assertThat(result.error).contains(user2.username)
    assertThat(result.error).contains("already exists")
    assertThat(result.isDatabaseError).isFalse()
    verifyFileContents(TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))
  }

  @Test fun deleteUser() {
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.deleteUser(TEST_USER_1.username)

    assertThat(result.didSucceed).isTrue()
    assertThat(result.error).isNull()
    assertThat(result.isDatabaseError).isFalse()
    verifyFileContents(TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_2)))
  }

  @Test fun deleteUser_userDoesNotExist() {
    writeToTestTable(TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))

    val result = jsonDatabase.deleteUser(TEST_USER_2.username)

    assertThat(result.didSucceed).isFalse()
    assertThat(result.error).contains(TEST_USER_2.username)
    assertThat(result.error).contains("doesn't exist")
    assertThat(result.isDatabaseError).isFalse()
    verifyFileContents(TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))
  }

  @Test fun updateUser_noUsernameChange() {
    val modifiedUser = TEST_USER_1.copy(passwordHash = "different password hash")
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.updateUser(TEST_USER_1, modifiedUser)

    assertThat(result.didSucceed).isTrue()
    assertThat(result.error).isNull()
    assertThat(result.isDatabaseError).isFalse()
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(modifiedUser, TEST_USER_2)))
  }

  @Test fun updateUser_usernameChange() {
    val modifiedUser = TEST_USER_1.copy(username = "something else as a username")
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.updateUser(TEST_USER_1, modifiedUser)

    assertThat(result.didSucceed).isTrue()
    assertThat(result.error).isNull()
    assertThat(result.isDatabaseError).isFalse()
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(modifiedUser, TEST_USER_2)))
  }

  @Test fun updateUser_noChange() {
    val modifiedUser = TEST_USER_1.copy()
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.updateUser(TEST_USER_1, modifiedUser)

    assertThat(result.didSucceed).isTrue()
    assertThat(result.error).isNull()
    assertThat(result.isDatabaseError).isFalse()
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(modifiedUser, TEST_USER_2)))
  }

  @Test fun updateUser_oldUserDoesNotExist() {
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))

    val result = jsonDatabase.updateUser(TEST_USER_2, TEST_USER_1)

    assertThat(result.didSucceed).isFalse()
    assertThat(result.error).contains(TEST_USER_2.username)
    assertThat(result.error).contains("doesn't exist")
    assertThat(result.isDatabaseError).isFalse()
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))
  }

  @Test fun updateUser_usernameChange_duplicateUsername() {
    val modifiedUser = TEST_USER_1.copy(username = TEST_USER_2.username)
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.updateUser(TEST_USER_1, modifiedUser)

    assertThat(result.didSucceed).isFalse()
    assertThat(result.error).contains(TEST_USER_1.username)
    assertThat(result.error).contains(TEST_USER_2.username)
    assertThat(result.error).contains("already exists")
    assertThat(result.isDatabaseError).isFalse()
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))
  }
}
