package me.fru1t.stak.server.components.database

/** Represents a result from a database query with context if the query failed. */
data class DatabaseResult<T>(
    /** The value of the result. */
    val result: T? = null,

    /** A user-friendly string describing the error if one occurred. */
    val error: String? = null,

    /**
     * Whether or not the error was the database's fault (ie. an I/O or transaction error), as
     * opposed to user or program error (ie. the user doesn't exist). If this value is `true`, it
     * usually indicates that a retry or a fallback should be used instead.
     */
    val isDatabaseError: Boolean = false)

/** The result of a database operation that doesn't produce a result. */
data class DatabaseOperationResult(
    /** Whether or not the operation succeeded. */
    val didSucceed: Boolean = false,

    /** A user-friendly string describing the error if one occurred. */
    val error: String? = null,

    /**
     * Whether or not the error was the database's fault (ie. an I/O or transaction error), as
     * opposed to user or program error (ie. the user doesn't exist). If this value is `true`, it
     * usually indicates that a retry or a fallback should be used instead.
     */
    val isDatabaseError: Boolean = false)
