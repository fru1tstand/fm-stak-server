package me.fru1t.stak.server.models

/** Represents an entity that's the owner of tasks. */
data class User(
    /** A unique identifier for the user. */
    val username: String,
    /** The user's password in hashed form. */
    val passwordHash: String,
    /** A string used to refer to the user. */
    val displayName: String)
