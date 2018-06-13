package me.fru1t.stak.server.models

import io.ktor.http.HttpStatusCode

/**
 * A [LegacyResult] contains an optional return value and an [HttpStatusCode] representing its result
 * state. [LegacyResult]s are predominately used to provide context to an internal controller call.
 *
 * Note: A method that returns a [LegacyResult] should document its [HttpStatusCode] possibilities and
 * describe what each possibility means.
 */
@Deprecated(
    "Introduces too much if-this-then-that behavior due to the httpStatusCode",
    ReplaceWith("Result", "me.fru1t.stak.server.models"))
data class LegacyResult<T>(
    /** A successfully yielded value from an operation. */
    val value: T? = null,

    /**
     * The HTTP status code that should be returned. Use [HttpStatusCode.isSuccess] to determine
     * whether or not this [LegacyResult] was successful.
     */
    val httpStatusCode: HttpStatusCode = HttpStatusCode.OK)

/**
 * Provides context to a function's return by providing two fields: the yielded [value] and the
 * [status]. Results are most notably used in `when` expression statements (ie. `return when(...)`
 * or `val foo = when(...)`) against the [status] which enables static checking to validate that
 * all possibilities of the [status] are accounted for.
 * @param V the value type which can be nullable or even [Nothing].
 * @param S the status type which must be an [Enum].
 */
data class Result<V, S : Enum<S>>(
    /** The yielded value of the method. */
    val value: V,

    /** Context for why the method returned. */
    val status: S)
