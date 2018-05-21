package me.fru1t.stak.server.routing

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import me.fru1t.stak.server.components.example.Example
import javax.inject.Inject

/** Routes for index. */
fun Route.index(indexHandler: IndexHandler) {
  get("*") {
    indexHandler.catchAll(call)
  }
  get("/") {
    indexHandler.catchAll(call)
  }
}

/** An inject-able helper class for the index routing table. */
class IndexHandler @Inject constructor(private val example: Example) {
  suspend fun catchAll(call: ApplicationCall) {
    example.example()
    call.respondText("Hello Index!", ContentType.Text.Html)
  }
}
