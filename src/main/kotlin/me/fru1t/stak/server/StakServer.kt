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
  val engine = embeddedServer(Netty, 8080) {
    routing {
      get("/") {
        call.respondText("Hello world!", ContentType.Text.Html)
      }
    }
  }
  Runtime.getRuntime().addShutdownHook(object : Thread() {
    override fun run() {
      println("Stopping server...")
      engine.stop(5, 10, TimeUnit.SECONDS)
      println("Server was stopped gracefully! Goodnight.")
    }
  })
  println("Starting Stak server...")
  engine.start(wait = true)
}

object StakServer {
  fun start() {
    main(emptyArray())
  }
}
