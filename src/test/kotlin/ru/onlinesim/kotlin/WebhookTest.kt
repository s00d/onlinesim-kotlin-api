package ru.onlinesim.kotlin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WebhookTest {
    @Test
    fun parsesAReceivingSmsBody() {
        val p =
            parseWebhook(
                """{"webhook_type":"receiving_sms","operation_id":1520,"user_id":2,"country_code":7,""" +
                    """"number":"+79001234567","sender":"VK","message":"VK: 195895 is your code","code":"195895",""" +
                    """"time_start":"2026-11-19 10:00:00","time_left":1100}""",
            )
        assertEquals("receiving_sms", p.webhookType())
        assertEquals(1520L, p.operationId())
        assertEquals(7, p.countryCode())
        assertEquals("195895", p.code())
        assertEquals(1100L, p.timeLeft())
    }

    @Test
    fun isLenient() {
        val p =
            parseWebhook(
                """{"webhook_type":"rent_sms","operation_id":"430","code":2991,"time_left":"soon","extra":true}""",
            )
        assertEquals("rent_sms", p.webhookType())
        assertEquals(430L, p.operationId())
        assertEquals("2991", p.code())
        assertNull(p.timeLeft())
        assertNull(p.userId())
    }

    @Test
    fun throwsOnlyForInputThatIsNotAJsonObject() {
        for (bad in listOf("not json", "[1,2]", "null", "")) {
            assertFailsWith<OnlineSimException> { parseWebhook(bad) }
        }
    }
}
