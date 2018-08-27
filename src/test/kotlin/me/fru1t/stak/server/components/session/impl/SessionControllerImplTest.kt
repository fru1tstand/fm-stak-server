package me.fru1t.stak.server.components.session.impl

import com.google.common.testing.FakeTicker
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.whenever
import io.ktor.auth.UserPasswordCredential
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.security.testing.FakeSecurity
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class SessionControllerImplTest {
  companion object {
    private val TEST_VALID_USER_ID = UserId("test-valid-username")
    private val TEST_VALID_USER_ID_2 = UserId("test-valid-username-2")
    private val TEST_REPLACEMENT_USER_ID = UserId("test-replacement-id")
    private const val TEST_VALID_PASSWORD = "test-valid-password"
    private const val TEST_SESSION_TIMEOUT_HOURS = 1L
  }

  @Mock private lateinit var mockDatabase: Database
  private lateinit var fakeSecurity: FakeSecurity
  private lateinit var fakeTicker: FakeTicker
  private lateinit var controller: SessionControllerImpl

  private lateinit var testValidPasswordHash: String

  @BeforeEach fun setUp() {
    MockitoAnnotations.initMocks(this)

    fakeTicker = FakeTicker()
    fakeSecurity = FakeSecurity()

    testValidPasswordHash = fakeSecurity.hash(TEST_VALID_PASSWORD)
    whenever(mockDatabase.getUserById(TEST_VALID_USER_ID))
      .thenReturn(
        Result(
          User(TEST_VALID_USER_ID, testValidPasswordHash, "Test display Name"),
          Database.GetUserByIdStatus.SUCCESS))
    whenever(mockDatabase.getUserById(TEST_VALID_USER_ID_2))
      .thenReturn(
        Result(
          User(TEST_VALID_USER_ID_2, testValidPasswordHash, "Test display Name 2"),
          Database.GetUserByIdStatus.SUCCESS))

    controller =
        SessionControllerImpl(mockDatabase, TEST_SESSION_TIMEOUT_HOURS, fakeSecurity, fakeTicker)
  }

  @Nested inner class LoginTest {
    @Test fun default() {
      val result =
        controller.login(UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))

      assertThat(result.status).isEqualTo(SessionController.LoginStatus.SUCCESS)
      assertThat(result.value).isNotNull()
      assertThat(result.value!!.userId).isEqualTo(TEST_VALID_USER_ID)
    }

    @Test fun invalidPassword() {
      val result =
        controller.login(UserPasswordCredential(TEST_VALID_USER_ID.username, "invalid pass"))

      assertThat(result.status).isEqualTo(SessionController.LoginStatus.BAD_USERNAME_OR_PASSWORD)
      assertThat(result.value).isNull()
    }

    @Test fun invalidUsername() {
      whenever(mockDatabase.getUserById(UserId("invalid username")))
        .thenReturn(Result(null, Database.GetUserByIdStatus.USER_ID_NOT_FOUND))
      val result = controller.login(UserPasswordCredential("invalid username", TEST_VALID_PASSWORD))

      assertThat(result.status).isEqualTo(SessionController.LoginStatus.BAD_USERNAME_OR_PASSWORD)
      assertThat(result.value).isNull()
    }

    @Test fun databaseError() {
      whenever(mockDatabase.getUserById(UserId("error")))
        .thenReturn(Result(null, Database.GetUserByIdStatus.DATABASE_ERROR))

      val result = controller.login(UserPasswordCredential("error", "password"))

      assertThat(result.status).isEqualTo(SessionController.LoginStatus.DATABASE_ERROR)
      assertThat(result.value).isNull()
    }
  }

  @Nested inner class LogoutTest {
    @Test fun default() {
      val activeSession =
        controller.login(UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))

      assertThat(controller.logout(activeSession.value!!.token)).isTrue()
    }

    @Test fun noActiveSession() {
      assertThat(controller.logout("not a valid token")).isFalse()
    }
  }

  @Nested inner class GetActiveSessionTest {
    @Test fun default() {
      val activeSession =
        controller.login(UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))

      val result = controller.getActiveSession(activeSession.value!!.token)

      assertThat(result.status).isEqualTo(SessionController.GetActiveSessionStatus.SUCCESS)
      assertThat(result.value).isEqualTo(activeSession.value)
    }

    @Test fun invalidToken() {
      val result = controller.getActiveSession("invalid token")

      assertThat(result.status)
        .isEqualTo(SessionController.GetActiveSessionStatus.SESSION_NOT_FOUND)
      assertThat(result.value).isNull()
    }

    @Test fun afterExpiration() {
      // Set active session
      val activeSession =
        controller.login(
          UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))

      // Advance past timeout
      fakeTicker.advance(TEST_SESSION_TIMEOUT_HOURS + 1, TimeUnit.HOURS)

      // Attempt to fetch previously active session
      val result = controller.getActiveSession(activeSession.value!!.token)

      // Verify it doesn't exist
      assertThat(result.status)
        .isEqualTo(SessionController.GetActiveSessionStatus.SESSION_NOT_FOUND)
      assertThat(result.value).isNull()
    }
  }

  @Test fun stopAllSessionsForUserId() {
    // Start multiple sessions
    val activeSessions = listOf(
      controller.login(UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD)),
      controller.login(UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD)),
      controller.login(UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))
    )

    // Plus an extra one
    val secondActiveSession =
      controller.login(UserPasswordCredential(TEST_VALID_USER_ID_2.username, TEST_VALID_PASSWORD))

    // Stop all sessions for the given user
    controller.stopAllSessionsForUserId(TEST_VALID_USER_ID)

    // Verify all sessions by TEST_VALID_USER_ID have been stopped
    for (activeSession in activeSessions) {
      val getActiveSessionResult = controller.getActiveSession(activeSession.value!!.token)
      assertThat(getActiveSessionResult.status)
        .isEqualTo(SessionController.GetActiveSessionStatus.SESSION_NOT_FOUND)
    }

    // Verify that the active sessions by other users still exist
    val getActiveSecondSession = controller.getActiveSession(secondActiveSession.value!!.token)
    assertThat(getActiveSecondSession.status)
      .isEqualTo(SessionController.GetActiveSessionStatus.SUCCESS)
  }

  @Nested inner class ReplaceSessionTest {
    @Test fun default() {
      // Prepare two sessions
      val activeSessionOne =
        controller.login(UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))
      val activeSessionTwo =
        controller.login(UserPasswordCredential(TEST_VALID_USER_ID_2.username, TEST_VALID_PASSWORD))

      // Perform a replacement on the first
      val resultStatus =
        controller.replaceSession(activeSessionOne.value!!.token, TEST_REPLACEMENT_USER_ID)

      // Obtain the resulting UserPrincipals of both sessions
      val resultSessionOneId = controller.getActiveSession(activeSessionOne.value!!.token)
      val resultSessionTwoId = controller.getActiveSession(activeSessionTwo.value!!.token)

      // Verify that the first session was replaced successfully
      assertThat(resultStatus.status).isEqualTo(SessionController.ReplaceSessionStatus.SUCCESS)
      assertThat(resultSessionOneId.value!!.userId).isEqualTo(TEST_REPLACEMENT_USER_ID)

      // And the second session was untouched
      assertThat(resultSessionTwoId.value!!.userId).isEqualTo(TEST_VALID_USER_ID_2)
    }

    @Test fun sessionNotFound() {
      // Perform a replacement on a session that doesn't exist
      val result =
        controller.replaceSession("random token that doesn't exist", TEST_REPLACEMENT_USER_ID)

      assertThat(result.status).isEqualTo(SessionController.ReplaceSessionStatus.TOKEN_NOT_FOUND)
    }
  }
}
