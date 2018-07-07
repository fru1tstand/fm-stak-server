package me.fru1t.stak.server.ktor.auth

import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.HttpAuthHeader
import io.ktor.auth.Principal
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import me.fru1t.stak.server.ktor.respondEmpty

/**
 * Represents a Bearer authentication provider.
 * @param name is the name of the provider, or `null` for a default provider.
 */
class BearerAuthenticationProvider(name: String?) : AuthenticationProvider(name) {
  internal var authenticationFunction: suspend (String) -> Principal? = { null }

  /**
   * Sets a validation function that will check given [String] which represents the Bearer token,
   * and return a [Principal] or `null` if the token does not correspond to an authenticated
   * principal.
   */
  fun validate(body: suspend (String) -> Principal?) {
    authenticationFunction = body
  }
}

/**
 * Installs Bearer Authentication mechanism. This authentication configuration guarantees a routed
 * authentication block always has credentials by responding with [HttpStatusCode.Unauthorized] if
 * the request's Bearer credentials are missing or invalid.
 */
fun Authentication.Configuration.bearer(
    name: String? = null, configure: BearerAuthenticationProvider.() -> Unit) {
  val provider = BearerAuthenticationProvider(name).apply(configure)
  val authenticate = provider.authenticationFunction

  provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
    // Store principal if authenticate function produces the principal if the bearer token exists
    val bearerCredentials = call.request.bearerAuthenticationCredentials()
    val sessionPrincipal = bearerCredentials?.let { authenticate(it) }
    if (sessionPrincipal != null) {
      context.principal(sessionPrincipal)
    } else {
      val cause =
        if (bearerCredentials == null) AuthenticationFailedCause.NoCredentials
        else AuthenticationFailedCause.InvalidCredentials
      context.challenge(bearerAuthenticationChallengeKey, cause) {
        call.respondEmpty(HttpStatusCode.Unauthorized)
        it.complete()
      }
    }
  }

  register(provider)
}

/** Retrieves Bearer authentication credentials for this [ApplicationRequest]. */
fun ApplicationRequest.bearerAuthenticationCredentials(): String? {
  // Grab the value of the auth header
  val parsed = parseAuthorizationHeader()
  when (parsed) {
    // Auth type bearer is of type "single" (ie. it only has a single value after "Bearer")
    is HttpAuthHeader.Single -> {
      // Check that the scheme is in fact the bearer type
      if (parsed.authScheme != bearerAuthenticationSchemeName) {
        return null
      }

      // Return the payload
      return parsed.blob
    }

    // Any other type of request shouldn't go through
    else -> return null
  }
}

private const val bearerAuthenticationChallengeKey = "BearerAuth"
private const val bearerAuthenticationSchemeName = "Bearer"
