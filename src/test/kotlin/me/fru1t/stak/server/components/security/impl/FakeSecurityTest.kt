package me.fru1t.stak.server.components.security.impl

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FakeSecurityTest {
  companion object {
    private const val TEST_STRING = "test string"
    private const val TEST_TOKEN_LENGTH = 10L
  }

  private lateinit var fakeSecurity: FakeSecurity

  @BeforeEach fun setUp() {
    fakeSecurity = FakeSecurity()
  }

  @Test fun hash() {
    assertThat(fakeSecurity.hash(TEST_STRING)).isEqualTo(fakeSecurity.hash(TEST_STRING))
  }

  @Test fun equals() {
    val hash = fakeSecurity.hash(TEST_STRING)

    assertThat(fakeSecurity.equals(TEST_STRING, hash)).isTrue()
  }

  @Test fun equals_notSame() {
    val hash = fakeSecurity.hash("$TEST_STRING ")

    assertThat(fakeSecurity.equals(TEST_STRING, hash)).isFalse()
  }

  @Test fun equals_nullHash() {
    assertThat(fakeSecurity.equals("", null)).isFalse()
    assertThat(fakeSecurity.equals("null", null)).isFalse()
  }

  @Test fun generateRandomToken() {
    assertThat(fakeSecurity.generateRandomToken(TEST_TOKEN_LENGTH))
        .isNotEqualTo(fakeSecurity.generateRandomToken(TEST_TOKEN_LENGTH))
  }
}
