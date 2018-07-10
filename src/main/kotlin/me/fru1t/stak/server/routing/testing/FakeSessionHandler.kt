package me.fru1t.stak.server.routing.testing

import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.server.testing.TestApplicationEngine
import me.fru1t.stak.server.Constants
import me.fru1t.stak.server.components.security.testing.FakeSecurity
import me.fru1t.stak.server.ktor.auth.bearer
import me.fru1t.stak.server.models.User
import me.fru1t.stak.server.models.UserPrincipal
import org.jetbrains.annotations.TestOnly

/** A session handler made for testing `authenticate(Constants.SESSION_AUTH_NAME)` routes. */
internal class FakeSessionHandler @TestOnly constructor() {
  companion object {
    private const val TOKEN_LENGTH = 64L
    private val fakeSecurity = FakeSecurity()
  }

  /** Stores active sessions set by [addActiveSession]. */
  val activeSessions: HashMap<String, UserPrincipal> = hashMapOf()

  /**
   * Creates a session for [user]. This method will return unique values for the same [user] to
   * simulate multiple sessions for a single [User]. Returns the generated session's
   * [UserPrincipal].
   */
  fun addActiveSession(user: User): UserPrincipal {
    val resultUserPrincipal =
      UserPrincipal(user.userId, fakeSecurity.generateRandomToken(TOKEN_LENGTH))
    activeSessions[resultUserPrincipal.token] = resultUserPrincipal
    return resultUserPrincipal
  }
}

/**
 * Registers a [bearer] type Authorization header authentication scheme that's validated by
 * [fakeSessionHandler]'s active sessions.
 */
@TestOnly
internal fun TestApplicationEngine.installFakeAuthentication(
    fakeSessionHandler: FakeSessionHandler) {
  application.install(Authentication) {
    bearer(Constants.SESSION_AUTH_NAME) {
      validate { token -> fakeSessionHandler.activeSessions[token] }
    }
  }
}
