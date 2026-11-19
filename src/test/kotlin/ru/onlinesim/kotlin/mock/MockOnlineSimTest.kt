package ru.onlinesim.kotlin.mock

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import ru.onlinesim.kotlin.ApiException
import ru.onlinesim.kotlin.NoNumberException
import ru.onlinesim.kotlin.OnlineSim
import ru.onlinesim.kotlin.OrderBy
import ru.onlinesim.kotlin.TransportException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

/** The Kotlin MockOnlineSim through the Kotlin client: options, every control helper, the main flows. */
class MockOnlineSimTest {
    /** A client on runTest's scheduler: virtual time, no real threads. */
    private fun TestScope.client(
        mock: MockOnlineSim,
        apiKey: String? = mock.apiKey,
    ) = mock.client(apiKey = apiKey, dispatcher = StandardTestDispatcher(testScheduler))

    private suspend fun code(call: suspend () -> Unit) = assertFailsWith<ApiException> { call() }.code

    @Test
    fun wrongOrMissingKeyIsApiExceptionTariffsAndFreeNeedNone() =
        runTest {
            val mock = MockOnlineSim(apiKey = "secret")
            assertEquals("ERROR_WRONG_KEY", code { client(mock, "nope").user.balance() })
            val anonymous = client(mock, apiKey = null)
            assertEquals("ERROR_WRONG_KEY", code { anonymous.user.balance() })
            assertEquals(44, anonymous.numbers.tariffsOne(44).code())
            assertEquals(7, anonymous.rent.tariffsOne().code())
            assertEquals(
                44,
                anonymous.free
                    .list()
                    .messages()
                    .country(),
            )
            assertEquals("secret", client(mock).user.profile().apikey())
        }

    @Test
    fun namedOptionsAndAHandBuiltClientOnTheTransport() =
        runTest {
            val mock =
                MockOnlineSim(
                    apiKey = "k",
                    balance = 5.0,
                    prices = mapOf(1 to mapOf("x" to 1.5)),
                    rentTariffs = mapOf(1 to MockRentTariff(mapOf(2 to 3.0), false)),
                )
            val client = OnlineSim("k", "https://anything.test/v2/api/", transport = mock.transport)
            assertEquals(5.0, client.user.balance().balance())
            assertEquals(1.5, client.numbers.price("x", 1))
            assertEquals(3.0, client.rent.tariffsOne(1).days()["2"])
            assertEquals("UNDEFINED_COUNTRY", code { client.numbers.price("google", 7) })
            val req = mock.requests()[0]
            assertEquals("GET", req.method())
            assertEquals("getBalance.php", req.path())
            assertEquals(mapOf("income" to "1", "lang" to "en"), req.query())
            assertEquals("https://mock.onlinesim.invalid/api/", MockOnlineSim.MOCK_BASE_URL)
        }

    @Test
    fun orderAddSmsAfterWaitCodeWithZeroIntervalNoTimePasses() =
        runTest {
            val mock = MockOnlineSim(balance = 10.0)
            val client = client(mock)
            val tzid = client.numbers.get("google", 44)
            mock.addSmsAfter(tzid, "Your code: 123-456", 2)
            assertEquals("123456", client.numbers.waitCode(tzid, interval = Duration.ZERO))
            assertEquals(0, currentTime)
            assertEquals(
                listOf("getNum.php", "getState.php", "getState.php", "getState.php", "setOperationOk.php"),
                mock.requests().map { it.path() },
            )
            assertEquals(9.8, mock.balance())
            assertEquals("TZ_OVER_OK", mock.operations()[0].status())
        }

    @Test
    fun waitCodeOnVirtualTimeWithTheDefaultInterval() =
        runTest {
            val mock = MockOnlineSim()
            val client = client(mock)
            val tzid = client.numbers.get("google", 44)
            mock.addSmsAfter(tzid, "4321", 2, service = "google")
            assertEquals("4321", client.numbers.waitCode(tzid, keepOpen = true))
            assertEquals(10_000, currentTime)
            assertEquals("google", mock.operations()[0].messages()[0].service())
            assertEquals("TZ_NUM_WAIT", mock.operations()[0].status()) // kept open by next
        }

