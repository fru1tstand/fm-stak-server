package me.fru1t.stak.server.components.database

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class DatabaseResultTest {
  @Test fun default() {
    val result = DatabaseResult<Any>()

    assertThat(result.result).isNull()
    assertThat(result.error).isNull()
    assertThat(result.isDatabaseError).isFalse()
  }
}

class DatabaseOperationResultTest {
  @Test fun default() {
    val result = DatabaseOperationResult()

    assertThat(result.didSucceed).isFalse()
    assertThat(result.error).isNull()
    assertThat(result.isDatabaseError).isFalse()
  }
}
