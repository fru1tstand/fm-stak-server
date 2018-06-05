package me.fru1t.stak.server.components.security

/** Methods pertaining to password hashing and token generation. */
interface Security {
  /** Securely hashes a [string] and returns the result. */
  fun hash(string: String): String

  /**
   * Returns whether or not the [string] matches the [hash] which must be a product of
   * [Security.hash]. Passing `null` into [hash] will always result in returning `false`; however,
   * this method should take the same time to complete as if [hash] weren't `null`.
   */
  fun equals(string: String, hash: String?): Boolean

  /** Generates a cryptographically random token. */
  fun generateRandomToken(length: Long): String
}
