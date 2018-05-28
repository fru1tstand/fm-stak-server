package me.fru1t.stak.server.components.server.impl

import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.locations.Locations
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import me.fru1t.stak.server.components.server.Server
import me.fru1t.stak.server.routing.IndexHandler
import me.fru1t.stak.server.routing.UserHandler
import me.fru1t.stak.server.routing.index
import me.fru1t.stak.server.routing.user
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Default implementation of [Server]. */
class ServerImpl @Inject constructor(
    private val indexHandler: IndexHandler,
    private val userHandler: UserHandler) : Server {
  companion object {
    private const val THREAD_SLEEP_TIME_MS = 5000L
  }

  override fun run(onStart: (() -> Unit)?) {
    println("Starting server...")
    val engine = embeddedServer(Netty, 8080) {
      install(Locations)
      install(Authentication) {
        userHandler.registerAuthentication(this)
      }
      install(Routing) {
        // Enable console tracing
        trace { println(it.buildText()) }

        index(indexHandler)
        user()
      }
    }
    engine.start()
    onStart?.let { it() }
    println("Server started. Listening to localhost:8080.")

    try {
      // Hold server awake
      while (true) {
        Thread.sleep(THREAD_SLEEP_TIME_MS)
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
}
