package me.fru1t.stak.server.routing

import com.google.common.truth.Truth.assertThat
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserPasswordCredential
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserHandlerTest {
  private lateinit var userHandler: UserHandler

  @BeforeEach fun setUp() {
    userHandler = UserHandler()
  }

  @Test fun registerAuthentication() = withTestApplication {
    installRoutingAndAuthentication(application)

    val result = handleRequest(HttpMethod.Post, "/user/login") {
      addHeader("Authorization", "Basic dGVzdDp0ZXN0")
    }

    assertThat(result.requestHandled).isTrue()
    assertThat(result.response.status()).isEqualTo(HttpStatusCode.OK)
  }

  @Test fun validate() = withTestApplication {
    val result = userHandler.validate(UserPasswordCredential("test", ""))

    assertThat(result).isNotNull()
    assertThat(result!!.name).isEqualTo("test account")
  }

  @Test fun validate_fail() = withTestApplication {
    val result = userHandler.validate(UserPasswordCredential("not-a-test", ""))

    assertThat(result).isNull()
  }

  private fun installRoutingAndAuthentication(application: Application) {
    application.install(Authentication) {
      userHandler.registerAuthentication(this)
    }
    application.routing {
      user()
    }
  }
}
