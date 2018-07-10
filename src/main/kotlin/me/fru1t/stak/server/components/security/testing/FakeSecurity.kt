package me.fru1t.stak.server.components.security.testing

import me.fru1t.stak.server.components.security.Security
import org.jetbrains.annotations.TestOnly
import java.security.MessageDigest
import java.util.Random
import kotlin.streams.asSequence

/**
 * A fake implementation of [Security] used for testing. Methods within are backed by a SHA-256
 * hash.
 */
class FakeSecurity @TestOnly constructor() : Security {
  companion object {
    private const val TOKEN_CHARACTERS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  }

  override fun hash(string: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(string.toByteArray(Charsets.UTF_8))
    return digest.fold("") { str, it -> str + "%02x".format(it) }
  }

  override fun equals(string: String, hash: String?): Boolean = hash(string) == hash

  override fun generateRandomToken(length: Long): String =
    Random().ints(length, 0, TOKEN_CHARACTERS.length)
        .asSequence()
        .map(TOKEN_CHARACTERS::get)
        .joinToString("")
}
