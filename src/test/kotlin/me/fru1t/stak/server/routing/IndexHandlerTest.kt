package me.fru1t.stak.server.routing

import com.google.common.truth.Truth.assertThat
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.routing.routing
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IndexHandlerTest {
  private lateinit var indexHandler: IndexHandler

  @BeforeEach internal fun setUp() {
    indexHandler = IndexHandler()
  }

  @Test fun catchAll() = withTestApplication {
    // Setup routing through index
    application.routing {
      index(indexHandler)
    }

    // Perform request
    val result = handleRequest(HttpMethod.Get, "/")

    // Verify our call was processed
    assertThat(result.requestHandled).isTrue()
    assertThat(result.response.contentType().contentType)
        .contains(ContentType.Text.Html.contentType)
    assertThat(result.response.content).contains("Hello Index")
  }

  @Test fun catchAll_everythingElse() = withTestApplication {
    // Setup routing through index
    application.routing {
      index(indexHandler)
    }

    // Perform request
    val result = handleRequest(HttpMethod.Get, "sdfgsdfg")

    // Verify our call was processed
    assertThat(result.requestHandled).isTrue()
    assertThat(result.response.contentType().contentType)
        .contains(ContentType.Text.Html.contentType)
    assertThat(result.response.content).contains("Hello Index")
  }
}
