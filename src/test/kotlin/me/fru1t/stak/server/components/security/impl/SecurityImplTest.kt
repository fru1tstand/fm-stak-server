package me.fru1t.stak.server.components.security.impl

import com.google.common.truth.Truth.assertThat
import me.fru1t.stak.server.components.security.Security
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SecurityImplTest {
  companion object {
    private const val TEST_STRING = "test string"
    private const val TEST_TOKEN_LENGTH = 10L
  }
  private lateinit var securityImpl: Security

  @BeforeEach internal fun setUp() {
    securityImpl = SecurityImpl()
  }

  @Test fun hash() {
    assertThat(securityImpl.hash(TEST_STRING)).isNotEqualTo(securityImpl.hash(TEST_STRING))
  }

  @Test fun equals() {
    val hash = securityImpl.hash(TEST_STRING)

    assertThat(securityImpl.equals(TEST_STRING, hash)).isTrue()
  }

  @Test fun equals_notEquivalent() {
    val hash = securityImpl.hash("$TEST_STRING ")

    assertThat(securityImpl.equals(TEST_STRING, hash)).isFalse()
  }

  @Test fun equals_nullHashAlwaysReturnsFalse() {
    assertThat(securityImpl.equals("", null)).isFalse()
    assertThat(securityImpl.equals("null", null)).isFalse()
  }

  @Test fun generateRandomToken() {
    assertThat(securityImpl.generateRandomToken(TEST_TOKEN_LENGTH))
        .isNotEqualTo(securityImpl.generateRandomToken(TEST_TOKEN_LENGTH))
  }
}
