package me.fru1t.stak.server.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

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

class StatusTest {
  enum class TestStatusEnum {
    TEST_VALUE
  }

  @Test fun default() {
    val result = Status(TestStatusEnum.TEST_VALUE)

    assertThat(result.status).isEqualTo(TestStatusEnum.TEST_VALUE)
  }
}
