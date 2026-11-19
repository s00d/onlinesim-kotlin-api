package ru.onlinesim.kotlin

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import java.net.ConnectException
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ClientTest {
    @Test
    fun sendsTheKeyAsBearerHeaderNeverInTheQueryPlusLangAndTheDefaultTimeout() =
        runTest {
            val f = Fake().json("""{"response":"1","price":0.36}""")
            f.client.numbers.price("vkcom", 44)
            val call = f.last()
            assertEquals("GET", call.method())
            assertEquals("Bearer test-key", call.headers()["Authorization"])
            assertEquals("en", Fake.queryOf(call)["lang"])
            assertFalse("dev_id" in Fake.queryOf(call) || "apikey" in Fake.queryOf(call))
            assertEquals(java.time.Duration.ofSeconds(30), call.timeout())
        }

    @Test
    fun appliesLangDevIdKotlinTimeoutAndABaseUrlWithoutTrailingSlash() =
        runTest {
            val f =
                Fake { t ->
                    OnlineSim(
                        apiKey = null,
                        baseUrl = "http://api.test/api",
                        lang = "ru",
                        devId = "42",
                        timeout = 5.seconds,
                        transport = t,
                        dispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
                    )
                }.json("""{"response":"1","price":1}""")
            f.client.numbers.price("vkcom")
            val call = f.last()
            assertEquals("http://api.test/api/getPrice.php", call.uri().toString().substringBefore('?'))
            assertEquals("ru", Fake.queryOf(call)["lang"])
            assertEquals("42", Fake.queryOf(call)["dev_id"])
            assertEquals(java.time.Duration.ofSeconds(5), call.timeout())
            assertFalse("Authorization" in call.headers())
        }

    @Test
    fun defaultsToTheOnlineSimHost() {
        assertEquals("https://onlinesim.host/api/", OnlineSim.DEFAULT_BASE_URL)
        assertEquals(30.seconds, OnlineSim.DEFAULT_TIMEOUT)
    }

    @Test
    fun runsTheBlockingCallsOnTheInjectedDispatcher() =
        runTest {
            Executors.newSingleThreadExecutor { Thread(it, "sdk-io") }.asCoroutineDispatcher().use { io ->
                val f = Fake(io).json("""{"response":"1","price":1}""")
                f.client.numbers.price("vkcom")
                assertEquals(listOf("sdk-io"), f.threads)
            }
        }

    @Test
    fun mapsNoNumberToNoNumberException() =
        runTest {
            val f = Fake().json("""{"response":"NO_NUMBER"}""")
            val e = assertFailsWith<NoNumberException> { f.client.numbers.get("google", 44) }
            assertIs<ApiException>(e)
            assertEquals("NO_NUMBER", e.code)
            assertEquals("No numbers available for this service right now", e.message)
        }

    @Test
    fun mapsHttp456WrongKeyToApiExceptionNotTransport() =
        runTest {
            val f = Fake().reply(456, """{"response":"ERROR_WRONG_KEY"}""")
            val e = assertFailsWith<ApiException> { f.client.user.balance() }
            assertEquals("ERROR_WRONG_KEY", e.code)
            assertEquals("Wrong API key", e.message)
        }

    @Test
    fun mapsAValidationObjectToValidationError() =
        runTest {
            val f = Fake().json("""{"response":{"webhook_url":["The webhook url must be a valid URL."]}}""")
            val e = assertFailsWith<ApiException> { f.client.user.setWebhookUrl("nope") }
            assertEquals("VALIDATION_ERROR", e.code)
            assertEquals("The webhook url must be a valid URL.", e.message)
        }

    @Test
    fun mapsNonJsonHttpErrorsAndNetworkFailuresToTransportException() =
        runTest {
            val f =
                Fake()
                    .reply(502, "<html>Bad gateway</html>")
                    .reply(500, """{"error":"boom"}""")
                    .fail(ConnectException("refused"))
            val nonJson = assertFailsWith<TransportException> { f.client.user.balance() }
            assertEquals(502, nonJson.status)
            assertEquals("<html>Bad gateway</html>", nonJson.body)
            assertEquals("HTTP 500", assertFailsWith<TransportException> { f.client.user.balance() }.message)
            val network = assertFailsWith<TransportException> { f.client.user.balance() }
            assertNull(network.status)
            assertTrue(network.message!!.contains("refused"))
        }

    @Test
    fun rejectsABadTimeout() {
        assertFailsWith<IllegalArgumentException> { OnlineSim(timeout = Duration.ZERO) }
    }
}
