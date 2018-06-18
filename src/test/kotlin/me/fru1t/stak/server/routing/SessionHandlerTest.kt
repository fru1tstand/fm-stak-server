package me.fru1t.stak.server.routing

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.ktor.testing.addBasicAuthorizationHeader
import me.fru1t.stak.server.ktor.testing.handleFormRequest
import me.fru1t.stak.server.ktor.testing.setBody
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SessionHandlerTest {
  companion object {
    private val TEST_VALID_USER_PRINCIPAL = UserPrincipal(UserId("username"), "sometoken")

    /**
     * Asserts that a call has the given [status], [content], and [contentType], and that it's
     * been handled.
     */
    private fun TestApplicationCall.assertHandled(
        status: HttpStatusCode, content: String, contentType: ContentType) {
      assertThat(this.requestHandled).isTrue()
      assertThat(this.response.status()).isEqualTo(status)
      assertThat(this.response.content).isEqualTo(content)
      assertThat(this.response.contentType().toString()).contains(contentType.toString())
    }

    /** Asserts that a call has an empty response and the given [status]. */
    private fun TestApplicationCall.assertEmpty(status: HttpStatusCode) {
      assertHandled(status, "", ContentType.Text.Plain)
    }
  }

  @Mock private lateinit var mockSessionController: SessionController
  private lateinit var sessionHandler: SessionHandler

  @BeforeEach internal fun setUp() {
    MockitoAnnotations.initMocks(this)

    whenever(mockSessionController.getActiveSession(any()))
        .thenReturn(
            Result(TEST_VALID_USER_PRINCIPAL, SessionController.GetActiveSessionStatus.SUCCESS))
    whenever(mockSessionController.login(any()))
        .thenReturn(Result(TEST_VALID_USER_PRINCIPAL, SessionController.LoginStatus.SUCCESS))

    sessionHandler = SessionHandler(mockSessionController)
  }

  @Test fun registerAuthentication_form_invalidUserParamValue() = withSessionHandler {
    val result = handleFormRequest(HttpMethod.Delete, "/session") {
      setBody {
        addParameter(SessionHandler.SESSION_USER_PARAM_NAME, "invalid")
        addParameter(SessionHandler.SESSION_PASS_PARAM_NAME, TEST_VALID_USER_PRINCIPAL.token)
      }
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun registerAuthentication_form_invalidActiveSession() = withSessionHandler {
    whenever(mockSessionController.getActiveSession(any()))
        .thenReturn(Result(null, SessionController.GetActiveSessionStatus.SESSION_NOT_FOUND))

    val result = handleFormRequest(HttpMethod.Delete, "/session") {
      setBody {
        addParameter(
            SessionHandler.SESSION_USER_PARAM_NAME, SessionHandler.SESSION_USER_PARAM_VALUE)
        addParameter(SessionHandler.SESSION_PASS_PARAM_NAME, "invalid")
      }
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun login() = withSessionHandler {
    val result = handleRequest(HttpMethod.Post, "/session") {
      addBasicAuthorizationHeader("test username", "test password")
    }

    result.assertHandled(HttpStatusCode.OK, TEST_VALID_USER_PRINCIPAL.token, ContentType.Text.Plain)
  }

  @Test fun login_invalidCredentials() = withSessionHandler {
    whenever(mockSessionController.login(any()))
        .thenReturn(Result(null, SessionController.LoginStatus.BAD_USERNAME_OR_PASSWORD))

    val result = handleFormRequest(HttpMethod.Post, "/session") {
      addBasicAuthorizationHeader("test username", "test password")
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun logout() = withSessionHandler {
    val result = handleFormRequest(HttpMethod.Delete, "/session") {
      addBasicAuthorizationHeader("test username", "test password")
      setBody {
        addParameter(
            SessionHandler.SESSION_USER_PARAM_NAME, SessionHandler.SESSION_USER_PARAM_VALUE)
        addParameter(SessionHandler.SESSION_PASS_PARAM_NAME, TEST_VALID_USER_PRINCIPAL.token)
      }
    }

    result.assertEmpty(HttpStatusCode.OK)
  }

  /** Extend this to automatically set up [SessionHandler] within the test application. */
  private fun withSessionHandler(test: TestApplicationEngine.() -> Unit) = withTestApplication {
    application.install(Authentication) { sessionHandler.registerAuthentication(this) }
    application.routing { session(sessionHandler) }
    test()
  }
}
