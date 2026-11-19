package ru.onlinesim.kotlin

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserTest {
    @Test
    fun balanceSendsIncome1AndParsesTheDecimalString() =
        runTest {
            val f = Fake().json(Fake.fixture("getBalance"))
            val b = f.client.user.balance()
            assertEquals(1442.33, b.balance())
            assertEquals(1023.41, b.zbalance())
            assertEquals(9.78, b.income())
            assertEquals("/api/getBalance.php", f.path())
            assertEquals(mapOf("income" to "1"), f.query())
        }

    @Test
    fun profile() =
        runTest {
            val f = Fake().json(Fake.fixture("getProfile"))
            val p = f.client.user.profile()
            assertEquals("/api/getProfile.php", f.path())
            assertEquals(1, p.id())
            assertEquals("https://example.com/hook", p.webhookUrl())
        }

    @Test
    fun paymentHistory() =
        runTest {
            val f = Fake().json(Fake.fixture("getPaymentHistory")).json(Fake.fixture("getPaymentHistory"))
            val h = f.client.user.paymentHistory(page = 2)
            f.client.user.paymentHistory()
            assertEquals("/api/getPaymentHistory.php", f.calls[0].uri().path)
            assertEquals(listOf("2", "1"), f.calls.map { Fake.queryOf(it)["page"] })
            assertTrue(h.paylist().isEmpty())
            assertEquals(106, h.orders().total())
        }

    @Test
    fun setAndClearWebhookUrlPostJson() =
        runTest {
            val f = Fake().json("""{"response":"1","user":{"id":2}}""").json("""{"response":"1","user":{"id":2}}""")
            f.client.user.setWebhookUrl("https://example.com/hook")
            f.client.user.clearWebhookUrl()
            val (set, clear) = f.calls
            assertEquals("POST", set.method())
            assertEquals("/api/profile.php", set.uri().path)
            assertEquals("application/json", set.headers()["Content-Type"])
            assertEquals("""{"profile":{"webhook_url":"https://example.com/hook"}}""", set.body())
            assertEquals("""{"profile":{"webhook_url":null}}""", clear.body())
        }

    @Test
    fun webhookLogs() =
        runTest {
            val page =
                """{"current_page":1,"data":[{"id":7,"user_id":2,"type":"receiving_sms",""" +
                    """"params":{"operation_id":1520,"code":"195895"},"status":"success"}],""" +
                    """"last_page":1,"per_page":10,"total":1}"""
            val f = Fake().json("""{"response":"1","data":$page}""")
            val logs = f.client.user.webhookLogs()
            assertEquals("/api/webhook-logs", f.path())
            assertEquals(mapOf("page" to "1"), f.query())
            assertEquals("195895", logs.data()[0].params().code())
        }
}
