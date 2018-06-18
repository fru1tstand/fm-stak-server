package me.fru1t.stak.server.routing

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.contentType
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.components.session.SessionController.LoginStatus
import me.fru1t.stak.server.components.user.UserController
import me.fru1t.stak.server.components.user.UserController.CreateUserStatus
import me.fru1t.stak.server.ktor.testing.handleJsonRequest
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserCreate
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class UserHandlerTest {
  companion object {
    private val TEST_GSON = Gson()
    private const val TEST_USER_PATH = "/user"

    private val TEST_USER = User(UserId("test username"), "test hash", "test display name")
    private val TEST_USER_PRINCIPAL = UserPrincipal(UserId(TEST_USER.userId.username), "test token")
    private val TEST_USER_CREATE =
      UserCreate("test create username", "test create password", "test create display name")

    /** Asserts that this [TestApplicationCall] matches all given constraints. */
    private fun TestApplicationCall.assert(
        content: String, status: HttpStatusCode, contentType: ContentType) {
      assertThat(this.requestHandled).isTrue()
      assertThat(this.response.status()).isEqualTo(status)
      assertThat(this.response.contentType().contentType).contains(contentType.contentType)
      assertThat(this.response.content).isEqualTo(content)
    }

    /** Asserts that this [TestApplicationCall] returned with [status] and an empty body. */
    private fun TestApplicationCall.assertEmptyContent(status: HttpStatusCode) {
      assert("", status, ContentType.Text.Plain)
    }

    /** Posts to `/user` passing in [TEST_USER_CREATE] in the body. */
    private fun TestApplicationEngine.handleCreateUserRequest() =
        handleJsonRequest(TEST_USER_CREATE, TEST_GSON, HttpMethod.Post, TEST_USER_PATH, {})
  }
  @Mock private lateinit var mockUserController: UserController
  @Mock private lateinit var mockSessionController: SessionController
  private lateinit var userHandler: UserHandler

  @BeforeEach fun setUp() {
    MockitoAnnotations.initMocks(this)

    whenever(mockUserController.createUser(any()))
        .thenReturn(Result(TEST_USER, CreateUserStatus.SUCCESS))
    whenever(mockSessionController.login(any()))
        .thenReturn(Result(TEST_USER_PRINCIPAL, LoginStatus.SUCCESS))

    userHandler = UserHandler(mockUserController, mockSessionController)
  }

  @Test fun createUser() = testWithUserHandler {
    val result = handleCreateUserRequest()

    result.assert(
        content = TEST_USER_PRINCIPAL.token,
        status = HttpStatusCode.Created,
        contentType = ContentType.Text.Plain)
  }

  @Test fun createUser_badRequest() = testWithUserHandler {
    val result =
      handleJsonRequest("a bad {{{ request", TEST_GSON, HttpMethod.Post, TEST_USER_PATH, {})

    result.assertEmptyContent(HttpStatusCode.BadRequest)
  }

  @Test fun createUser_conflict() = testWithUserHandler {
    whenever(mockUserController.createUser(any()))
        .thenReturn(Result(null, CreateUserStatus.USERNAME_ALREADY_EXISTS))

    handleCreateUserRequest().assertEmptyContent(HttpStatusCode.Conflict)
  }

  @Test fun createUser_internalServerError() = testWithUserHandler {
    whenever(mockUserController.createUser(any()))
        .thenReturn(Result(null, CreateUserStatus.DATABASE_ERROR))

    handleCreateUserRequest().assertEmptyContent(HttpStatusCode.InternalServerError)
  }

  @Test fun createUser_serviceUnavailable() = testWithUserHandler {
    whenever(mockSessionController.login(any()))
        .thenReturn(Result(null, LoginStatus.DATABASE_ERROR))

    handleCreateUserRequest().assertEmptyContent(HttpStatusCode.ServiceUnavailable)
  }

  @Test fun createUser_resetContent() = testWithUserHandler {
    whenever(mockSessionController.login(any()))
        .thenReturn(Result(null, LoginStatus.BAD_USERNAME_OR_PASSWORD))

    handleCreateUserRequest().assertEmptyContent(HttpStatusCode.ResetContent)
  }

  /** Extend this to automatically set up [UserHandler] within the test application. */
  private fun testWithUserHandler(test: TestApplicationEngine.() -> Unit) = withTestApplication {
    application.install(ContentNegotiation) { gson { setPrettyPrinting() } }
    application.routing { user(userHandler) }
    test()
  }
}
