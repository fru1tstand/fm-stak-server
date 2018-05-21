package me.fru1t.stak.server.components.example.impl

import me.fru1t.stak.server.components.example.Example
import javax.inject.Inject

/** The default implementation for [Example]. */
class ExampleImpl @Inject constructor() : Example {
  override fun example() {
    println("Example was called!")
  }
}
