package me.fru1t.stak.server.components.database.json

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import me.fru1t.stak.server.components.database.Database.GetUserByIdStatus
import me.fru1t.stak.server.components.database.Database.CreateUserStatus
import me.fru1t.stak.server.components.database.Database.DeleteUserStatus
import me.fru1t.stak.server.components.database.Database.UpdateUserStatus
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserId
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
    private val TEST_USER_1 = User(UserId("TestUsername"), "test hash", "a display name")
    private val TEST_USER_2 = User(UserId("TestUsername2"), "test hash2", "a display name2")

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
     * Creates the equivalent data structure of a `JsonTable<User>` mapping a [User]'s user id to
     * their data.
     */
    private fun createUserTableMap(vararg users: User): MutableMap<String, User> =
      mutableMapOf(*users.map { Pair(it.userId.username, it) }.toTypedArray())
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

    val result = jsonDatabase.getUserById(TEST_USER_1.userId).value

    assertThat(result).isEqualTo(TEST_USER_1)
  }

  @Test fun getUserByUsername_notFound() {
    jsonDatabase = JsonDatabase(TEST_DATABASE_FOLDER + "does-not-exist", GSON)

    val result = jsonDatabase.getUserById(UserId("doesn't exist"))

    assertThat(result.value).isNull()
    assertThat(result.status).isEqualTo(GetUserByIdStatus.USER_ID_NOT_FOUND)
  }

  @Test fun createUser() {
    val result1 = jsonDatabase.createUser(TEST_USER_1)
    val result2 = jsonDatabase.createUser(TEST_USER_2)

    assertThat(result1.status).isEqualTo(CreateUserStatus.SUCCESS)
    assertThat(result2.status).isEqualTo(CreateUserStatus.SUCCESS)
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))
  }

  @Test fun createUser_duplicateUser() {
    val user2 = TEST_USER_1.copy(
        passwordHash = "different password hash", displayName = "different display name")

    jsonDatabase.createUser(TEST_USER_1)
    val result = jsonDatabase.createUser(user2)

    assertThat(result.status).isEqualTo(CreateUserStatus.USER_ID_ALREADY_EXISTS)
    verifyFileContents(TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))
  }

  @Test fun deleteUser() {
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.deleteUser(TEST_USER_1.userId)

    assertThat(result.status).isEqualTo(DeleteUserStatus.SUCCESS)
    verifyFileContents(TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_2)))
  }

  @Test fun deleteUser_userDoesNotExist() {
    writeToTestTable(TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))

    val result = jsonDatabase.deleteUser(TEST_USER_2.userId)

    assertThat(result.status).isEqualTo(DeleteUserStatus.USER_ID_NOT_FOUND)
    verifyFileContents(TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))
  }

  @Test fun updateUser_noUsernameChange() {
    val modifiedUser = TEST_USER_1.copy(passwordHash = "different password hash")
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.updateUser(TEST_USER_1.userId, modifiedUser)

    assertThat(result.status).isEqualTo(UpdateUserStatus.SUCCESS)
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(modifiedUser, TEST_USER_2)))
  }

  @Test fun updateUser_usernameChange() {
    val modifiedUser = TEST_USER_1.copy(userId = UserId("something else as a username"))
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.updateUser(TEST_USER_1.userId, modifiedUser)

    assertThat(result.status).isEqualTo(UpdateUserStatus.SUCCESS)
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(modifiedUser, TEST_USER_2)))
  }

  @Test fun updateUser_noChange() {
    val modifiedUser = TEST_USER_1.copy()
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.updateUser(TEST_USER_1.userId, modifiedUser)

    assertThat(result.status).isEqualTo(UpdateUserStatus.SUCCESS)
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(modifiedUser, TEST_USER_2)))
  }

  @Test fun updateUser_oldUserDoesNotExist() {
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))

    val result = jsonDatabase.updateUser(TEST_USER_2.userId, TEST_USER_1)

    assertThat(result.status).isEqualTo(UpdateUserStatus.USER_ID_NOT_FOUND)
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1)))
  }

  @Test fun updateUser_usernameChange_duplicateUsername() {
    val modifiedUser = TEST_USER_1.copy(userId = TEST_USER_2.userId)
    writeToTestTable(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))

    val result = jsonDatabase.updateUser(TEST_USER_1.userId, modifiedUser)

    assertThat(result.status).isEqualTo(UpdateUserStatus.NEW_USER_ID_ALREADY_EXISTS)
    verifyFileContents(
        TEST_USER_TABLE_PATH, GSON.toJson(createUserTableMap(TEST_USER_1, TEST_USER_2)))
  }
}
