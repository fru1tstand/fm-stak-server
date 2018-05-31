package me.fru1t.stak.server.models

import io.ktor.auth.Principal

/** Internal user credentials stored within the session to track logged in users. */
data class UserPrincipal(
    /** The database username of the user. */
    val username: String,
    /** The session token given to this login session. */
    val token: String) : Principal
