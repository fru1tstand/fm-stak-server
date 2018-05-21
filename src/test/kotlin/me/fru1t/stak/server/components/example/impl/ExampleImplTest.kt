package me.fru1t.stak.server.components.example.impl

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExampleImplTest {
  private lateinit var exampleImpl: ExampleImpl

  @BeforeEach fun setUp() {
    exampleImpl = ExampleImpl()
  }

  @Test fun example() {
    exampleImpl.example()
    // Not much to test here...
  }
}
