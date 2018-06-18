package me.fru1t.stak.server.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UserPrincipalTest {
  @Test fun default() {
    val result = UserPrincipal(UserId("test username"), "test token")

    assertThat(result.userId).isEqualTo(UserId("test username"))
    assertThat(result.token).isEqualTo("test token")
  }
}
