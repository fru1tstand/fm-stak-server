package me.fru1t.stak.server.components.session.impl

import com.google.common.testing.FakeTicker
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.whenever
import io.ktor.auth.UserPasswordCredential
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.DatabaseResult
import me.fru1t.stak.server.components.security.impl.FakeSecurity
import me.fru1t.stak.server.models.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit

class SessionControllerImplTest {
  companion object {
    private const val TEST_VALID_USERNAME = "test-valid-username"
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
    whenever(mockDatabase.getUserByUsername(TEST_VALID_USERNAME))
        .thenReturn(
            DatabaseResult(
                result = User(
                    username = TEST_VALID_USERNAME,
                    passwordHash = testValidPasswordHash,
                    displayName = "Test Name")))

    sessionControllerImpl =
        SessionControllerImpl(mockDatabase, TEST_SESSION_TIMEOUT_HOURS, fakeSecurity, fakeTicker)
  }

  @Test fun login() {
    val result =
      sessionControllerImpl.login(UserPasswordCredential(TEST_VALID_USERNAME, TEST_VALID_PASSWORD))

    assertThat(result.httpStatusCode.isSuccess()).isTrue()
    assertThat(result.value!!.username).isEqualTo(TEST_VALID_USERNAME)
    assertThat(result.value!!.token).hasLength(128)
  }

  @Test fun login_invalidPassword() {
    val result =
      sessionControllerImpl.login(UserPasswordCredential(TEST_VALID_USERNAME, "invalid pass"))

    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(result.value).isNull()
  }

  @Test fun login_invalidUsername() {
    whenever(mockDatabase.getUserByUsername("invalid username"))
        .thenReturn(DatabaseResult(error = "User not found"))
    val result =
      sessionControllerImpl.login(UserPasswordCredential("invalid username", TEST_VALID_PASSWORD))

    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(result.value).isNull()
  }

  @Test fun login_databaseError() {
    whenever(mockDatabase.getUserByUsername("error"))
        .thenReturn(DatabaseResult(error = "Some error", isDatabaseError = true))

    val result = sessionControllerImpl.login(UserPasswordCredential("error", "password"))

    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.InternalServerError)
    assertThat(result.value).isNull()
  }

  @Test fun logout() {
    val activeSession =
      sessionControllerImpl.login(UserPasswordCredential(TEST_VALID_USERNAME, TEST_VALID_PASSWORD))

    assertThat(sessionControllerImpl.logout(activeSession.value!!.token)).isTrue()
  }

  @Test fun logout_noActiveSession() {
    assertThat(sessionControllerImpl.logout("not a valid token")).isFalse()
  }

  @Test fun getActiveSession() {
    val activeSession =
      sessionControllerImpl.login(UserPasswordCredential(TEST_VALID_USERNAME, TEST_VALID_PASSWORD))

    val result = sessionControllerImpl.getActiveSession(activeSession.value!!.token)

    assertThat(result.httpStatusCode.isSuccess()).isTrue()
    assertThat(result.value).isEqualTo(activeSession.value)
  }

  @Test fun getActiveSession_invalidToken() {
    val result = sessionControllerImpl.getActiveSession("invalid token")

    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(result.value).isNull()
  }

  @Test fun getActiveSession_afterExpiration() {
    // Set active session
    val activeSession =
      sessionControllerImpl.login(UserPasswordCredential(TEST_VALID_USERNAME, TEST_VALID_PASSWORD))

    // Advance past timeout
    fakeTicker.advance(TEST_SESSION_TIMEOUT_HOURS + 1, TimeUnit.HOURS)

    // Attempt to fetch previously active session
    val result = sessionControllerImpl.getActiveSession(activeSession.value!!.token)

    // Verify it doesn't exist
    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(result.value).isNull()
  }
}
