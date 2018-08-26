package me.fru1t.stak.server.components.user.impl

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserModify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class UserControllerImplTest {
  companion object {
    private const val TEST_PASSWORD_HASH = "test hash"
    private const val TEST_UPDATED_PASSWORD_HASH = "test updated hash"
    private val TEST_USER_CREATE = UserCreate("test username", "test password", "test display name")
    private val TEST_USER_ID = UserId("test username")
    private val TEST_USER = User(TEST_USER_ID, TEST_PASSWORD_HASH, "test display name")
    private val TEST_USER_MODIFY_DELTA =
      UserModify("different username", "different password", "different display name")
  }

  @Mock private lateinit var mockDatabase: Database
  @Mock private lateinit var mockSecurity: Security
  @Mock private lateinit var mockSessionController: SessionController

  private lateinit var userCaptor: KArgumentCaptor<User>
  private lateinit var controller: UserControllerImpl

  @BeforeEach fun setUp() {
    MockitoAnnotations.initMocks(this)
    userCaptor = argumentCaptor()

    controller =
        UserControllerImpl(
            database = mockDatabase,
            security = mockSecurity,
            sessionController = mockSessionController)
  }

  @Nested inner class CreateUserTest {
    @BeforeEach fun setUp() {
      whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)
      whenever(mockDatabase.createUser(any())).thenReturn(Status(Database.CreateUserStatus.SUCCESS))
    }

    @Test fun success() {
      val result = controller.createUser(TEST_USER_CREATE)

      assertThat(result.status).isEqualTo(UserController.CreateUserStatus.SUCCESS)
      assertThat(result.value!!.userId.username).isEqualTo(TEST_USER_CREATE.username)
      assertThat(result.value!!.passwordHash).isEqualTo(TEST_PASSWORD_HASH)
      assertThat(result.value!!.displayName).isEqualTo(TEST_USER_CREATE.displayName)
    }

    @Test fun databaseError() {
      whenever(mockDatabase.createUser(any()))
        .thenReturn(Status(Database.CreateUserStatus.DATABASE_ERROR))
      whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)

      val result = controller.createUser(TEST_USER_CREATE)

      assertThat(result.status).isEqualTo(UserController.CreateUserStatus.DATABASE_ERROR)
      assertThat(result.value).isNull()
    }

    @Test fun existingUser() {
      whenever(mockDatabase.createUser(any()))
        .thenReturn(Status(Database.CreateUserStatus.USER_ID_ALREADY_EXISTS))
      whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)

      val result = controller.createUser(TEST_USER_CREATE)

      assertThat(result.status).isEqualTo(UserController.CreateUserStatus.USER_ID_ALREADY_EXISTS)
      assertThat(result.value).isNull()
    }
  }

  @Nested inner class DeleteUserTest {
    @BeforeEach fun setUp() {
      whenever(mockDatabase.deleteUser(any()))
        .thenReturn(Status(Database.DeleteUserStatus.SUCCESS))
      whenever(mockSessionController.stopAllSessionsForUserId(any()))
        .thenReturn(Status(SessionController.StopAllSessionsForUserIdStatus.SUCCESS))
    }

    @Test fun success() {
      val result = controller.deleteUser(TEST_USER_ID)

      assertThat(result.status).isEqualTo(UserController.DeleteUserStatus.SUCCESS)
      verify(mockSessionController).stopAllSessionsForUserId(TEST_USER_ID)
    }

    @Test fun userIdNotFound() {
      whenever(mockDatabase.deleteUser(any()))
        .thenReturn(Status(Database.DeleteUserStatus.USER_ID_NOT_FOUND))

      val result = controller.deleteUser(TEST_USER_ID)

      assertThat(result.status).isEqualTo(UserController.DeleteUserStatus.USER_ID_NOT_FOUND)
    }

    @Test fun deleteFromDatabaseError() {
      whenever(mockDatabase.deleteUser(any()))
        .thenReturn(Status(Database.DeleteUserStatus.DATABASE_ERROR))

      val result = controller.deleteUser(TEST_USER_ID)

      assertThat(result.status).isEqualTo(UserController.DeleteUserStatus.DATABASE_ERROR)
    }

    @Test fun terminateSessionsDatabaseError() {
      whenever(mockSessionController.stopAllSessionsForUserId(any()))
        .thenReturn(Status(SessionController.StopAllSessionsForUserIdStatus.DATABASE_ERROR))

      val result = controller.deleteUser(TEST_USER_ID)

      assertThat(result.status)
        .isEqualTo(UserController.DeleteUserStatus.DATABASE_ERROR_ON_SESSION_DELETE)
    }
  }

  @Nested inner class ModifyUserTest {
    @BeforeEach fun setUp() {
      whenever(mockSecurity.hash(any())).thenReturn(TEST_UPDATED_PASSWORD_HASH)
      whenever(mockDatabase.getUserById(TEST_USER_ID))
        .thenReturn(Result(TEST_USER, Database.GetUserByIdStatus.SUCCESS))
      whenever(mockDatabase.updateUser(eq(TEST_USER_ID), any()))
        .thenReturn(Status(Database.UpdateUserStatus.SUCCESS))
    }

    @Test fun success() {
      val result = controller.modifyUser(TEST_USER_ID, TEST_USER_MODIFY_DELTA)

      verify(mockDatabase).updateUser(eq(TEST_USER_ID), userCaptor.capture())
      assertThat(result.status).isEqualTo(UserController.ModifyUserStatus.SUCCESS)

      val updatedUser = userCaptor.firstValue
      assertThat(updatedUser.userId.username).isEqualTo(TEST_USER_MODIFY_DELTA.username)
      assertThat(updatedUser.displayName).isEqualTo(TEST_USER_MODIFY_DELTA.displayName)
      assertThat(updatedUser.passwordHash).isEqualTo(TEST_UPDATED_PASSWORD_HASH)
    }

    @Test fun usernameUnchanged() {
      val delta = UserModify(password = "alternate pw", displayName = "alternate name")

      val result = controller.modifyUser(TEST_USER_ID, delta)

      verify(mockDatabase).updateUser(eq(TEST_USER_ID), userCaptor.capture())
      assertThat(result.status).isEqualTo(UserController.ModifyUserStatus.SUCCESS)

      val updatedUser = userCaptor.firstValue
      assertThat(updatedUser.userId).isEqualTo(TEST_USER_ID)
      assertThat(updatedUser.displayName).isEqualTo(delta.displayName)
      assertThat(updatedUser.passwordHash).isEqualTo(TEST_UPDATED_PASSWORD_HASH)
    }

    @Test fun passwordUnchanged() {
      val delta = UserModify(username = "alternate username", displayName = "alternate name")

      val result = controller.modifyUser(TEST_USER_ID, delta)

      verify(mockDatabase).updateUser(eq(TEST_USER_ID), userCaptor.capture())
      assertThat(result.status).isEqualTo(UserController.ModifyUserStatus.SUCCESS)

      val updatedUser = userCaptor.firstValue
      assertThat(updatedUser.userId.username).isEqualTo(delta.username)
      assertThat(updatedUser.displayName).isEqualTo(delta.displayName)
      assertThat(updatedUser.passwordHash).isEqualTo(TEST_PASSWORD_HASH)
    }

    @Test fun displayNameUnchanged() {
      val delta = UserModify(username = "alternate username", password = "alternate pw")

      val result = controller.modifyUser(TEST_USER_ID, delta)

      verify(mockDatabase).updateUser(eq(TEST_USER_ID), userCaptor.capture())
      assertThat(result.status).isEqualTo(UserController.ModifyUserStatus.SUCCESS)

      val updatedUser = userCaptor.firstValue
      assertThat(updatedUser.userId.username).isEqualTo(delta.username)
      assertThat(updatedUser.displayName).isEqualTo(TEST_USER.displayName)
      assertThat(updatedUser.passwordHash).isEqualTo(TEST_UPDATED_PASSWORD_HASH)
    }

    @Test fun database_getUserById_userNotFound() {
      setUpFailingDatabaseGetUserByIdAndExpect(
        Database.GetUserByIdStatus.USER_ID_NOT_FOUND,
        UserController.ModifyUserStatus.USER_ID_NOT_FOUND)
    }

    @Test fun database_getUserById_databaseError() {
      setUpFailingDatabaseGetUserByIdAndExpect(
        Database.GetUserByIdStatus.DATABASE_ERROR,
        UserController.ModifyUserStatus.DATABASE_ERROR)
    }

    @Test fun database_updateUser_userIdNotFound() {
      setUpFailingDatabaseUpdateUserAndExpect(
        Database.UpdateUserStatus.USER_ID_NOT_FOUND,
        UserController.ModifyUserStatus.USER_ID_NOT_FOUND)
    }

    @Test fun database_updateUser_newUserIdAlreadyExists() {
      setUpFailingDatabaseUpdateUserAndExpect(
        Database.UpdateUserStatus.NEW_USER_ID_ALREADY_EXISTS,
        UserController.ModifyUserStatus.NEW_USER_ID_ALREADY_EXISTS)
    }

    @Test fun database_updateUser_databaseError() {
      setUpFailingDatabaseUpdateUserAndExpect(
        Database.UpdateUserStatus.DATABASE_ERROR,
        UserController.ModifyUserStatus.DATABASE_ERROR)
    }

    /**
     * Sets up the [mockDatabase] to return the [getUserByIdStatus] when [Database.getUserById]
     * is called; then asserts that a resulting valid call to [UserControllerImpl.modifyUser]
     * returns the [expectedStatus].
     */
    private fun setUpFailingDatabaseGetUserByIdAndExpect(
      getUserByIdStatus: Database.GetUserByIdStatus,
      expectedStatus: UserController.ModifyUserStatus) {
      whenever(mockDatabase.getUserById(TEST_USER_ID))
        .thenReturn(Result(null, getUserByIdStatus))

      val result = controller.modifyUser(TEST_USER_ID, TEST_USER_MODIFY_DELTA)

      assertThat(result.status).isEqualTo(expectedStatus)
    }

    /**
     * Sets up the [mockDatabase] to return the [updateUserStatus] when [Database.updateUser] is
     * called; then asserts that a resulting valid call to [UserControllerImpl.modifyUser] returns
     * the [expectedStatus].
     */
    private fun setUpFailingDatabaseUpdateUserAndExpect(
      updateUserStatus: Database.UpdateUserStatus,
      expectedStatus: UserController.ModifyUserStatus) {
      whenever(mockDatabase.updateUser(eq(TEST_USER_ID), any()))
        .thenReturn(Status(updateUserStatus))

      val result = controller.modifyUser(TEST_USER_ID, TEST_USER_MODIFY_DELTA)

      assertThat(result.status).isEqualTo(expectedStatus)
    }
  }
}
