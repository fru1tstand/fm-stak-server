package me.fru1t.stak.server.ktor.auth

import com.google.common.truth.Truth.assertThat
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserPrincipal
import org.junit.jupiter.api.Test

class BearerAuthenticationProviderTest {
  companion object {
    private const val TEST_VALID_TOKEN = "token"
    private const val TEST_ROUTE = "test"

    private val TEST_USER_PRINCIPAL = UserPrincipal(UserId("test username"), "test token")

    /**
     * Sets up a test application with a bearer authentication and a [HttpMethod.Get] route at
     * [TEST_ROUTE]. Authentication will provide [TEST_USER_PRINCIPAL] if the passed bearer token
     * matches [TEST_VALID_TOKEN]; otherwise, it will not validate.
     */
    private fun withBearerAuth(test: TestApplicationEngine.() -> Unit) = withTestApplication {
      // Set up auth, responding with the responsePrincipal
      application.install(Authentication) {
        bearer {
          validate { token -> if (token == TEST_VALID_TOKEN) TEST_USER_PRINCIPAL else null }
        }
      }

      // Set up routing with a single authenticated route that responds with the user principal
      // token, or an empty string if not available
      application.install(Routing) {
        authenticate {
          get(TEST_ROUTE) {
            call.respondText(
                call.authentication.principal?.let { (it as UserPrincipal).token } ?: "",
                ContentType.Text.Plain,
                HttpStatusCode.OK)
          }
        }
      }

      // Then run the tests
      test()
    }

    /** Perform a [HttpMethod.Get] request to [TEST_ROUTE]. */
    private fun TestApplicationEngine.handleTestRequest(setup: TestApplicationRequest.() -> Unit) =
      handleRequest(HttpMethod.Get, TEST_ROUTE, setup)

    /**
     * Asserts that the request was handled and that the response gave the status code [status]
     * with [content].
     */
    private fun TestApplicationCall.assertHandled(status: HttpStatusCode, content: String?) {
      assertThat(this.response.status()).isEqualTo(status)
      assertThat(this.response.content).isEqualTo(content)
      assertThat(this.requestHandled).isTrue()
    }
  }

  @Test fun valid() = withBearerAuth {
    val request = handleTestRequest {
      addHeader("Authorization", "Bearer $TEST_VALID_TOKEN")
    }

    request.assertHandled(HttpStatusCode.OK, TEST_USER_PRINCIPAL.token)
  }

  @Test fun invalidBearerToken() = withBearerAuth {
    val request = handleTestRequest {
      addHeader("Authorization", "Bearer invalid-token")
    }

    request.assertHandled(HttpStatusCode.Unauthorized, "")
  }

  @Test fun invalidBearerValueDueToSpaces() = withBearerAuth {
    val request = handleTestRequest {
      addHeader("Authorization", "Bearer invalid token")
    }

    request.assertHandled(HttpStatusCode.Unauthorized, "")
  }

  @Test fun noAuthorizationHeader() = withBearerAuth {
    val request = handleTestRequest {}

    request.assertHandled(HttpStatusCode.Unauthorized, "")
  }

  @Test fun invalidAuthorizationScheme() = withBearerAuth {
    val request = handleTestRequest {
      addHeader("Authorization", "NotBearer $TEST_VALID_TOKEN")
    }

    request.assertHandled(HttpStatusCode.Unauthorized, "")
  }
}
