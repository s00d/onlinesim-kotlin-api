package ru.onlinesim.kotlin

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumbersTest {
    @Test
    fun price() =
        runTest {
            val f = Fake().json("""{"response":"1","price":0.36}""")
            assertEquals(0.36, f.client.numbers.price("vkcom", 44))
            assertEquals("/api/getPrice.php", f.path())
            assertEquals(mapOf("service" to "vkcom", "country" to "44"), f.query())
        }

    @Test
    fun priceDefaultsToCountry7AndIsNullWhenTheServerHasNone() =
        runTest {
            val f = Fake().json("""{"response":"1","price":null}""")
            assertNull(f.client.numbers.price("vkcom"))
            assertEquals("7", f.query()["country"])
        }

    @Test
    fun getSendsNoNumberFlagAndRejectAsACommaList() =
        runTest {
            val f = Fake().json("""{"response":1,"tzid":1520}""")
            assertEquals(1520, f.client.numbers.get("google", 44, reject = listOf("4470", "4471")))
            assertEquals("/api/getNum.php", f.path())
            assertEquals(mapOf("service" to "google", "country" to "44", "reject" to "4470,4471"), f.query())
        }

    @Test
    fun getDefaults() =
        runTest {
            val f = Fake().json("""{"response":1,"tzid":1}""")
            f.client.numbers.get("google")
            assertEquals(mapOf("service" to "google", "country" to "7"), f.query())
        }

    @Test
    fun getWithNumberSendsNumber1AndReturnsTheOrder() =
        runTest {
            val f =
                Fake().json(
                    """{"response":1,"tzid":1520,"number":"+447459608320","country":44,""" +
                        """"time":1200,"service":"google"}""",
                )
            val order = f.client.numbers.getWithNumber("google", 44)
            assertEquals(1520, order.tzid())
            assertEquals("+447459608320", order.number())
            assertEquals(44, order.country())
            assertEquals(1200, order.time())
            assertEquals("google", order.service())
            assertEquals(mapOf("service" to "google", "country" to "44", "number" to "1"), f.query())
        }

    @Test
    fun stateSendsBooleansAs10LowercaseOrderbyAndTheNumberFlag() =
        runTest {
            val f = Fake().json(Fake.fixture("getState"))
            val ops = f.client.numbers.state(orderBy = OrderBy.DESC, msgList = true)
            assertEquals(2, ops.size)
            assertEquals(594, ops[0].tzid())
            assertEquals("195895", ops[0].msg())
            assertEquals(120, ops[1].guardIntervalRemainingSeconds())
            assertEquals("/api/getState.php", f.path())
            assertEquals(
                mapOf("message_to_code" to "1", "orderby" to "desc", "msg_list" to "1", "number" to "1"),
                f.query(),
            )
        }

    @Test
    fun stateDefaultsAndEmptyOnErrorNoOperations() =
        runTest {
            val f = Fake().json("""{"response":"ERROR_NO_OPERATIONS"}""")
            assertTrue(
                f.client.numbers
                    .state()
                    .isEmpty(),
            )
            assertEquals(
                mapOf("message_to_code" to "1", "orderby" to "asc", "msg_list" to "0", "number" to "1"),
                f.query(),
            )
        }

    @Test
    fun stateOne() =
        runTest {
            val f = Fake().json("""[{"tzid":594,"response":"TZ_NUM_ANSWER","msg":"VK: 195895"}]""")
            assertEquals(
                "VK: 195895",
                f.client.numbers
                    .stateOne(594, messageToCode = false)
                    .msg(),
            )
            assertEquals(
                mapOf("tzid" to "594", "message_to_code" to "0", "msg_list" to "0", "number" to "1"),
                f.query(),
            )
        }

    @Test
    fun stateOneWithMsgList() =
        runTest {
            val f = Fake().json("""[{"tzid":5,"response":"TZ_NUM_ANSWER","msg":[{"service":"vk","msg":"1234"}]}]""")
            val op = f.client.numbers.stateOne(5, msgList = true)
            assertEquals("1234", op.msgList()[0].msg())
            assertEquals("1", f.query()["msg_list"])
        }

    @Test
    fun stateOneRaisesErrorNoOperations() =
        runTest {
            val f = Fake().json("[]")
            assertEquals("ERROR_NO_OPERATIONS", assertFailsWith<ApiException> { f.client.numbers.stateOne(1) }.code)
        }

    @Test
    fun nextCloseBan() =
        runTest {
            val f = Fake().json("""{"response":"1"}""").json("""{"response":1}""").json("""{"response":1}""")
            f.client.numbers.next(1520)
            f.client.numbers.close(1520)
            f.client.numbers.ban(1520)
            assertEquals(
                listOf(
                    "/api/setOperationRevise.php?tzid=1520&lang=en",
                    "/api/setOperationOk.php?tzid=1520&lang=en",
                    "/api/setOperationOk.php?tzid=1520&ban=1&lang=en",
                ),
                f.calls.map { "${it.uri().path}?${it.uri().rawQuery}" },
            )
        }

    @Test
    fun closeSurfacesNoCompleteTzid() =
        runTest {
            val f = Fake().json("""{"response":"NO_COMPLETE_TZID"}""")
            assertEquals("NO_COMPLETE_TZID", assertFailsWith<ApiException> { f.client.numbers.close(1) }.code)
        }

    @Test
    fun tariffs() =
        runTest {
            val f = Fake().json("""{"44":${Fake.fixture("getNumbersStats_44")}}""")
            val all = f.client.numbers.tariffs()
            assertEquals("/api/getNumbersStats.php", f.path())
            assertEquals(mapOf("country" to "all"), f.query())
            assertEquals(
                "vkcom",
                all
                    .getValue("44")
                    .services()
                    .getValue("service_vkcom")
                    .slug(),
            )
        }

    @Test
    fun tariffsOne() =
        runTest {
            val f = Fake().json(Fake.fixture("getNumbersStats_44"))
            val uk = f.client.numbers.tariffsOne(44)
            assertEquals(mapOf("country" to "44"), f.query())
            assertEquals(44, uk.code())
            assertEquals(0.36, uk.services().getValue("service_vkcom").price())
        }
}
