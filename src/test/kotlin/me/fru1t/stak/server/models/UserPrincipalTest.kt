package me.fru1t.stak.server.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UserPrincipalTest {
  @Test fun default() {
    val result = UserPrincipal(username = "test username", token = "test token")

    assertThat(result.username).isEqualTo("test username")
    assertThat(result.token).isEqualTo("test token")
  }
}