    @Test
    fun addSmsStateNextCloseBan() =
        runTest {
            val mock = MockOnlineSim()
            val client = client(mock)
            val a = client.numbers.get("telegram")
            val b = client.numbers.getWithNumber("vkcom").tzid()
            mock.addSms(a, "Welcome!", service = "telegram")
            assertEquals("Welcome!", client.numbers.stateOne(a).msg())
            client.numbers.next(a)
            mock.addSms(a, "code 98765")
            assertEquals("98765", client.numbers.stateOne(a).msg())
            assertEquals(
                2,
                client.numbers
                    .stateOne(a, msgList = true)
                    .msgList()
                    .size,
            )
            assertEquals(listOf(b, a), client.numbers.state(orderBy = OrderBy.DESC).map { it.tzid() })
            assertEquals("0", code { client.numbers.ban(a) }) // ban after an SMS
            client.numbers.close(a)
            client.numbers.ban(b)
            assertEquals(listOf("TZ_OVER_OK", "TZ_OVER_OK"), mock.operations().map { it.status() })
            assertFailsWith<IllegalStateException> { mock.addSms(999, "x") }
        }

    @Test
    fun lowBalanceRejectAndGuardInterval() =
        runTest {
            val mock = MockOnlineSim(balance = 0.25, guardInterval = 120)
            val client = client(mock)
            assertEquals("WARNING_LOW_BALANCE", code { client.numbers.get("google", 7) })
            assertFailsWith<NoNumberException> { client.numbers.get("google", 44, reject = listOf("4490")) }
            val tzid = client.numbers.get("google", 44)
            assertEquals(120, client.numbers.stateOne(tzid).guardIntervalRemainingSeconds())
            assertEquals("NO_COMPLETE_TZID", code { client.numbers.close(tzid) })
            mock.setBalance(50.0)
            assertEquals(50.0, client.user.balance().balance())
        }

    @Test
    fun failNextAndFailNextTransport() =
        runTest {
            val mock = MockOnlineSim()
            val client = client(mock)
            mock.failNext("TRY_AGAIN_LATER", endpoint = "getNum.php")
            client.user.balance() // another endpoint: not affected
            assertEquals("TRY_AGAIN_LATER", code { client.numbers.get("google") })
            mock.failNext("NO_NUMBER")
            assertFailsWith<NoNumberException> { client.user.balance() }
            mock.failNextTransport()
            assertEquals(502, assertFailsWith<TransportException> { client.user.balance() }.status)
            mock.failNextTransport(status = 503, endpoint = "getBalance.php")
            assertEquals(503, assertFailsWith<TransportException> { client.user.balance() }.status)
        }

    @Test
    fun rentFlowAndRents() =
        runTest {
            val mock = MockOnlineSim(balance = 10.0)
            val client = client(mock)
            val rent = client.rent.get(days = 3)
            assertEquals(7.5, mock.balance())
            mock.addSms(rent.tzid(), "Bank: 777 888")
            assertEquals(
                "777888",
                client.rent
                    .stateOne(rent.tzid())
                    .messages()[0]
                    .code(),
            )
            assertEquals(4, client.rent.extend(rent.tzid()).days())
            assertEquals(listOf(rent.tzid()), mock.rents().map { it.tzid() })
            client.rent.close(rent.tzid())
            assertTrue(mock.rents().isEmpty())
        }

    @Test
    fun webhookUrlLogsAndReset() =
        runTest {
            val mock = MockOnlineSim()
            val client = client(mock)
            assertEquals("VALIDATION_ERROR", code { client.user.setWebhookUrl("ftp://x") })
            client.user.setWebhookUrl("https://example.com/hook")
            val tzid = client.numbers.get("google", 44)
            mock.addSms(tzid, "code 1234")
            assertEquals(
                "1234",
                client.user
                    .webhookLogs()
                    .data()[0]
                    .params()
                    .code(),
            )
            client.user.clearWebhookUrl()
            assertNull(client.user.profile().webhookUrl())
            mock.reset()
            assertTrue(mock.operations().isEmpty() && mock.requests().isEmpty())
            assertEquals(100.0, mock.balance())
        }
}
