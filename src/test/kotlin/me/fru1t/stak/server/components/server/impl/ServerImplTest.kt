package me.fru1t.stak.server.components.server.impl

import dagger.Component
import me.fru1t.stak.server.ComponentsModule
import me.fru1t.stak.server.StakServerModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import kotlin.test.fail

@Singleton
@Component(modules = [StakServerModule::class, ComponentsModule::class])
interface ServerImplTestComponent {
  fun inject(serverImplTest: ServerImplTest)
}

class ServerImplTest {
  @Inject lateinit var serverImpl: ServerImpl

  @BeforeEach fun setUp() {
    DaggerServerImplTestComponent.create().inject(this)
  }

  @Test fun run() {
    // Bring up server
    val cdl = CountDownLatch(1)
    val serverThread = thread {
      serverImpl.run(cdl::countDown)
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
