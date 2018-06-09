package me.fru1t.stak.server.ktor

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.response.respondText
import me.fru1t.stak.server.models.Result

/**
 * Responds to a request using a [Result]. Http status will always be set to the
 * [Result.httpStatusCode]. If the [result] was successful, the response body will be set to the
 * output of [successText] with content type [successContentType]. If the [result] was unsuccessful,
 * the response body will be an empty string with the content type [ContentType.Text.Plain].
 */
suspend fun <T> ApplicationCall.respondResult(
    result: Result<T>,
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
