package me.fru1t.stak.server.components.server

/** Handles preparing and starting up the StakServer with proper routing. */
interface Server {
  /**
   * Starts the stak server and blocks the thread. Shuts down the server gracefully when the thread
   * is interrupted. Calls the optional [onStart] callback after the server has started.
   */
  fun run(onStart: (() -> Unit)? = null)
}
