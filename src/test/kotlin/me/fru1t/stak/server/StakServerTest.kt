package me.fru1t.stak.server

import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.fail

class StakServerTest {
  @Test fun run() {
    // Bring up server
    val cdl = CountDownLatch(1)
    val serverThread = thread {
      StakServer().run(cdl::countDown)
    }
    if (!cdl.await(5, TimeUnit.SECONDS)) {
      fail("Server never came up.")
    }

    // Verify we can ping it
    val socketAddress = InetSocketAddress("localhost", 8080)
    val socket = Socket()
    try {
      socket.connect(socketAddress, 5000)
    } catch (e: SocketTimeoutException) {
      fail("Couldn't connect to the server after 5 seconds: ${e.message}")
    } finally {
      socket.close()
    }
    serverThread.interrupt()
  }
}
