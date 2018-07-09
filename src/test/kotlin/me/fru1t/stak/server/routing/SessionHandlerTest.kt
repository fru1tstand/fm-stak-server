package me.fru1t.stak.server.routing

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserPasswordCredential
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.ktor.respondEmpty
import me.fru1t.stak.server.ktor.testing.addBearerAuthorizationHeader
import me.fru1t.stak.server.ktor.testing.handleFormRequest
import me.fru1t.stak.server.ktor.testing.setBody
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class SessionHandlerTest {
  companion object {
    private val TEST_VALID_USER_PRINCIPAL = UserPrincipal(UserId("username"), "sometoken")
    private val TEST_VALID_USER_PASSWORD_CREDENTIALS =
      UserPasswordCredential("test username", "test password")

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

  @BeforeEach fun setUp() {
    MockitoAnnotations.initMocks(this)

    sessionHandler = SessionHandler(mockSessionController)
  }

  @Nested inner class SessionRouting {
    @BeforeEach fun setUp() {
      // Set up valid login or active sessions for the session controller.
      whenever(mockSessionController.getActiveSession(TEST_VALID_USER_PRINCIPAL.token))
          .thenReturn(
              Result(TEST_VALID_USER_PRINCIPAL, SessionController.GetActiveSessionStatus.SUCCESS))
      whenever(mockSessionController.login(TEST_VALID_USER_PASSWORD_CREDENTIALS))
          .thenReturn(Result(TEST_VALID_USER_PRINCIPAL, SessionController.LoginStatus.SUCCESS))
    }

    @Test fun post_logsInUser() = withSessionHandler {
      val request = handleFormRequest(HttpMethod.Post, "/session") {
        setLoginCredentials(TEST_VALID_USER_PASSWORD_CREDENTIALS)
      }

      request.assertHandled(
          HttpStatusCode.OK, TEST_VALID_USER_PRINCIPAL.token, ContentType.Text.Plain)
    }

    @Test fun delete_logsOutUser() = withSessionHandler {
      val request = handleRequest(HttpMethod.Delete, "/session") {
        addBearerAuthorizationHeader(TEST_VALID_USER_PRINCIPAL.token)
      }

      request.assertEmpty(HttpStatusCode.OK)
      verify(mockSessionController).logout(TEST_VALID_USER_PRINCIPAL.token)
    }
  }

  @Test fun registerAuthentication_basic() = withSessionHandler {
    whenever(mockSessionController.login(TEST_VALID_USER_PASSWORD_CREDENTIALS))
        .thenReturn(Result(TEST_VALID_USER_PRINCIPAL, SessionController.LoginStatus.SUCCESS))

    val request = handleTestLoginRequest {
      setLoginCredentials(TEST_VALID_USER_PASSWORD_CREDENTIALS)
    }

    request.assertEmpty(HttpStatusCode.OK)
  }

  @Test fun registerAuthentication_basic_returnsUnauthorized_whenLoginCredentialsAreInvalid(
      // Empty param list
  ) = withSessionHandler {
    whenever(mockSessionController.login(any()))
        .thenReturn(Result(null, SessionController.LoginStatus.BAD_USERNAME_OR_PASSWORD))

    val request = handleTestLoginRequest {
      setLoginCredentials(TEST_VALID_USER_PASSWORD_CREDENTIALS)
    }

    assertThat(request.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
  }

  @Test fun registerAuthentication_bearer() = withSessionHandler {
    whenever(mockSessionController.getActiveSession(TEST_VALID_USER_PRINCIPAL.token))
        .thenReturn(
            Result(TEST_VALID_USER_PRINCIPAL, SessionController.GetActiveSessionStatus.SUCCESS))

    val request = handleTestRequest {
      addBearerAuthorizationHeader(TEST_VALID_USER_PRINCIPAL.token)
    }

    request.assertEmpty(HttpStatusCode.OK)
  }

  @Test fun registerAuthentication_bearer_returnsUnauthorized_whenNoSessionFound(
      // Empty parameter list
  ) = withSessionHandler {
    whenever(mockSessionController.getActiveSession(any()))
        .thenReturn(Result(null, SessionController.GetActiveSessionStatus.SESSION_NOT_FOUND))

    val request = handleTestRequest {
      addBearerAuthorizationHeader(TEST_VALID_USER_PRINCIPAL.token)
    }

    request.assertEmpty(HttpStatusCode.Unauthorized)
  }

  @Test fun login() = withTestApplication {
    // Set up routing to call login
    application.routing {
      get("test") {
        call.authentication.principal(TEST_VALID_USER_PRINCIPAL)
        sessionHandler.login(call)
      }
    }

    val result = handleRequest(HttpMethod.Get, "/test")

    result.assertHandled(HttpStatusCode.OK, TEST_VALID_USER_PRINCIPAL.token, ContentType.Text.Plain)
  }

  @Test fun logout() = withTestApplication {
    // Set up routing to call log out
    application.routing {
      get("test") {
        call.authentication.principal(TEST_VALID_USER_PRINCIPAL)
        sessionHandler.logout(call)
      }
    }

    val result = handleRequest(HttpMethod.Get, "/test")

    result.assertEmpty(HttpStatusCode.OK)
    verify(mockSessionController).logout(TEST_VALID_USER_PRINCIPAL.token)
  }

  /**
   * Sets up authentication with [sessionHandler]'s [SessionHandler.registerAuthentication], routes
   * [session] routes passing in [sessionHandler], and sets up a test [Constants.SESSION_AUTH_NAME]
   * authentication route as a [HttpMethod.Get].
   */
  private fun withSessionHandler(test: TestApplicationEngine.() -> Unit) = withTestApplication {
    application.install(Authentication) { sessionHandler.registerAuthentication(this) }
    application.routing {
      session(sessionHandler)
      authenticate(Constants.SESSION_AUTH_NAME) {
        get("test") { call.respondEmpty() }
      }
      authenticate(Constants.LOGIN_AUTH_NAME) {
        post("test") { call.respondEmpty() }
      }
    }
    test()
  }

  /**
   * Sets the login credentials for this request by setting the body contents of the request as
   * a query string. One must create the request via [handleFormRequest] in order for this to do
   * anything.
   */
  private fun TestApplicationRequest.setLoginCredentials(credentials: UserPasswordCredential) {
    setBody {
      addParameter(Constants.LOGIN_USER_PARAM_NAME, credentials.name)
      addParameter(Constants.LOGIN_PASS_PARAM_NAME, credentials.password)
    }
  }

  /**
   * Performs an [HttpMethod.Get] request at `/test` to hit the bearer authentication test route
   * set up by [withSessionHandler].
   */
  private fun TestApplicationEngine.handleTestRequest(setup: TestApplicationRequest.() -> Unit) =
    handleRequest(HttpMethod.Get, "/test", setup)

  /**
   * Performs a multi-part form [HttpMethod.Post] request at `/test` to hit the form authentication
   * test route set up by [withSessionHandler].
   */
  private fun TestApplicationEngine.handleTestLoginRequest(
      setup: TestApplicationRequest.() -> Unit) = handleFormRequest(HttpMethod.Post, "/test", setup)
}
