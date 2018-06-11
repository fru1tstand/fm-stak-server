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
  /** Test enum that implements [StatusEnum]. */
  enum class TestStatusEnum(val httpStatusCode: HttpStatusCode) : StatusEnum {
    TEST_VALUE(HttpStatusCode.OK);
    override fun getStatus(): HttpStatusCode = httpStatusCode
  }

  @Test fun default() {
    val result = Result("Test Value", TestStatusEnum.TEST_VALUE)

    assertThat(result.value).isEqualTo("Test Value")
    assertThat(result.status).isEqualTo(TestStatusEnum.TEST_VALUE)
  }
}
