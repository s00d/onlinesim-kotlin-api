package ru.onlinesim.kotlin

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.net.URI
import java.net.http.HttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Opt-in integration test against a LOCAL backend. Skipped unless both are set:
 * `ONLINESIM_IT_BASE=https://on.test/api/ ONLINESIM_IT_KEY=<apikey>`. With `ONLINESIM_IT_MOCK=1` it also runs the
 * number lifecycle on the provider-free `test` driver (`spec/verify/backend_mock.php setup` first, a key of a user
 * without webhook_url, teardown after). Real time here (a real backend), unlike the unit tests.
 */
@EnabledIfEnvironmentVariable(named = "ONLINESIM_IT_BASE", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ONLINESIM_IT_KEY", matches = ".+")
class IntegrationTest {
    private companion object {
        val BASE: String = System.getenv("ONLINESIM_IT_BASE") ?: ""
        val KEY: String = System.getenv("ONLINESIM_IT_KEY") ?: ""

        /** Test-only: local backends use a self-signed certificate. Never part of the SDK API. */
        val transport: Transport by lazy {
            val host = URI.create(BASE).host
            check(host == "localhost" || host == "127.0.0.1" || host.endsWith(".test") || host.endsWith(".localhost")) {
                "refusing non-local integration host $host"
            }
            val trustAll =
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String,
                    ) = Unit

                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String,
                    ) = Unit

                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }
            val ssl =
                SSLContext
                    .getInstance(
                        "TLS",
                    ).apply { init(null, arrayOf<TrustManager>(trustAll), SecureRandom()) }
            HttpTransport(HttpClient.newBuilder().sslContext(ssl).build())
        }
        val client: OnlineSim by lazy { OnlineSim(KEY, BASE, transport = transport) }
    }

    @Test
    fun readOnlyCalls() =
        runBlocking {
            val b = client.user.balance()
            assertTrue(b.balance().isFinite() && b.zbalance().isFinite())
            assertTrue(client.user.profile().id() > 0)
            assertEquals(44, client.numbers.tariffsOne(44).code())
            assertNotNull(client.numbers.state())
            assertEquals(7, client.rent.tariffsOne(7).code())
            client.rent.state().forEach { assertNotNull(it.extend()) }
            assertNotNull(
                client.user
                    .paymentHistory()
                    .orders()
                    .data(),
            )
            assertNotNull(client.user.webhookLogs().data())
            assertNotNull(client.free.list().countries())
        }

    @Test
    fun wrongKeyIsApiException() =
        runBlocking {
            val bad = OnlineSim("wrong-key", BASE, transport = transport)
            assertEquals("ERROR_WRONG_KEY", assertFailsWith<ApiException> { bad.user.balance() }.code)
        }

    @Nested
    @EnabledIfEnvironmentVariable(named = "ONLINESIM_IT_MOCK", matches = "1")
    inner class Lifecycle {
        /** Injects an SMS through the test driver callback (local backends only, no auth). */
        private fun injectSms(
            tzid: Long,
            text: String,
        ) {
            val res =
                transport.send(
                    TransportRequest(
                        "POST",
                        URI.create(BASE).resolve("sms_in_getaways/test"),
                        mapOf("Content-Type" to "application/json", "Accept" to "application/json"),
                        """{"id":$tzid,"phoneFrom":"VK","text":"$text"}""",
                        java.time.Duration.ofSeconds(30),
                    ),
                )
            assertEquals(200, res.status())
        }

        @Test
        fun priceGetGetWithNumberStateSmsWaitCodeClose() =
            runBlocking {
                assertNotNull(client.numbers.price("google", 44))
                val tzid = client.numbers.get("google", 44)
                val order = client.numbers.getWithNumber("google", 44)
                assertTrue(Regex("^\\+?44\\d+$").matches(order.number()))
                val ids = client.numbers.state().map { it.tzid() }
                assertTrue(tzid in ids && order.tzid() in ids)
                assertTrue(
                    client.numbers
                        .stateOne(tzid)
                        .response()
                        .startsWith("TZ_"),
                )
                // The test driver answers NO_NUMBER to any reject list: proves `reject` is sent.
                assertFailsWith<NoNumberException> { client.numbers.get("google", 44, reject = listOf("4470")) }
                assertEquals(
                    "NO_COMPLETE_TZID",
                    assertFailsWith<ApiException> { client.numbers.close(order.tzid()) }.code,
                )
                injectSms(tzid, "VK: 195895 is your code")
                assertEquals("195895", client.numbers.waitCode(tzid, interval = 1.seconds, timeout = 30.seconds))
            }

        @Test
        fun setWebhookUrlValidationErrorClearWebhookUrl() =
            runBlocking {
                client.user.setWebhookUrl("https://example.com/hook")
                assertEquals("https://example.com/hook", client.user.profile().webhookUrl())
                assertEquals(
                    "VALIDATION_ERROR",
                    assertFailsWith<ApiException> { client.user.setWebhookUrl("not a url") }.code,
                )
                client.user.clearWebhookUrl()
                assertNull(client.user.profile().webhookUrl())
            }
    }
}
