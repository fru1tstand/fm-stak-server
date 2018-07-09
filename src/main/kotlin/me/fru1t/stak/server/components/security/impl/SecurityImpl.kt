package me.fru1t.stak.server.components.security.impl

import me.fru1t.stak.server.components.security.Security
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import javax.inject.Inject
import kotlin.streams.asSequence

/** The default implementation of [Security] back by BCrypt. */
class SecurityImpl @Inject constructor() : Security {
  companion object {
    private const val BCRYPT_ROUNDS = 12
    private val DUMMY_PASSWORD_HASH = BCrypt.hashpw("", BCrypt.gensalt(BCRYPT_ROUNDS))

    private const val TOKEN_CHARACTERS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  }

  override fun hash(string: String): String = BCrypt.hashpw(string, BCrypt.gensalt(BCRYPT_ROUNDS))

  override fun equals(string: String, hash: String?): Boolean {
    val result = BCrypt.checkpw(string, hash ?: DUMMY_PASSWORD_HASH)
    return if (hash != null) result else false
  }

  override fun generateRandomToken(length: Long): String =
    SecureRandom().ints(length, 0, TOKEN_CHARACTERS.length)
        .asSequence()
        .map(TOKEN_CHARACTERS::get)
        .joinToString("")
}
