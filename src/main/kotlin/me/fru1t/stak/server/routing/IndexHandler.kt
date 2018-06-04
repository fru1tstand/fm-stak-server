package me.fru1t.stak.server.routing

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
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
class IndexHandler @Inject constructor() {
  suspend fun catchAll(call: ApplicationCall) {
    call.respondText("Hello Index!", ContentType.Text.Html)
  }
}
