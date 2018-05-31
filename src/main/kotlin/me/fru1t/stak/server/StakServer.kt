package me.fru1t.stak.server

import me.fru1t.stak.server.components.server.Server
import me.fru1t.stak.server.dagger.DaggerStakServerComponent
import javax.inject.Inject

// TODO(#1): Consume arguments to parse out binding port and ip.
fun main(args: Array<String>) {
  StakServer().run()
}

/**
 * Handles bootstrapping the stak server by building the dagger dependency graph and starting the
 * server.
 */
class StakServer : Runnable {
  @Inject lateinit var server: Server

  init {
    // Initialize the dagger dependency graph.
    DaggerStakServerComponent.create().inject(this)
  }

  /** Starts the stak server and blocks the current thread. See [Server.run]. */
  override fun run() {
    server.run()
  }
}
