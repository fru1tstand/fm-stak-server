package me.fru1t.stak.server.testing.ktor

import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import java.net.URLEncoder
import java.util.Base64

/** Constants used within this utils file. */
private object Constants {
  /* Authorization header constants. */
  const val HEADER_AUTHORIZATION = "Authorization"
  const val AUTHORIZATION_CREDENTIALS_FORMAT = "%s:%s"
  const val AUTHORIZATION_BASIC_FORMAT = "Basic %s"

  /* Content-type header constants. */
  const val HEADER_CONTENT_TYPE = "content-type"
  const val CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded"
}

/**
 * Performs a [handleRequest] call automatically adding a
 * `content-type: application/x-www-form-urlencoded` header. that allows encoding of parameters in
 * the request body. This is required to use [TestApplicationRequest.setBody].
 */
fun TestApplicationEngine.handleFormRequest(
    method: HttpMethod,
    uri: String,
    setup: TestApplicationRequest.() -> Unit) = handleRequest(method, uri) {
  addHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_FORM_URLENCODED)
  setup(this)
}

/** Adds the basic authorization header to this request given a `username` and `password`. */
fun TestApplicationRequest.addBasicAuthorizationHeader(username: String, password: String) {
  val base64EncodedCredentials =
    Base64.getEncoder().encode(
        Constants.AUTHORIZATION_CREDENTIALS_FORMAT
            .format(username, password)
            .toByteArray(Charsets.UTF_8))
        ?.toString(Charsets.UTF_8)
  addHeader(
      Constants.HEADER_AUTHORIZATION,
      Constants.AUTHORIZATION_BASIC_FORMAT.format(base64EncodedCredentials))
}

/**
 * Builds and sets the requests body content. Requires the
 * `content-type: application/x-www-form-urlencoded` header, which can be automatically set by using
 * [TestApplicationEngine.handleFormRequest].
 */
fun TestApplicationRequest.setBody(builder: RequestBodyBuilder.() -> Unit) {
  val bodyBuilder = RequestBodyBuilder()
  builder(bodyBuilder)
  setBody(bodyBuilder.build())
}

/** Handles building the content of a request body. */
class RequestBodyBuilder {
  companion object {
    private const val PARAMETER_FORMAT = "%s=%s"

    /** URL encodes the string using [Charsets.UTF_8]. */
    private fun String.encode() = URLEncoder.encode(this, Charsets.UTF_8.name())
  }

  private val parameters: MutableMap<String, String> = HashMap()

  /** Adds a [key]-[value] parameter to the request. */
  fun addParameter(key: String, value: String) {
    parameters[key] = value
  }

  /** Returns the body content describing the current state of this builder. */
  fun build(): String =
    parameters.map { entry -> PARAMETER_FORMAT.format(entry.key.encode(), entry.value.encode()) }
        .joinToString(separator = "&")
}
