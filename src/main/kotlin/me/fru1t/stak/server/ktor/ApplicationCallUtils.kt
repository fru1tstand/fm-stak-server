package me.fru1t.stak.server.ktor

import com.google.gson.JsonSyntaxException
import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respondText
import me.fru1t.stak.server.models.UserPrincipal
import kotlin.reflect.KClass

/**
 * Responds to a request with [httpStatusCode] passing an empty body and a
 * [ContentType.Text.Plain] `content-type`.
 */
suspend fun ApplicationCall.respondEmpty(httpStatusCode: HttpStatusCode = HttpStatusCode.OK) {
  respondText(text = "", contentType = ContentType.Text.Plain, status = httpStatusCode)
}

/**
 * Receives [T] from this [ApplicationCall] or responds with [HttpStatusCode.BadRequest] and returns
 * `null`. This method will catch json parsing errors and treat it as an empty file.
 */
suspend fun <T : Any> ApplicationCall.receiveOrBadRequest(expectedType: KClass<T>): T? {
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
suspend inline fun <reified T : Any> ApplicationCall.receiveOrBadRequest(): T? =
  receiveOrBadRequest(T::class)

/**
 * Retrieves the session principal casted to a [UserPrincipal]. This method should only be used
 * within a Bearer-authenticated route as it is not null-safe, 'nor type safe.
 */
fun ApplicationCall.getSessionUserPrincipal(): UserPrincipal =
  authentication.principal!! as UserPrincipal
