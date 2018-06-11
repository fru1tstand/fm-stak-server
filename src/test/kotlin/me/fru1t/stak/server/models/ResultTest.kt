package me.fru1t.stak.server.models

import com.google.common.truth.Truth.assertThat
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

class ResultTest {
  @Test fun default() {
    val result = LegacyResult<Any>()

    assertThat(result.value).isNull()
    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.OK)
  }
}
