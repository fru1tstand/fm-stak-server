package me.fru1t.stak.server.routing

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
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
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.ktor.testing.addBearerAuthorizationHeader
import me.fru1t.stak.server.ktor.testing.handleJsonRequest
import me.fru1t.stak.server.ktor.testing.installFakeJsonContentNegotiation
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserPrincipal
import me.fru1t.stak.server.routing.testing.FakeSessionHandler
import me.fru1t.stak.server.routing.testing.installFakeAuthentication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class UserHandlerTest {
  private companion object {
    private val TEST_GSON = Gson()
    private const val TEST_USER_PATH = "/user"

    private val TEST_USER = User(UserId("test username"), "test hash", "test display name")
    private val TEST_USER_PRINCIPAL = UserPrincipal(UserId(TEST_USER.userId.username), "test token")
    private val TEST_USER_CREATE =
      UserCreate("test create username", "test create password", "test create display name")

    /** Asserts that this [TestApplicationCall] matches all given constraints. */
    private fun TestApplicationCall.assertHandled(
        content: String, status: HttpStatusCode, contentType: ContentType) {
      assertThat(this.response.status()).isEqualTo(status)
      assertThat(this.response.content).isEqualTo(content)
      assertThat(this.requestHandled).isTrue()
      assertThat(this.response.contentType().toString()).contains(contentType.toString())
    }

    /** Asserts that this [TestApplicationCall] returned with [status] and an empty body. */
    private fun TestApplicationCall.assertEmpty(status: HttpStatusCode) {
      assertHandled("", status, ContentType.Text.Plain)
    }
  }

  @Mock private lateinit var mockUserController: UserController
  @Mock private lateinit var mockSessionController: SessionController
  private lateinit var userHandler: UserHandler
  private lateinit var fakeSessionHandler: FakeSessionHandler

  @BeforeEach fun setUp() {
    MockitoAnnotations.initMocks(this)

    fakeSessionHandler = FakeSessionHandler()
    userHandler = UserHandler(mockUserController, mockSessionController)
  }

  @Nested inner class CreateUser {
    @BeforeEach fun setUpCreateUser() {
      whenever(mockUserController.createUser(any()))
          .thenReturn(Result(TEST_USER, UserController.CreateUserStatus.SUCCESS))
    }

    @Test fun default() = withUserHandler {
      whenever(mockSessionController.login(any()))
          .thenReturn(Result(TEST_USER_PRINCIPAL, SessionController.LoginStatus.SUCCESS))

      val result = handleCreateUserRequest()

      result.assertHandled(
          content = TEST_USER_PRINCIPAL.token,
          status = HttpStatusCode.Created,
          contentType = ContentType.Text.Plain)
    }

    @Test fun badRequest() = withUserHandler {
      val result =
        handleJsonRequest("a bad {{{ request", TEST_GSON, HttpMethod.Post, TEST_USER_PATH) {}

      result.assertEmpty(HttpStatusCode.BadRequest)
    }

    @Test fun conflict() = withUserHandler {
      whenever(mockUserController.createUser(any()))
          .thenReturn(Result(null, UserController.CreateUserStatus.USER_ID_ALREADY_EXISTS))

      handleCreateUserRequest().assertEmpty(HttpStatusCode.Conflict)
    }

    @Test fun internalServerError() = withUserHandler {
      whenever(mockUserController.createUser(any()))
          .thenReturn(Result(null, UserController.CreateUserStatus.DATABASE_ERROR))

      handleCreateUserRequest().assertEmpty(HttpStatusCode.InternalServerError)
    }

    @Test fun serviceUnavailable() = withUserHandler {
      whenever(mockSessionController.login(any()))
          .thenReturn(Result(null, SessionController.LoginStatus.DATABASE_ERROR))

      handleCreateUserRequest().assertEmpty(HttpStatusCode.ServiceUnavailable)
    }

    @Test fun resetContent() = withUserHandler {
      whenever(mockSessionController.login(any()))
          .thenReturn(Result(null, SessionController.LoginStatus.BAD_USERNAME_OR_PASSWORD))

      handleCreateUserRequest().assertEmpty(HttpStatusCode.ResetContent)
    }

    /** Posts to `/user` passing in [TEST_USER_CREATE] in the body. */
    private fun TestApplicationEngine.handleCreateUserRequest() =
      handleJsonRequest(TEST_USER_CREATE, TEST_GSON, HttpMethod.Post, TEST_USER_PATH) {}
  }

  @Nested inner class DeleteMe {
    private lateinit var testAuthedUserPrincipal: UserPrincipal

    @BeforeEach fun setUpDeleteMe() {
      testAuthedUserPrincipal = fakeSessionHandler.addActiveSession(TEST_USER)
    }

    @Test fun success_noContent() = withUserHandler {
      whenever(mockUserController.deleteUser(testAuthedUserPrincipal.userId))
          .thenReturn(Status(UserController.DeleteUserStatus.SUCCESS))

      val request = handleDeleteMeRequest()

      request.assertEmpty(HttpStatusCode.NoContent)
    }

    @Test fun notFound() = withUserHandler {
      whenever(mockUserController.deleteUser(testAuthedUserPrincipal.userId))
          .thenReturn(Status(UserController.DeleteUserStatus.USER_ID_NOT_FOUND))

      val request = handleDeleteMeRequest()

      request.assertEmpty(HttpStatusCode.NotFound)
    }

    @Test fun internalServerError_databaseErrorOnSessionDelete() = withUserHandler {
      whenever(mockUserController.deleteUser(testAuthedUserPrincipal.userId))
          .thenReturn(Status(UserController.DeleteUserStatus.DATABASE_ERROR_ON_SESSION_DELETE))

      val request = handleDeleteMeRequest()

      request.assertEmpty(HttpStatusCode.InternalServerError)
    }

    @Test fun internalServerError_databaseError() = withUserHandler {
      whenever(mockUserController.deleteUser(testAuthedUserPrincipal.userId))
          .thenReturn(Status(UserController.DeleteUserStatus.DATABASE_ERROR))

      val request = handleDeleteMeRequest()

      request.assertEmpty(HttpStatusCode.InternalServerError)
    }

    private fun TestApplicationEngine.handleDeleteMeRequest() =
      handleRequest(HttpMethod.Delete, "/user/me") {
        addBearerAuthorizationHeader(testAuthedUserPrincipal.token)
      }
  }

  /** Extend this to automatically set up [UserHandler] within the test application. */
  private fun withUserHandler(test: TestApplicationEngine.() -> Unit) = withTestApplication {
    installFakeJsonContentNegotiation()
    installFakeAuthentication(fakeSessionHandler)
    application.routing { user(userHandler) }
    test()
  }
}
