package me.fru1t.stak.server.ktor.testing

import com.google.common.io.CharStreams
import com.google.common.truth.Truth.assertThat
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.experimental.io.jvm.javaio.toInputStream
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import java.util.Base64

class HandleRequestUtilsTest {
  @Test fun handleFormRequest() = withTestApplication {
    val httpMethod = HttpMethod.Delete
    val uri = "/example/uri"

    val result = handleFormRequest(httpMethod, uri) {}

    assertThat(result.request.headers["content-type"]!!)
        .contains("application/x-www-form-urlencoded")
    assertThat(result.request.uri).isEqualTo(uri)
  }

  @Test fun addBasicAuthorizationHeader() = withTestApplication {
    val username = "username"
    val password = "password"
    val authorizationString =
      "Basic " +
          Base64.getEncoder().encode("$username:$password".toByteArray(Charsets.UTF_8))
              .toString(Charsets.UTF_8)

    val result = handleRequest {
      addBasicAuthorizationHeader(username, password)
    }

    assertThat(result.request.headers["Authorization"]).isEqualTo(authorizationString)
  }

  @Test fun setBody() = withTestApplication {
    val result = handleRequest(HttpMethod.Post, "/") {
      setBody {
        addParameter("key", "value")
        addParameter("key2", "value2")
      }
    }

    val body = CharStreams.toString(
        InputStreamReader(result.request.bodyChannel.toInputStream(), Charsets.UTF_8))

    assertThat(body).contains("key=value")
    assertThat(body).contains("&")
    assertThat(body).contains("key2=value2")
  }
}

class RequestBodyBuilderTest {
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
