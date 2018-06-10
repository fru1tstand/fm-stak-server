package me.fru1t.stak.server.models

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

/**
 * A [Result] contains an optional return value and an [HttpStatusCode] representing its result
 * state. [Result]s are predominately used to provide context to an internal controller call.
 *
 * Note: A method that returns a [Result] should document its [HttpStatusCode] possibilities and
 * describe what each possibility means.
 */
data class Result<T>(
    /** A successfully yielded value from an operation. */
    val value: T? = null,

    /**
     * The HTTP status code that should be returned. Use [HttpStatusCode.isSuccess] to determine
     * whether or not this [Result] was successful.
     */
    val httpStatusCode: HttpStatusCode = HttpStatusCode.OK)
