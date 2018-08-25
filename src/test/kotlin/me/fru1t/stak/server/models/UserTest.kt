package me.fru1t.stak.server.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserTest {
  companion object {
    private const val TEST_USERNAME = "test username"
    private const val TEST_PASSWORD = "some password"
    private const val TEST_DISPLAY_NAME = "fru1tstand"
  }

  @Nested inner class UserTest {
    @Test fun default() {
      val user =
        User(
          userId = UserId(username = TEST_USERNAME),
          passwordHash = TEST_PASSWORD,
          displayName = TEST_DISPLAY_NAME)

      assertThat(user.userId).isEqualTo(UserId(username = TEST_USERNAME))
      assertThat(user.passwordHash).isEqualTo(TEST_PASSWORD)
      assertThat(user.displayName).isEqualTo(TEST_DISPLAY_NAME)
    }
  }

  @Nested inner class UserIdTest {
    @Test fun default() {
      val userId = UserId(username = TEST_USERNAME)

      assertThat(userId.username).isEqualTo(TEST_USERNAME)
    }
  }

  @Nested inner class UserCreateTest {
    @Test fun default() {
      val userCreate =
        UserCreate(
          username = TEST_USERNAME, password = TEST_PASSWORD, displayName = TEST_DISPLAY_NAME)

      assertThat(userCreate.username).isEqualTo(TEST_USERNAME)
      assertThat(userCreate.password).isEqualTo(TEST_PASSWORD)
      assertThat(userCreate.displayName).isEqualTo(TEST_DISPLAY_NAME)
    }
  }

  @Nested inner class UserModifyTest {
    @Test fun default() {
      val userModify = UserModify()

      assertThat(userModify.username).isNull()
      assertThat(userModify.password).isNull()
      assertThat(userModify.displayName).isNull()
    }

    @Test fun constructor() {
      val userModify =
        UserModify(
          username = TEST_USERNAME, password = TEST_PASSWORD, displayName = TEST_DISPLAY_NAME)

      assertThat(userModify.username).isEqualTo(TEST_USERNAME)
      assertThat(userModify.password).isEqualTo(TEST_PASSWORD)
      assertThat(userModify.displayName).isEqualTo(TEST_DISPLAY_NAME)
    }

    @Test fun isEmpty() {
      val userModify = UserModify()

      assertThat(userModify.isEmpty()).isTrue()
    }

    @Test fun isEmpty_returnsFalse_whenAnyFieldIsNonNull() {
      val userModifyWithUsername = UserModify(username = TEST_USERNAME)
      val userModifyWithPassword = UserModify(password = TEST_PASSWORD)
      val userModifyWithDisplayName = UserModify(displayName = TEST_DISPLAY_NAME)

      assertThat(userModifyWithUsername.isEmpty()).isFalse()
      assertThat(userModifyWithPassword.isEmpty()).isFalse()
      assertThat(userModifyWithDisplayName.isEmpty()).isFalse()
    }
  }
}
