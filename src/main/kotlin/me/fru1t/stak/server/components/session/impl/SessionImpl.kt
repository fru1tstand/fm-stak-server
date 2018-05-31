package me.fru1t.stak.server.components.session.impl

import com.google.common.base.Ticker
import com.google.common.cache.CacheBuilder
import io.ktor.auth.UserPasswordCredential
import io.ktor.http.HttpStatusCode
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.session.Session
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.UserPrincipal
import mu.KLogging
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.streams.asSequence

/** The default implementation of [Session]. */
class SessionImpl @Inject constructor(
    private val database: Database,
    @Named(Constants.NAMED_SESSION_TIMEOUT_HOURS) private val sessionTimeoutHours: Long,
    private val cacheTicker: Ticker) : Session {
  companion object : KLogging() {
    private const val TOKEN_CHARACTERS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+=-[],.<>/?';:"
    private const val TOKEN_LENGTH = 128L

    private const val BCRYPT_ROUNDS = 12
    private val DUMMY_PASSWORD_HASH = BCrypt.hashpw("", BCrypt.gensalt(BCRYPT_ROUNDS))

    /** Generates a cryptographically secure random session token. */
    private fun generateToken(): String =
      SecureRandom().ints(TOKEN_LENGTH, 0, TOKEN_CHARACTERS.length)
          .asSequence()
          .map(TOKEN_CHARACTERS::get)
          .joinToString("")
  }

  private val activeSessions = CacheBuilder.newBuilder()
      .ticker(cacheTicker)
      .expireAfterAccess(sessionTimeoutHours, TimeUnit.HOURS)
      .build<String, UserPrincipal>()

  override fun login(userPasswordCredential: UserPasswordCredential): Result<UserPrincipal> {
    val userResult = database.getUserByUsername(userPasswordCredential.name)
    if (userResult.error != null && userResult.isDatabaseError) {
      return Result<UserPrincipal>(
          error = "An internal error occurred.",
          httpStatusCode = HttpStatusCode.InternalServerError)
          .withCode { logger.error { "Database error: ${userResult.error}; Code: $it" } }
    }

    val existingPasswordHash = userResult.result?.passwordHash ?: DUMMY_PASSWORD_HASH
    if (!BCrypt.checkpw(userPasswordCredential.password, existingPasswordHash)) {
      return Result(
          error = "Invalid username or password", httpStatusCode = HttpStatusCode.Unauthorized)
    }

    val userPrincipal = UserPrincipal(
        username = userResult.result!!.username, token = generateToken())
    activeSessions.put(userPrincipal.token, userPrincipal)
    return Result(value = userPrincipal)
  }

  override fun logout(token: String): Boolean {
    if (activeSessions.getIfPresent(token) != null) {
      activeSessions.invalidate(token)
      return true
    }
    return false
  }

  override fun getActiveSession(token: String): Result<UserPrincipal> =
    activeSessions.getIfPresent(token)?.let { Result(value = it) }
        ?: Result(error = "Invalid session token.", httpStatusCode = HttpStatusCode.Unauthorized)
}
