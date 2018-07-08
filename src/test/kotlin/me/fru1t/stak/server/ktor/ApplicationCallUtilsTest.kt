package me.fru1t.stak.server.ktor

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.ktor.auth.bearer
import me.fru1t.stak.server.ktor.testing.addBearerAuthorizationHeader
import me.fru1t.stak.server.ktor.testing.handleJsonRequest
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserPrincipal
import org.junit.jupiter.api.Test

class ApplicationCallUtilsTest {
  companion object {
    /** Simple data class for testing. */
    private data class TestClass(val param: String)

    private val TEST_OBJECT = TestClass("example value")

    /**
     * Asserts that this [TestApplicationCall] matches the following constraints, failing the test
     * if any parameter doesn't match.
     */
    private fun TestApplicationCall.assertHandled(
        contentType: ContentType,
        content: String?,
        status: HttpStatusCode) {
      assertThat(this.requestHandled).isTrue()
      assertThat(this.response.contentType().toString()).contains(contentType.toString())
      assertThat(this.response.content).isEqualTo(content)
      assertThat(this.response.status()).isEqualTo(status)
    }

    /** Asserts this [TestApplicationCall] returns an empty body with then given [status]. */
    private fun TestApplicationCall.assertEmpty(status: HttpStatusCode) {
      assertHandled(ContentType.Text.Plain, "", status)
    }

    /** Installs the [ContentNegotiation] module to the test application. */
    private fun withContentNegotiationTestApplication(
        test: TestApplicationEngine.() -> Unit) = withTestApplication {
      application.install(ContentNegotiation) { gson { setPrettyPrinting() } }
      test()
    }
  }

  @Test fun respondEmpty_default() = withTestApplication {
    application.routing {
      get("/") { call.respondEmpty() }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assertEmpty(HttpStatusCode.OK)
  }

  @Test fun respondEmpty() = withTestApplication {
    val responseStatus = HttpStatusCode.Continue
    application.routing {
      get("/") { call.respondEmpty(responseStatus) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assertEmpty(responseStatus)
  }

  @Test fun receiveOrBadRequest() = withContentNegotiationTestApplication {
    application.routing {
      post("/") {
        val result = call.receiveOrBadRequest<TestClass>() ?: return@post
        call.respondText(result.param, ContentType.Text.Plain, HttpStatusCode.OK)
      }
    }

    val request = handleJsonRequest(TEST_OBJECT, Gson(), HttpMethod.Post, "/") {}

    request.assertHandled(
        contentType = ContentType.Text.Plain,
        content = TEST_OBJECT.param,
        status = HttpStatusCode.OK)
  }

  @Test fun receiveOrBadRequest_badRequest() = withContentNegotiationTestApplication {
    application.routing {
      get("/") {
        call.receiveOrBadRequest(TestClass::class) ?: return@get
        call.respondEmpty()
      }
    }

    val request = handleJsonRequest("error{{", Gson(), HttpMethod.Get, "/") {}

    request.assertEmpty(HttpStatusCode.BadRequest)
  }

  @Test fun getSessionUserPrincipal() = withTestApplication {
    val userPrincipal = UserPrincipal(UserId("test username"), "test-token")
    application.install(Authentication) {
      bearer(Constants.SESSION_AUTH_NAME) {
        validate { _ -> userPrincipal }
      }
    }
    application.routing {
      authenticate(Constants.SESSION_AUTH_NAME) {
        get("test") {
          // Route will respond with the user principal's username
          call.respondText(
              call.getSessionUserPrincipal().userId.username,
              ContentType.Text.Plain,
              HttpStatusCode.OK)
        }
      }
    }

    val request = handleRequest(HttpMethod.Get, "/test") {
      addBearerAuthorizationHeader(userPrincipal.token)
    }

    request.assertHandled(ContentType.Text.Plain, userPrincipal.userId.username, HttpStatusCode.OK)
  }
}
