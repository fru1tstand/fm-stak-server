package me.fru1t.stak.server

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
  // Bring up server
  val engine = embeddedServer(Netty, 8080) {
    routing {
      get("/") {
        call.respondText("Hello world!", ContentType.Text.Html)
      }
    }
  }
  println("Starting Stak server...")
  engine.start()
  println("Stak server started.")

  try {
    // Hold server awake
    while (true) {
      Thread.sleep(StakServer.THREAD_SLEEP_TIME)
    }
  } catch (e: InterruptedException) {
    println("Server thread was interrupted, triggering shutdown.")
  } finally {
    // Shut down server
    println("Stopping Stak server...")
    engine.stop(200, 500, TimeUnit.MILLISECONDS)
    println("Stak server stopped.")
  }
}

/** Internal interface for starting the [StakServer]. */
object StakServer {
  const val THREAD_SLEEP_TIME = 5000L

  fun start() {
    main(emptyArray())
  }
}
