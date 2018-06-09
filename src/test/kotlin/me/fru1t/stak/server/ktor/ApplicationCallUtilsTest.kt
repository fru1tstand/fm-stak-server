package me.fru1t.stak.server.ktor

import com.google.common.truth.Truth.assertThat
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import me.fru1t.stak.server.models.Result
import org.junit.jupiter.api.Test

class ApplicationCallUtilsTest {
  companion object {
    fun TestApplicationCall.assert(
        requestHandled: Boolean,
        contentType: ContentType,
        content: String?,
        status: HttpStatusCode) {
      assertThat(this.requestHandled).isEqualTo(requestHandled)
      assertThat(this.response.contentType().contentType).contains(contentType.contentType)
      assertThat(this.response.content).isEqualTo(content)
      assertThat(this.response.status()).isEqualTo(status)
    }
  }

  @Test fun respondResult_default() = withTestApplication {
    val responseResult = Result("This is a successful result")
    application.routing {
      get("/") { call.respondResult(responseResult) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(true, ContentType.Text.Plain, responseResult.value, HttpStatusCode.OK)
  }

  @Test fun respondResult_default_noValue() = withTestApplication {
    application.routing {
      get("/") { call.respondResult(Result<Any>(null)) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(true, ContentType.Text.Plain, "", HttpStatusCode.OK)
  }

  @Test fun respondResult_success() = withTestApplication {
    val responseText = "[ \"test response\" ]"
    val responseType = ContentType.Application.Json
    val responseResult =
      Result(value = "This is a successful result", httpStatusCode = HttpStatusCode.Accepted)
    application.routing {
      get("/") { call.respondResult(responseResult, { responseText }, responseType) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(true, responseType, responseText, responseResult.httpStatusCode)
  }

  @Test fun respondResult_notSuccess() = withTestApplication {
    val responseResult = Result<Any>(httpStatusCode = HttpStatusCode.Unauthorized)
    application.routing {
      get("/") { call.respondResult(responseResult) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(true, ContentType.Text.Plain, "", HttpStatusCode.Unauthorized)
  }

  @Test fun respondEmpty_default() = withTestApplication {
    application.routing {
      get("/") { call.respondEmpty() }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(true, ContentType.Text.Plain, "", HttpStatusCode.OK)
  }

  @Test fun respondEmpty() = withTestApplication {
    val responseStatus = HttpStatusCode.Continue
    application.routing {
      get("/") { call.respondEmpty(responseStatus) }
    }

    val result = handleRequest(HttpMethod.Get, "/")

    result.assert(true, ContentType.Text.Plain, "", responseStatus)
  }
}
