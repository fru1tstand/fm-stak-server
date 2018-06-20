package me.fru1t.stak.server.components.session.impl

import com.google.common.base.Ticker
import com.google.common.cache.CacheBuilder
import io.ktor.auth.UserPasswordCredential
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.database.Database
import me.fru1t.stak.server.components.database.Database.GetUserByIdStatus
import me.fru1t.stak.server.components.security.Security
import me.fru1t.stak.server.components.session.SessionController
import me.fru1t.stak.server.components.session.SessionController.LoginStatus
import me.fru1t.stak.server.models.Result
import me.fru1t.stak.server.models.Status
import me.fru1t.stak.server.models.UserId
import me.fru1t.stak.server.models.UserPrincipal
import mu.KLogging
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/** The default implementation of [SessionController] which uses an in-memory, expiring cache. */
class SessionControllerImpl @Inject constructor(
    private val database: Database,
    @Named(Constants.NAMED_SESSION_TIMEOUT_HOURS) private val sessionTimeoutHours: Long,
    private val security: Security,
    cacheTicker: Ticker) : SessionController {
  companion object : KLogging() {
    private const val TOKEN_LENGTH = 128L
  }

  private val activeSessions =
    CacheBuilder.newBuilder()
        .ticker(cacheTicker)
        .expireAfterAccess(sessionTimeoutHours, TimeUnit.HOURS)
        .build<String, UserPrincipal>()

  override fun login(userPasswordCredential: UserPasswordCredential)
      : Result<UserPrincipal?, SessionController.LoginStatus> {
    // Fetch from database
    val userResult = database.getUserById(UserId(userPasswordCredential.name))
    val user = when (userResult.status) {
      GetUserByIdStatus.DATABASE_ERROR -> return Result(null, LoginStatus.DATABASE_ERROR)
      GetUserByIdStatus.USER_ID_NOT_FOUND,
      GetUserByIdStatus.SUCCESS -> userResult.value
    }

    // Verify password
    if (!security.equals(userPasswordCredential.password, user?.passwordHash)) {
      return Result(null, SessionController.LoginStatus.BAD_USERNAME_OR_PASSWORD)
    }

    val userPrincipal =
      UserPrincipal(user!!.userId, security.generateRandomToken(TOKEN_LENGTH))
    synchronized(activeSessions) {
      activeSessions.put(userPrincipal.token, userPrincipal)
    }
    return Result(userPrincipal, SessionController.LoginStatus.SUCCESS)
}

  override fun logout(token: String): Boolean {
    synchronized(activeSessions) {
      if (activeSessions.getIfPresent(token) != null) {
        activeSessions.invalidate(token)
        return true
      }
      return false
    }
  }

  override fun getActiveSession(token: String)
      : Result<UserPrincipal?, SessionController.GetActiveSessionStatus> =
    synchronized(activeSessions) {
      activeSessions.getIfPresent(token)
          ?.let { Result(it, SessionController.GetActiveSessionStatus.SUCCESS) }
          ?: Result(null, SessionController.GetActiveSessionStatus.SESSION_NOT_FOUND)
    }

  override fun stopAllSessionsForUserId(userId: UserId)
      : Status<SessionController.StopAllSessionsForUserIdStatus> {
    synchronized(activeSessions) {
      activeSessions.asMap().forEach {
        token, userPrincipal ->
        if (userPrincipal.userId == userId) {
          activeSessions.invalidate(token)
        }
      }
    }
    return Status(SessionController.StopAllSessionsForUserIdStatus.SUCCESS)
  }
}
