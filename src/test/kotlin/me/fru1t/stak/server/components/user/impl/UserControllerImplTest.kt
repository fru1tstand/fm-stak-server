package me.fru1t.stak.server.components.user.impl

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.UserCreate
import me.fru1t.stak.server.models.UserId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class UserControllerImplTest {
  companion object {
    private val TEST_USER_CREATE = UserCreate("test username", "test password", "test display name")
    private val TEST_USER_ID = UserId("test username")
    private const val TEST_PASSWORD_HASH = "test hash"
  }

  @Mock private lateinit var mockDatabase: Database
  @Mock private lateinit var mockSecurity: Security
  @Mock private lateinit var mockSessionController: SessionController
  private lateinit var userControllerImpl: UserControllerImpl

  @BeforeEach internal fun setUp() {
    MockitoAnnotations.initMocks(this)

    whenever(mockDatabase.createUser(any())).thenReturn(Status(Database.CreateUserStatus.SUCCESS))
    whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)
    whenever(mockDatabase.deleteUser(any()))
        .thenReturn(Status(Database.DeleteUserStatus.SUCCESS))
    whenever(mockSessionController.stopAllSessionsForUserId(any()))
        .thenReturn(Status(SessionController.StopAllSessionsForUserIdStatus.SUCCESS))

    userControllerImpl =
        UserControllerImpl(
            database = mockDatabase,
            security = mockSecurity,
            sessionController = mockSessionController)
  }

  @Test fun createUser() {
    val result = userControllerImpl.createUser(TEST_USER_CREATE)

    assertThat(result.status).isEqualTo(UserController.CreateUserStatus.SUCCESS)
    assertThat(result.value!!.userId.username).isEqualTo(TEST_USER_CREATE.username)
    assertThat(result.value!!.passwordHash).isEqualTo(TEST_PASSWORD_HASH)
    assertThat(result.value!!.displayName).isEqualTo(TEST_USER_CREATE.displayName)
  }

  @Test fun createUser_databaseError() {
    whenever(mockDatabase.createUser(any()))
        .thenReturn(Status(Database.CreateUserStatus.DATABASE_ERROR))
    whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)

    val result = userControllerImpl.createUser(TEST_USER_CREATE)

    assertThat(result.status).isEqualTo(UserController.CreateUserStatus.DATABASE_ERROR)
    assertThat(result.value).isNull()
  }

  @Test fun createUser_existingUser() {
    whenever(mockDatabase.createUser(any()))
        .thenReturn(Status(Database.CreateUserStatus.USER_ID_ALREADY_EXISTS))
    whenever(mockSecurity.hash(any())).thenReturn(TEST_PASSWORD_HASH)

    val result = userControllerImpl.createUser(TEST_USER_CREATE)

    assertThat(result.status).isEqualTo(UserController.CreateUserStatus.USER_ID_ALREADY_EXISTS)
    assertThat(result.value).isNull()
  }

  @Test fun deleteUser() {
    val result = userControllerImpl.deleteUser(TEST_USER_ID)

    assertThat(result.status).isEqualTo(UserController.DeleteUserStatus.SUCCESS)
    verify(mockSessionController).stopAllSessionsForUserId(TEST_USER_ID)
  }

  @Test fun deleteUser_userIdNotFound() {
    whenever(mockDatabase.deleteUser(any()))
        .thenReturn(Status(Database.DeleteUserStatus.USER_ID_NOT_FOUND))

    val result = userControllerImpl.deleteUser(TEST_USER_ID)

    assertThat(result.status).isEqualTo(UserController.DeleteUserStatus.USER_ID_NOT_FOUND)
  }

  @Test fun deleteUser_deleteFromDatabaseError() {
    whenever(mockDatabase.deleteUser(any()))
        .thenReturn(Status(Database.DeleteUserStatus.DATABASE_ERROR))

    val result = userControllerImpl.deleteUser(TEST_USER_ID)

    assertThat(result.status).isEqualTo(UserController.DeleteUserStatus.DATABASE_ERROR)
  }

  @Test fun deleteUser_terminateSessionsDatabaseError() {
    whenever(mockSessionController.stopAllSessionsForUserId(any()))
        .thenReturn(Status(SessionController.StopAllSessionsForUserIdStatus.DATABASE_ERROR))

    val result = userControllerImpl.deleteUser(TEST_USER_ID)

    assertThat(result.status)
        .isEqualTo(UserController.DeleteUserStatus.DATABASE_ERROR_ON_SESSION_DELETE)
  }
}
