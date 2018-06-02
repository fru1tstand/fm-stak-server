package me.fru1t.stak.server.models

/**
 * Represents an entity that's the owner of tasks. This is used as an internal representation of
 * a user and should never be passed to a client.
 */
data class User(
    /** A unique identifier for the user. */
    val username: String,
    /** The user's password in hashed form. */
    val passwordHash: String,
    /** A string used to refer to the user. */
    val displayName: String)

/**
 * Routing data structure received from a client to create a user. See [User]. This is used as an
 * intermediary data structure and should never be passed to a client.
 */
data class UserCreate(
    val username: String,
    val password: String,
    val displayName: String)
