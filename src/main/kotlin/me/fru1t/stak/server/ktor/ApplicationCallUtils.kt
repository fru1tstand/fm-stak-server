package me.fru1t.stak.server.ktor

import com.google.gson.JsonSyntaxException
import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.request.receiveOrNull
import io.ktor.response.respondText
import me.fru1t.stak.server.models.LegacyResult
import mu.KLogger
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * Responds to a request using a [LegacyResult]. Http status will always be set to the
 * [LegacyResult.httpStatusCode]. If the [result] was successful, the response body will be set to the
 * output of [successText] with content type [successContentType]. If the [result] was unsuccessful,
 * the response body will be an empty string with the content type [ContentType.Text.Plain].
 */
suspend fun <T> ApplicationCall.respondResult(
    result: LegacyResult<T>,
    successText: (T?) -> String = { it?.toString() ?: "" },
    successContentType: ContentType = ContentType.Text.Plain) {
  if (result.httpStatusCode.isSuccess()) {
    respondText(
        text = successText(result.value),
        contentType = successContentType,
        status = result.httpStatusCode)
  } else {
    respondText(text = "", contentType = ContentType.Text.Plain, status = result.httpStatusCode)
  }
}

/**
 * Responds to a request with [httpStatusCode] passing an empty body and a
 * [ContentType.Text.Plain] content-type.
 */
suspend fun ApplicationCall.respondEmpty(httpStatusCode: HttpStatusCode = HttpStatusCode.OK) {
  respondText(text = "", contentType = ContentType.Text.Plain, status = httpStatusCode)
}

/**
 * Responds to a request with [HttpStatusCode.NotImplemented] and error logs the
 * [unexpectedResult]'s [HttpStatusCode] providing context on the [producingFunction].
 */
suspend fun ApplicationCall.respondResultNotImplemented(
    unexpectedResult: LegacyResult<*>, producingFunction: KFunction<*>, logger: KLogger) {
  logger.error {
    "Unexpected result from $producingFunction. HttpStatusCode not handled: " +
        unexpectedResult.httpStatusCode
  }
  respondText(
      text = "", contentType = ContentType.Text.Plain, status = HttpStatusCode.NotImplemented)
}

/**
 * Receives [T] from this [ApplicationCall] or responds with [HttpStatusCode.BadRequest] and returns
 * `null`. This method will catch json parsing errors.
 */
suspend fun <T : Any> ApplicationCall.receiveOrBadRequest(expectedType: KClass<T>) : T? {
  val result: T? = try {
    receiveOrNull(expectedType)
  } catch (e: JsonSyntaxException) {
    null // Treat JsonSyntaxException the same as an invalid request
  }
  if (result == null) {
    respondEmpty(HttpStatusCode.BadRequest)
  }
  return result
}

/**
 * Receives [T] from this [ApplicationCall] or responds with [HttpStatusCode.BadRequest] and returns
 * `null`. This method will catch json parsing errors.
 */
suspend inline fun <reified T : Any> ApplicationCall.receiveOrBadRequest() : T? =
  receiveOrBadRequest(T::class)
