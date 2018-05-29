package me.fru1t.stak.server.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UserTest {
  @Test fun default() {
    val user = User(username = "test", passwordHash = "testpassword", displayName = "test name")

    assertThat(user.username).isEqualTo("test")
    assertThat(user.passwordHash).isEqualTo("testpassword")
    assertThat(user.displayName).isEqualTo("test name")
  }
}
