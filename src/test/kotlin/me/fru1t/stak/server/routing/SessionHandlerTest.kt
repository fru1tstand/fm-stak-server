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
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.ktor.testing.addBasicAuthorizationHeader
import me.fru1t.stak.server.ktor.testing.handleFormRequest
import me.fru1t.stak.server.ktor.testing.setBody
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SessionHandlerTest {
  companion object {
    private val TEST_VALID_USER_PRINCIPAL_RESULT = Result(UserPrincipal("username", "sometoken"))
  }

  @Mock private lateinit var mockSessionController: SessionController
  private lateinit var sessionHandler: SessionHandler

  @BeforeEach internal fun setUp() {
    MockitoAnnotations.initMocks(this)

    sessionHandler = SessionHandler(mockSessionController)
  }

  @Test fun registerAuthentication_form_invalidUserParamValue() = withSessionHandler {
    whenever(mockSessionController.getActiveSession(any()))
        .thenReturn(TEST_VALID_USER_PRINCIPAL_RESULT)

    val result = handleFormRequest(HttpMethod.Delete, "/session") {
      setBody {
        addParameter(SessionHandler.SESSION_USER_PARAM_NAME, "invalid")
        addParameter(
            SessionHandler.SESSION_PASS_PARAM_NAME, TEST_VALID_USER_PRINCIPAL_RESULT.value!!.token)
      }
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun registerAuthentication_form_invalidActiveSession() = withSessionHandler {
    whenever(mockSessionController.getActiveSession(any()))
        .thenReturn(Result(httpStatusCode = HttpStatusCode.Unauthorized))

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
    whenever(mockSessionController.login(any())).thenReturn(TEST_VALID_USER_PRINCIPAL_RESULT)

    val result = handleRequest(HttpMethod.Post, "/session") {
      addBasicAuthorizationHeader("test username", "test password")
    }

    assertThat(result.response.content).isEqualTo(TEST_VALID_USER_PRINCIPAL_RESULT.value!!.token)
    assertThat(result.response.status()).isEqualTo(HttpStatusCode.OK)
  }

  @Test fun login_invalidCredentials() = withSessionHandler {
    whenever(mockSessionController.login(any()))
        .thenReturn(Result(httpStatusCode = HttpStatusCode.Unauthorized))

    val result = handleFormRequest(HttpMethod.Post, "/session") {
      addBasicAuthorizationHeader("test username", "test password")
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun logout() = withSessionHandler {
    whenever(mockSessionController.getActiveSession(any()))
        .thenReturn(TEST_VALID_USER_PRINCIPAL_RESULT)

    val result = handleFormRequest(HttpMethod.Delete, "/session") {
      addBasicAuthorizationHeader("test username", "test password")
      setBody {
        addParameter(
            SessionHandler.SESSION_USER_PARAM_NAME, SessionHandler.SESSION_USER_PARAM_VALUE)
        addParameter(
            SessionHandler.SESSION_PASS_PARAM_NAME, TEST_VALID_USER_PRINCIPAL_RESULT.value!!.token)
      }
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.OK)
  }

  /** Extend this to automatically set up [SessionHandler] within the test application. */
  private fun withSessionHandler(test: TestApplicationEngine.() -> Unit) = withTestApplication {
    application.install(Authentication) { sessionHandler.registerAuthentication(this) }
    application.routing { session(sessionHandler) }
    test()
  }
}
