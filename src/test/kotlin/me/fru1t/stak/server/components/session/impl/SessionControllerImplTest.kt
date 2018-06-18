package me.fru1t.stak.server.components.session.impl

import com.google.common.testing.FakeTicker
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.whenever
import io.ktor.auth.UserPasswordCredential
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.DatabaseResult
import me.fru1t.stak.server.components.security.impl.FakeSecurity
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class SessionControllerImplTest {
  companion object {
    private val TEST_VALID_USER_ID = UserId("test-valid-username")
    private const val TEST_VALID_PASSWORD = "test-valid-password"
    private const val TEST_SESSION_TIMEOUT_HOURS = 1L
  }

  @Mock private lateinit var mockDatabase: Database
  private lateinit var fakeSecurity: FakeSecurity
  private lateinit var fakeTicker: FakeTicker
  private lateinit var sessionControllerImpl: SessionControllerImpl

  private lateinit var testValidPasswordHash: String

  @BeforeEach internal fun setUp() {
    MockitoAnnotations.initMocks(this)

    fakeTicker = FakeTicker()
    fakeSecurity = FakeSecurity()

    testValidPasswordHash = fakeSecurity.hash(TEST_VALID_PASSWORD)
    whenever(mockDatabase.getUserById(TEST_VALID_USER_ID))
        .thenReturn(
            DatabaseResult(
                result = User(
                    userId = TEST_VALID_USER_ID,
                    passwordHash = testValidPasswordHash,
                    displayName = "Test Name")))

    sessionControllerImpl =
        SessionControllerImpl(mockDatabase, TEST_SESSION_TIMEOUT_HOURS, fakeSecurity, fakeTicker)
  }

  @Test fun login() {
    val result =
      sessionControllerImpl.login(
          UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))

    assertThat(result.status).isEqualTo(SessionController.LoginStatus.SUCCESS)
    assertThat(result.value).isNotNull()
    assertThat(result.value!!.userId).isEqualTo(TEST_VALID_USER_ID)
  }

  @Test fun login_invalidPassword() {
    val result =
      sessionControllerImpl.login(
          UserPasswordCredential(TEST_VALID_USER_ID.username, "invalid pass"))

    assertThat(result.status).isEqualTo(SessionController.LoginStatus.BAD_USERNAME_OR_PASSWORD)
    assertThat(result.value).isNull()
  }

  @Test fun login_invalidUsername() {
    whenever(mockDatabase.getUserById(UserId("invalid username")))
        .thenReturn(DatabaseResult(error = "User not found"))
    val result =
      sessionControllerImpl.login(UserPasswordCredential("invalid username", TEST_VALID_PASSWORD))

    assertThat(result.status).isEqualTo(SessionController.LoginStatus.BAD_USERNAME_OR_PASSWORD)
    assertThat(result.value).isNull()
  }

  @Test fun login_databaseError() {
    whenever(mockDatabase.getUserById(UserId("error")))
        .thenReturn(DatabaseResult(error = "Some error", isDatabaseError = true))

    val result = sessionControllerImpl.login(UserPasswordCredential("error", "password"))

    assertThat(result.status).isEqualTo(SessionController.LoginStatus.DATABASE_ERROR)
    assertThat(result.value).isNull()
  }

  @Test fun logout() {
    val activeSession =
      sessionControllerImpl.login(
          UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))

    assertThat(sessionControllerImpl.logout(activeSession.value!!.token)).isTrue()
  }

  @Test fun logout_noActiveSession() {
    assertThat(sessionControllerImpl.logout("not a valid token")).isFalse()
  }

  @Test fun getActiveSession() {
    val activeSession =
      sessionControllerImpl.login(
          UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))

    val result = sessionControllerImpl.getActiveSession(activeSession.value!!.token)

    assertThat(result.status).isEqualTo(SessionController.GetActiveSessionStatus.SUCCESS)
    assertThat(result.value).isEqualTo(activeSession.value)
  }

  @Test fun getActiveSession_invalidToken() {
    val result = sessionControllerImpl.getActiveSession("invalid token")

    assertThat(result.status).isEqualTo(SessionController.GetActiveSessionStatus.SESSION_NOT_FOUND)
    assertThat(result.value).isNull()
  }

  @Test fun getActiveSession_afterExpiration() {
    // Set active session
    val activeSession =
      sessionControllerImpl.login(
          UserPasswordCredential(TEST_VALID_USER_ID.username, TEST_VALID_PASSWORD))

    // Advance past timeout
    fakeTicker.advance(TEST_SESSION_TIMEOUT_HOURS + 1, TimeUnit.HOURS)

    // Attempt to fetch previously active session
    val result = sessionControllerImpl.getActiveSession(activeSession.value!!.token)

    // Verify it doesn't exist
    assertThat(result.status).isEqualTo(SessionController.GetActiveSessionStatus.SESSION_NOT_FOUND)
    assertThat(result.value).isNull()
  }
}
