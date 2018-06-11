package me.fru1t.stak.server.components.session.impl

import com.google.common.base.Ticker
import com.google.common.cache.CacheBuilder
import io.ktor.auth.UserPasswordCredential
import io.ktor.http.HttpStatusCode
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.UserPrincipal
import mu.KLogging
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/** The default implementation of [SessionController]. */
class SessionControllerImpl @Inject constructor(
    private val database: Database,
    @Named(Constants.NAMED_SESSION_TIMEOUT_HOURS) private val sessionTimeoutHours: Long,
    private val security: Security,
    cacheTicker: Ticker) : SessionController {
  companion object : KLogging() {
    private const val TOKEN_LENGTH = 128L
  }

  private val activeSessions = CacheBuilder.newBuilder()
      .ticker(cacheTicker)
      .expireAfterAccess(sessionTimeoutHours, TimeUnit.HOURS)
      .build<String, UserPrincipal>()

  override fun login(userPasswordCredential: UserPasswordCredential): Result<UserPrincipal> {
    val userResult = database.getUserByUsername(userPasswordCredential.name)
    if (userResult.error != null && userResult.isDatabaseError) {
      logger.error { "Database error: ${userResult.error}" }
      return Result(httpStatusCode = HttpStatusCode.InternalServerError)
    }

    if (!security.equals(userPasswordCredential.password, userResult.result?.passwordHash)) {
      return Result(httpStatusCode = HttpStatusCode.Unauthorized)
    }

    val userPrincipal = UserPrincipal(
        username = userResult.result!!.username, token = security.generateRandomToken(TOKEN_LENGTH))
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
    activeSessions.getIfPresent(token)
        ?.let { Result(value = it) }
        ?: Result(httpStatusCode = HttpStatusCode.Unauthorized)
}
