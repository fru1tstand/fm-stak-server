package me.fru1t.stak.server.routing

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.components.session.Session
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SessionHandlerTest {
  companion object {
    private val TEST_VALID_USER_PRINCIPAL_RESULT = Result(UserPrincipal("username", "sometoken"))

    private const val AUTHORIZATION_HEADER = "Authorization"
    private const val AUTHORIZATION_HEADER_VALUE = "Basic dGVzdDp0ZXN0"

    private const val CONTENT_TYPE_HEADER = "content-type"
    private const val CONTENT_TYPE_HEADER_VALUE = "application/x-www-form-urlencoded"
  }
  @Mock private lateinit var mockSession: Session
  private lateinit var sessionHandler: SessionHandler

  @BeforeEach internal fun setUp() {
    MockitoAnnotations.initMocks(this)

    sessionHandler = SessionHandler(mockSession)
  }

  @Test fun registerAuthentication_form_invalidUserParamValue() = withSessionHandler {
    whenever(mockSession.getActiveSession(any())).thenReturn(TEST_VALID_USER_PRINCIPAL_RESULT)

    val result = handleRequest(HttpMethod.Delete, "/session") {
      addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE)
      setBody(
          "${SessionHandler.SESSION_USER_PARAM_NAME}=invalid" +
              "&${SessionHandler.SESSION_PASS_PARAM_NAME}=" +
              TEST_VALID_USER_PRINCIPAL_RESULT.value!!.token)
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun registerAuthentication_form_invalidActiveSession() = withSessionHandler {
    whenever(mockSession.getActiveSession(any())).thenReturn(Result(error = "error!"))

    val result = handleRequest(HttpMethod.Delete, "/session") {
      addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE)
      setBody(
          "${SessionHandler.SESSION_USER_PARAM_NAME}=${SessionHandler.SESSION_USER_PARAM_VALUE}" +
              "&${SessionHandler.SESSION_PASS_PARAM_NAME}=invalid")
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun login() = withSessionHandler {
    whenever(mockSession.login(any())).thenReturn(TEST_VALID_USER_PRINCIPAL_RESULT)

    val result = handleRequest(HttpMethod.Post, "/session") {
      addHeader(AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_VALUE)
    }

    assertThat(result.response.content).isEqualTo(TEST_VALID_USER_PRINCIPAL_RESULT.value!!.token)
    assertThat(result.response.status()).isEqualTo(HttpStatusCode.OK)
  }

  @Test fun login_invalidCredentials() = withSessionHandler {
    whenever(mockSession.login(any())).thenReturn(Result(error = "error!"))

    val result = handleRequest(HttpMethod.Post, "/session") {
      addHeader(AUTHORIZATION_HEADER, AUTHORIZATION_HEADER_VALUE)
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun logout() = withSessionHandler {
    whenever(mockSession.getActiveSession(any())).thenReturn(TEST_VALID_USER_PRINCIPAL_RESULT)

    val result = handleRequest(HttpMethod.Delete, "/session") {
      addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE)
      setBody(
          "${SessionHandler.SESSION_USER_PARAM_NAME}=${SessionHandler.SESSION_USER_PARAM_VALUE}" +
              "&${SessionHandler.SESSION_PASS_PARAM_NAME}=" +
              TEST_VALID_USER_PRINCIPAL_RESULT.value!!.token)
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.OK)
  }

  /** Extend this to automatically set up [SessionHandler] within the test application. */
  private fun withSessionHandler(
      test: TestApplicationEngine.() -> Unit) = withTestApplication {
    application.install(Authentication) {
      sessionHandler.registerAuthentication(this)
    }
    application.routing {
      session(sessionHandler)
    }
    test.invoke(this)
  }
}