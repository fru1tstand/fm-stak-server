package me.fru1t.stak.server.ktor

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import io.ktor.application.call
import io.ktor.application.install
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
import me.fru1t.stak.server.ktor.testing.handleJsonRequest
import me.fru1t.stak.server.models.Result
import mu.KLogger
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
    private fun TestApplicationCall.assert(
        requestHandled: Boolean,
        contentType: ContentType,
        content: String?,
        status: HttpStatusCode) {
      assertThat(this.requestHandled).isEqualTo(requestHandled)
      assertThat(this.response.contentType().toString()).contains(contentType.toString())
      assertThat(this.response.content).isEqualTo(content)
      assertThat(this.response.status()).isEqualTo(status)
    }

    /** Installs the [ContentNegotiation] module to the test application. */
    private fun withContentNegotiationTestApplication(
        test: TestApplicationEngine.() -> Unit) = withTestApplication {
      application.install(ContentNegotiation) { gson { setPrettyPrinting() } }
      test()
    }

    private fun testFunction() = "just a test"
  }

  @Test fun respondResult_default() = withTestApplication {
    val responseResult = Result("This is a successful result")
    application.routing {
      get("/") { call.respondResult(responseResult) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(
        requestHandled = true,
        contentType = ContentType.Text.Plain,
        content = responseResult.value,
        status = HttpStatusCode.OK)
  }

  @Test fun respondResult_default_noValue() = withTestApplication {
    application.routing {
      get("/") { call.respondResult(Result(null)) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(
        requestHandled = true,
        contentType = ContentType.Text.Plain,
        content = "",
        status = HttpStatusCode.OK)
  }

  @Test fun respondResult_success() = withTestApplication {
    val responseText = "[ \"test response\" ]"
    val responseType = ContentType.Application.Json
    val responseResult =
      Result(value = "This is a successful result", httpStatusCode = HttpStatusCode.Accepted)
    application.routing {
      get("/") {
        call.respondResult(
            responseResult,
            { if (it == responseResult.value) responseText else "fail" },
            responseType)
      }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(
        requestHandled = true,
        contentType = responseType,
        content = responseText,
        status = responseResult.httpStatusCode)
  }

  @Test fun respondResult_notSuccess() = withTestApplication {
    val responseResult = Result<Any>(httpStatusCode = HttpStatusCode.Unauthorized)
    application.routing {
      get("/") { call.respondResult(responseResult) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(
        requestHandled = true,
        contentType = ContentType.Text.Plain,
        content = "",
        status = HttpStatusCode.Unauthorized)
  }

  @Test fun respondEmpty_default() = withTestApplication {
    application.routing {
      get("/") { call.respondEmpty() }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(
        requestHandled = true,
        contentType = ContentType.Text.Plain,
        content = "",
        status = HttpStatusCode.OK)
  }

  @Test fun respondEmpty() = withTestApplication {
    val responseStatus = HttpStatusCode.Continue
    application.routing {
      get("/") { call.respondEmpty(responseStatus) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(
        requestHandled = true,
        contentType = ContentType.Text.Plain,
        content = "",
        status = responseStatus)
  }

  @Test fun respondResultNotImplemented() = withTestApplication {
    val mockLogger = mock<KLogger>()
    val errorCaptor = argumentCaptor<() -> String>()
    val unexpectedResponseCode = HttpStatusCode.ExceptionFailed
    application.routing {
      get("/") {
        call.respondResultNotImplemented(
            Result<Nothing>(httpStatusCode = unexpectedResponseCode),
            Companion::testFunction,
            mockLogger)
      }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(true, ContentType.Text.Plain, "", HttpStatusCode.NotImplemented)
    verify(mockLogger).error(errorCaptor.capture())
    val error = errorCaptor.firstValue()
    assertThat(error).contains("Unexpected result")
    assertThat(error).contains(Companion::testFunction.toString())
    assertThat(error).contains("HttpStatusCode not handled")
    assertThat(error).contains(unexpectedResponseCode.toString())
  }

  @Test fun receiveOrBadRequest() = withContentNegotiationTestApplication {
    application.routing {
      post("/") {
        val result = call.receiveOrBadRequest<TestClass>() ?: return@post
        call.respondText(result.param, ContentType.Text.Plain, HttpStatusCode.OK)
      }
    }

    val request = handleJsonRequest(TEST_OBJECT, Gson(), HttpMethod.Post, "/") {}

    request.assert(
        requestHandled = true,
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

    request.assert(
        requestHandled = true,
        contentType = ContentType.Text.Plain,
        content = "",
        status = HttpStatusCode.BadRequest)
  }
}
