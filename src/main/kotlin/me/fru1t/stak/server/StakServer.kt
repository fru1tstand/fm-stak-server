package me.fru1t.stak.server

import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import me.fru1t.stak.server.routing.IndexHandler
import me.fru1t.stak.server.routing.index
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// TODO(#1): Consume arguments to parse out binding port and ip.
fun main(args: Array<String>) {
  StakServer().run()
}

/**
 * Handles bootstrapping the stak server by building the dagger dependency graph and starting the
 * server.
 */
class StakServer {
  companion object {
    private const val THREAD_SLEEP_TIME = 5000L
  }

  @Inject lateinit var indexHandler: IndexHandler

  init {
    // Initialize the dagger dependency graph.
    DaggerStakServerComponent.builder().build().inject(this)
  }

  /**
   * Starts the stak server and blocks the thread. Shuts down the server gracefully when the thread
   * is interrupted. Calls the optional [onStart] callback after the server has started.
   */
  fun run(onStart: (() -> Unit)? = null) {
    println("Starting server...")
    val engine = embeddedServer(Netty, 8080) {
      routing {
        // Enable console tracing
        trace { println(it.buildText()) }

        // Install all other routes
        index(indexHandler)
      }
    }
    engine.start()
    onStart?.let { it() }
    println("Server started. Listening to localhost:8080.")

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
}
