package me.fru1t.stak.server.models

import io.ktor.http.HttpStatusCode
import java.util.Random
import kotlin.streams.asSequence

/**
 * A result contains a yielded value and an error if one occurred. Usually a [Result] will only
 * have one or the other and not both.
 */
data class Result<T>(
    /** A successfully yielded value from an operation. */
    val value: T? = null,

    /** The HTTP status code that should be returned. */
    val httpStatusCode: HttpStatusCode = HttpStatusCode.OK,

    /** The unsuccessful fetch of [value]'s error message. */
    val error: String? = null) {
  companion object {
    private const val SOURCE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    private const val CODE_LENGTH = 18L
    private const val CODE_STRING_SUFFIX = " Code: "
  }

  /**
   * Creates a unique code to append to this [error] and passes it to the [codeHandler], usually for
   * logging.
   */
  fun withCode(codeHandler: (String) -> Unit): Result<T> {
    val code = Random().ints(CODE_LENGTH, 0, SOURCE.length)
        .asSequence()
        .map(SOURCE::get)
        .joinToString("")

    codeHandler(code)
    return copy(error = error + CODE_STRING_SUFFIX + code)
  }
}
