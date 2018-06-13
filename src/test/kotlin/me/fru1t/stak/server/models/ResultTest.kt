package me.fru1t.stak.server.models

import com.google.common.truth.Truth.assertThat
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

class LegacyResultTest {
  @Test fun default() {
    val result = LegacyResult<Any>()

    assertThat(result.value).isNull()
    assertThat(result.httpStatusCode).isEqualTo(HttpStatusCode.OK)
  }
}

class ResultTest {
  /** A testing enum used as the status state for [Result]. */
  enum class TestStatusEnum {
    TEST_VALUE
  }

  @Test fun default() {
    val result = Result("Test Value", TestStatusEnum.TEST_VALUE)

    assertThat(result.value).isEqualTo("Test Value")
    assertThat(result.status).isEqualTo(TestStatusEnum.TEST_VALUE)
  }
}
