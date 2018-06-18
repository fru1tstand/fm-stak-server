package me.fru1t.stak.server.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UserIdTest {
  @Test fun default() {
    val userId = UserId(username = "test")

    assertThat(userId.username).isEqualTo("test")
  }
}

class UserTest {
  @Test fun default() {
    val user =
      User(
          userId = UserId(username = "test"),
          passwordHash = "test password",
          displayName = "test name")

    assertThat(user.userId).isEqualTo(UserId(username = "test"))
    assertThat(user.passwordHash).isEqualTo("test password")
    assertThat(user.displayName).isEqualTo("test name")
  }
}

class UserCreateTest {
  @Test fun default() {
    val userCreate = UserCreate(username = "test", password = "test password", displayName = "name")

    assertThat(userCreate.username).isEqualTo("test")
    assertThat(userCreate.password).isEqualTo("test password")
    assertThat(userCreate.displayName).isEqualTo("name")
  }
}
