package xyz.tapcaferush.jglsrc

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import xyz.tapcaferush.jglsrc.entry.LaunchService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class GatewayRetryTest {
    private val launchService get() = LaunchService(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test fun productionAttemptsUseFifteenFifteenThirtySeconds() {
        assertEquals(listOf(15L, 15L, 30L), launchService.requestDeadlines)
    }

    @Test fun firstSuccessfulResponseDoesNotWaitOrRetry() = runBlocking {
        ReplyServer { 200 to "native" }.use { server ->
            val start = System.nanoTime()
            assertEquals("native", launchService.execute(server.address))
            assertEquals(1, server.requests.get())
            assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 5)
        }
    }

    @Test fun failedResponsesRetryUntilThirdSuccess() = runBlocking {
        ReplyServer { index -> if (index < 3) 503 to "" else 200 to "native" }.use { server ->
            assertEquals("native", launchService.execute(server.address))
            assertEquals(3, server.requests.get())
        }
    }

    @Test fun emptyResponsesOnlyFailAfterThirdAttempt() = runBlocking {
        ReplyServer { 200 to "  " }.use { server ->
            assertNull(launchService.execute(server.address))
            assertEquals(3, server.requests.get())
        }
    }

    @Test fun stalledRequestsRespectEachWholeCallDeadline() = runBlocking {
        ReplyServer { null }.use { server ->
            val start = System.nanoTime()
            // Shorter deadlines exercise the same call timeout path without a minute-long test.
            assertNull(launchService.execute(server.address, listOf(1L, 1L, 2L)))
            val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            assertEquals(3, server.requests.get())
            assertTrue("Returned before all deadlines: $elapsed", elapsed >= 3700)
            assertTrue("Whole-call deadlines exceeded: $elapsed", elapsed < 9000)
        }
    }

    private class ReplyServer(private val reply: (Int) -> Pair<Int, String>?) : AutoCloseable {
        private val server = ServerSocket(0)
        private val stopped = CountDownLatch(1)
        private val sockets = Collections.synchronizedList(mutableListOf<Socket>())
        val requests = AtomicInteger()
        val address = "http://127.0.0.1:${server.localPort}/entry"

        init {
            thread(isDaemon = true, name = "launchService-fixture") {
                while (!server.isClosed) {
                    val socket = try { server.accept() } catch (_: Exception) { break }
                    sockets.add(socket)
                    thread(isDaemon = true) {
                        socket.use {
                            try {
                                val reader = it.getInputStream().bufferedReader()
                                while (!reader.readLine().isNullOrEmpty()) { }
                                val response = reply(requests.incrementAndGet())
                                if (response == null) {
                                    stopped.await(10, TimeUnit.SECONDS)
                                } else {
                                    val (status, body) = response
                                    val bytes = body.toByteArray()
                                    val header = "HTTP/1.1 $status Result\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                                    it.getOutputStream().write(header.toByteArray() + bytes)
                                    it.getOutputStream().flush()
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
            }
        }

        override fun close() {
            server.close()
            stopped.countDown()
            synchronized(sockets) { sockets.forEach { runCatching { it.close() } } }
        }
    }
}
