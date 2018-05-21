package me.fru1t.stak.server.components.example

/** An example class to show how to use and bind components within Dagger. See ComponentsModule. */
interface Example {
  /**
   * An example interface method that will be overridden by the implementation and injected by
   * dagger when the graph is build.
   */
  fun example()
}
