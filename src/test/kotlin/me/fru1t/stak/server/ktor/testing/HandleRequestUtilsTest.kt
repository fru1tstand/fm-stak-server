package me.fru1t.stak.server.ktor.testing

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.contentType
import io.ktor.request.receiveOrNull
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.ktor.respondEmpty
import org.junit.jupiter.api.Test

class HandleRequestUtilsTest {
  /** A basic data class for testing. */
  private data class TestClass(val param1: String, val param2: Int)

  @Test fun handleFormRequest() = withTestApplication {
    val httpMethod = HttpMethod.Delete
    val uri = "/example/uri"

    val result = handleFormRequest(httpMethod, uri) {}

    assertThat(result.request.contentType().toString())
        .contains("application/x-www-form-urlencoded")
    assertThat(result.request.uri).isEqualTo(uri)
    assertThat(result.request.method).isEqualTo(httpMethod)
  }

  @Test fun handleJsonRequest() = withTestApplication {
    val httpMethod = HttpMethod.Patch
    val uri = "/example"
    val gson = Gson()
    val data = TestClass("example param 1", 300)

    val result = this.handleJsonRequest(data, gson, httpMethod, uri) {}

    assertThat(result.getBody()).isEqualTo(gson.toJson(data))
    assertThat(result.request.method).isEqualTo(httpMethod)
    assertThat(result.request.contentType().toString()).contains("application/json")
  }

  @Test fun installFakeJsonContentNegotiation() = withTestApplication {
    val testData = TestClass("test string", 1234)
    this.installFakeJsonContentNegotiation()
    application.routing {
      post("test") {
        // Utilizes the content negotiation
        val receivedData = call.receiveOrNull(TestClass::class)

        if (receivedData == null) {
          call.respondEmpty(HttpStatusCode.BadRequest)
          return@post
        }

        assertThat(receivedData).isEqualTo(testData)
        call.respondEmpty(HttpStatusCode.OK)
      }
    }

    val request = handleRequest(HttpMethod.Post, "/test") {
      addHeader("content-type", "application/json")
      setBody(Gson().toJson(testData))
    }

    assertThat(request.response.status()).isEqualTo(HttpStatusCode.OK)
    assertThat(request.requestHandled).isTrue()
  }

  @Test fun addBearerAuthorizationHeader() = withTestApplication {
    val token = "test-token"
    val expectedAuthorizationString = "Bearer $token"

    val result = handleRequest {
      addBearerAuthorizationHeader(token)
    }

    assertThat(result.request.headers["Authorization"]).isEqualTo(expectedAuthorizationString)
  }

  @Test fun setBody() = withTestApplication {
    val request = handleRequest(HttpMethod.Post, "/") {
      setBody {
        addParameter("key", "value")
        addParameter("key2", "value2")
      }
    }

    val result = request.getBody()
    assertThat(result).contains("key=value")
    assertThat(result).contains("&")
    assertThat(result).contains("key2=value2")
  }

  @Test fun getBody() = withTestApplication {
    val body = "example content in {{{ the body with lots of 43333 !#(^%$&^%@) things."

    val request = handleRequest { setBody(body) }

    val result = request.getBody()
    assertThat(result).isEqualTo(body)
  }
}

class RequestBodyBuilderTest {
  /** A basic data class for testing. */
  private data class TestClass(val test: String, private val test2: String)

  @Test fun addParameter() {
    val result = RequestBodyBuilder().run {
      addParameter("test", "value1")
      addParameter("test2", "value with spaces&")
      build()
    }

    assertThat(result).contains("test=value1")
    assertThat(result).contains("&")
    assertThat(result).contains("test2=value+with+spaces%26")
  }

  @Test fun addData() {
    val data = TestClass("value1", "value2")
    val result = RequestBodyBuilder().run {
      addData(data)
      build()
    }

    assertThat(result).contains("test=value1")
    assertThat(result).contains("&")
    assertThat(result).contains("test2=value2")
  }
}
