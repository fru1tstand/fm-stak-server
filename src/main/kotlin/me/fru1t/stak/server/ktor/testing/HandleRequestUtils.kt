package me.fru1t.stak.server.ktor.testing

import com.google.common.io.CharStreams
import com.google.gson.Gson
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import kotlinx.coroutines.experimental.io.jvm.javaio.toInputStream
import org.jetbrains.annotations.TestOnly
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.Base64
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/** Constants used within this utils file. */
private object Constants {
  /* Authorization header constants. */
  const val HEADER_AUTHORIZATION = "Authorization"
  const val AUTHORIZATION_CREDENTIALS_FORMAT = "%s:%s"
  const val AUTHORIZATION_BASIC_FORMAT = "Basic %s"

  /* Content-type header constants. */
  const val HEADER_CONTENT_TYPE = "content-type"
  const val CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded"
  const val CONTENT_TYPE_JSON = "application/json"
}

/**
 * Performs a [handleRequest] call automatically adding a
 * `content-type: application/x-www-form-urlencoded` header. that allows encoding of parameters in
 * the request body. This is required to use [TestApplicationRequest.setBody].
 */
@TestOnly
fun TestApplicationEngine.handleFormRequest(
    method: HttpMethod,
    uri: String,
    setup: TestApplicationRequest.() -> Unit) = handleRequest(method, uri) {
  addHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_FORM_URLENCODED)
  setup()
}

/**
 * Performs a [handleRequest] call setting the `content-type` header to `application/json` and
 * encoding the given [data] as JSON in the request body.
 */
@TestOnly
fun TestApplicationEngine.handleJsonRequest(
    data: Any,
    gson: Gson,
    method: HttpMethod,
    uri: String,
    setup: TestApplicationRequest.() -> Unit) = handleRequest(method, uri) {
  addHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
  setBody(gson.toJson(data))
  setup()
}

/** Adds the basic authorization header to this request given a [username] and [password]. */
@TestOnly
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
@TestOnly
fun TestApplicationRequest.setBody(builder: RequestBodyBuilder.() -> Unit) {
  val bodyBuilder = me.fru1t.stak.server.ktor.testing.RequestBodyBuilder()
  builder(bodyBuilder)
  setBody(bodyBuilder.build())
}

/** Retrieves the body of this [TestApplicationCall]. */
@TestOnly
fun TestApplicationCall.getBody() : String =
  CharStreams.toString(InputStreamReader(request.bodyChannel.toInputStream(), Charsets.UTF_8))

/** Handles building the content of a request body. */
class RequestBodyBuilder @TestOnly internal constructor() {
  companion object {
    private const val PARAMETER_FORMAT = "%s=%s"

    /** URL encodes the string using [Charsets.UTF_8]. */
    private fun String.encode() = URLEncoder.encode(this, Charsets.UTF_8.name())
  }

  private val parameters: MutableMap<String, String> = HashMap()

  /** Adds a [key]-[value] parameter to the request. */
  @TestOnly
  fun addParameter(key: String, value: String) {
    parameters[key] = value
  }

  /**
   * Serializes the primary constructor parameters within [data] into this request body in the form
   * of `parameter name`-`value` pairs. This works best with data classes.
   */
  @TestOnly
  fun addData(data: Any) {
    // For each constructor parameter
    for (kParameter in data.javaClass.kotlin.primaryConstructor!!.parameters) {
      // Find the backing field
      val kProperty1 =
        data.javaClass.kotlin.declaredMemberProperties.find { it.name == kParameter.name!! }!!
      // Make sure we have access rights
      if (!kProperty1.isAccessible) {
        kProperty1.isAccessible = true
      }
      // And add it to this body builder
      addParameter(kProperty1.name, kProperty1.get(data).toString())
    }
  }

  /** Returns the body content describing the current state of this builder. */
  @TestOnly
  fun build(): String =
    parameters.map { entry -> PARAMETER_FORMAT.format(entry.key.encode(), entry.value.encode()) }
        .joinToString(separator = "&")
}
