package me.fru1t.stak.server.routing.testing

import com.google.common.truth.Truth.assertThat
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.ktor.getSessionUserPrincipal
import me.fru1t.stak.server.ktor.testing.addBearerAuthorizationHeader
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FakeSessionHandlerTest {
  companion object {
    private val TEST_USER = User(UserId("test username"), "test pw hash", "test display name")
  }

  private lateinit var fakeSessionHandler: FakeSessionHandler

  @BeforeEach fun setUp() {
    fakeSessionHandler = FakeSessionHandler()
  }

  @Test fun valid() = withTestApplication {
    installFakeAuthentication(fakeSessionHandler)
    installTestRoute()

    val principal = fakeSessionHandler.addActiveSession(TEST_USER)
    val result = handleFakeRouteRequest {
      addBearerAuthorizationHeader(principal.token)
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.OK)
    assertThat(result.requestHandled).isTrue()
    assertThat(result.response.content).isEqualTo(TEST_USER.userId.username)
  }

  @Test fun unauthorized_whenTokenDoesNotExist() = withTestApplication {
    installFakeAuthentication(fakeSessionHandler)
    installTestRoute()

    fakeSessionHandler.addActiveSession(TEST_USER)
    val result = handleFakeRouteRequest {
      addBearerAuthorizationHeader("clearly-fake-token")
    }

    assertThat(result.response.status()).isEqualTo(HttpStatusCode.Unauthorized)
    assertThat(result.requestHandled).isTrue()
    assertThat(result.response.content).isEmpty()
  }

  /**
   * Sets up an [HttpMethod.Get] route at `/test` which is an authenticated-only route that returns
   * the user's username as the body.
   */
  private fun TestApplicationEngine.installTestRoute() {
    application.install(Routing) {
      authenticate(Constants.SESSION_AUTH_NAME) {
        get("test") {
          call.respondText(
              call.getSessionUserPrincipal().userId.username,
              ContentType.Text.Plain,
              HttpStatusCode.OK)
        }
      }
    }
  }

  /** Performs a [handleRequest] [HttpMethod.Get] request at `/test`. */
  private fun TestApplicationEngine.handleFakeRouteRequest(
      setup: TestApplicationRequest.() -> Unit) = handleRequest(HttpMethod.Get, "/test", setup)
}
