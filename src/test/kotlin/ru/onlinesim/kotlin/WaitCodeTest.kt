package ru.onlinesim.kotlin

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.ConnectException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** waitCode on runTest's virtual time: `delay` only moves the scheduler's clock, nothing really waits. */
class WaitCodeTest {
    private fun TestScope.fake() = Fake(StandardTestDispatcher(testScheduler), clock = { currentTime })

    private fun Fake.waiting() = json("""[{"tzid":7,"response":"TZ_NUM_WAIT"}]""")

    private fun Fake.answer(msg: String) = json("""[{"tzid":7,"response":"TZ_NUM_ANSWER","msg":"$msg"}]""")

    private fun Fake.ok() = json("""{"response":1}""")

    /** Each request as `path@virtual ms`. */
    private fun Fake.log() = calls.map { it.uri().path.removePrefix("/api/") }.zip(times) { p, t -> "$p@$t" }

    @Test
    fun returnsTheCodeOnceItArrivesAndClosesTheOperation() =
        runTest {
            val f =
                fake()
                    .waiting()
                    .waiting()
                    .answer("195895")
                    .ok()
            assertEquals("195895", f.client.numbers.waitCode(7))
            assertEquals(
                listOf("getState.php@0", "getState.php@5000", "getState.php@10000", "setOperationOk.php@10000"),
                f.log(),
            )
            assertEquals("1", Fake.queryOf(f.calls[0])["message_to_code"])
        }

    @Test
    fun keepOpenCallsNextSinceSkipsThePreviousCodeFullMessageAsksForText() =
        runTest {
            val f = fake().answer("old").answer("Your code is 2991").ok()
            val code =
                f.client.numbers.waitCode(
                    7,
                    interval = 1.seconds,
                    keepOpen = true,
                    since = "old",
                    fullMessage = true,
                )
            assertEquals("Your code is 2991", code)
            assertEquals(1000, currentTime)
            assertEquals(
                "/api/setOperationRevise.php",
                f.calls
                    .last()
                    .uri()
                    .path,
            )
            assertEquals("0", Fake.queryOf(f.calls[0])["message_to_code"])
        }

    @ParameterizedTest
    @ValueSource(strings = ["TZ_OVER_OK", "ERROR_NO_ITEMS"])
    fun raisesApiExceptionAtOnceForAFinishedOperation(status: String) =
        runTest {
            val f = fake().json("""[{"tzid":7,"response":"$status"}]""")
            assertEquals(status, assertFailsWith<ApiException> { f.client.numbers.waitCode(7) }.code)
            assertEquals(1, f.calls.size)
            assertEquals(0, currentTime)
        }

    @Test
    fun raisesWaitTimeoutWithALastPollAtTheDeadline() =
        runTest {
            val f = fake()
            repeat(4) { f.waiting() }
            assertFailsWith<WaitTimeoutException> {
                f.client.numbers.waitCode(
                    7,
                    interval = 4.seconds,
                    timeout = 10.seconds,
                )
            }
            assertEquals(listOf(0L, 4000L, 8000L, 10_000L), f.times) // the last poll at the deadline
            assertEquals(10_000, currentTime)
        }

    @Test
    fun retriesATransientTransportException() =
        runTest {
            val f = fake().fail(ConnectException("reset")).answer("42").ok()
            assertEquals("42", f.client.numbers.waitCode(7))
            assertEquals(5000, currentTime)
        }

    @Test
    fun raisesAnyApiExceptionAtOnce() =
        runTest {
            val f = fake().json("""{"response":"ERROR_NO_OPERATIONS"}""")
            assertEquals("ERROR_NO_OPERATIONS", assertFailsWith<ApiException> { f.client.numbers.waitCode(7) }.code)
            assertEquals(1, f.calls.size)
        }

    @Test
    fun zeroIntervalPollsBackToBackAndBadArgumentsAreRejected() =
        runTest {
            val f =
                fake()
                    .waiting()
                    .waiting()
                    .answer("1")
                    .ok()
            assertEquals("1", f.client.numbers.waitCode(7, interval = Duration.ZERO))
            assertEquals(0, currentTime)
            assertFailsWith<IllegalArgumentException> { f.client.numbers.waitCode(7, interval = (-1).seconds) }
            assertFailsWith<IllegalArgumentException> { f.client.numbers.waitCode(7, timeout = Duration.ZERO) }
        }
}
