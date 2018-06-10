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
        addParameter("some-key", "a-value with-spaces")
        addParameter("another-key", "with-a-&bad.value")
      }
    }

    val body = CharStreams.toString(
        InputStreamReader(result.request.bodyChannel.toInputStream(), Charsets.UTF_8))
    assertThat(body).isEqualTo("some-key=a-value+with-spaces&another-key=with-a-%26bad.value")
  }
}
