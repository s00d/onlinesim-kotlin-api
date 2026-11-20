package ru.onlinesim.kotlin

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Shapes of the regenerated API spec decoded through the client. */
class SpecShapesTest {
    @Test
    fun aStateItemWithoutResponseDecodes() =
        runTest {
            val op =
                Fake()
                    .json("""[{"tzid":7}]""")
                    .client.numbers
                    .stateOne(7)
            assertEquals(7L, op.tzid())
            assertNull(op.response())
        }

    @Test
    fun rentNotifySmsCodeZeroIsTheString0AndSumIsANumberOrADecimalString() =
        runTest {
            val item =
                """{"tzid":5,"status":1,"number":"79001112233","country":7,"sum":2.4,"messages":""" +
                    """[{"id":1,"service":"notify","text":"x","code":0,"created_at":"2026-01-01 00:00:00"}]}"""
            val f =
                Fake()
                    .json("""{"response":1,"item":$item}""")
                    .json("""{"response":1,"list":[${item.replace("2.4", "\"2.40\"")}]}""")
            val rent = f.client.rent.get()
            assertEquals("0", rent.messages()[0].code())
            assertEquals(2.4, rent.sum())
            assertEquals(
                2.4,
                f.client.rent
                    .stateOne(5)
                    .sum(),
            )
        }

    @Test
    fun nullablePaymentOrderAndFreeCountryFields() =
        runTest {
            val f =
                Fake()
                    .json(
                        """{"response":1,"orders":{"data":[{"id":1,"sum":"1.00","currency":null,"form_text":null}],""" +
                            """"current_page":1,"last_page":1,"total":1},"paylist":{}}""",
                    ).json(
                        """{"response":1,"countries":[{"country":44,"country_text":null,"country_original":null}],""" +
                            """"numbers":{},"messages":{"data":[],"current_page":1,"last_page":1,"total":0}}""",
                    )
            val order =
                f.client.user
                    .paymentHistory()
                    .orders()
                    .data()[0]
            assertNull(order.currency())
            assertNull(order.formText())
            val country =
                f.client.free
                    .list()
                    .countries()[0]
            assertNull(country.countryText())
            assertNull(country.countryOriginal())
        }
}
