package me.fru1t.stak.server.models

import io.ktor.auth.Principal

/** Internal user credentials stored within the session to track logged in users. */
data class UserPrincipal(
    /** The database [UserId] of the user. */
    val userId: UserId,
    /** The session token given to this login session. */
    val token: String) : Principal
