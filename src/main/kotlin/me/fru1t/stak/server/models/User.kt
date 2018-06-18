package me.fru1t.stak.server.models

/**
 * Data that uniquely identifies a single user within the stak server. This is the data object that
 * should be used when passing a user reference between methods.
 */
data class UserId(
    /** A unique identifier for the user. */
    val username: String)

/**
 * Represents an entity that's the owner of tasks. This is used as an internal representation of
 * a user and should never be passed to a client. This class should never be used to pass user data
 * between components. Always use [UserId] when passing references to users.
 */
data class User(
    /** A unique identifier for the user. */
    val userId: UserId,
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
