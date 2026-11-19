package ru.onlinesim.kotlin

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RentTest {
    private val detail = mapOf("pagination" to "0", "number" to "1")

    /** `{"response":1,"item":<list[index] of the rent_getRentState fixture>}` */
    private fun withItem(index: Int): String {
        val list =
            com.google.gson.JsonParser
                .parseString(Fake.fixture("rent_getRentState"))
                .asJsonObject["list"]
        return """{"response":1,"item":${list.asJsonArray[index]}}"""
    }

    @Test
    fun getSendsExtensionExplicitlyPaginationAndNumber() =
        runTest {
            val f = Fake().json(withItem(0))
            val rent = f.client.rent.get(7, days = 3)
            assertEquals("/api/rent/getRentNum.php", f.path())
            assertEquals(mapOf("country" to "7", "days" to "3", "extension" to "1") + detail, f.query())
            assertEquals(5.14, rent.sum())
            assertEquals("195895", rent.messages()[0].code())
        }

    @Test
    fun getDefaultsAndExtensionFalse() =
        runTest {
            val f = Fake().json(withItem(1)).json(withItem(1))
            f.client.rent.get()
            f.client.rent.get(extension = false)
            assertEquals(
                listOf(
                    mapOf("country" to "7", "days" to "1", "extension" to "1") + detail,
                    mapOf("country" to "7", "days" to "1", "extension" to "0") + detail,
                ),
                f.calls.map { Fake.queryOf(it) - "lang" },
            )
        }

    @Test
    fun state() =
        runTest {
            val f = Fake().json(Fake.fixture("rent_getRentState"))
            val list = f.client.rent.state()
            assertEquals("/api/rent/getRentState.php", f.path())
            assertEquals(detail, f.query())
            assertEquals(2, list.size)
            assertTrue(list[1].extend().isEmpty())
        }

    @Test
    fun stateOne() =
        runTest {
            val f = Fake().json(Fake.fixture("rent_getRentState"))
            assertEquals(
                430,
                f.client.rent
                    .stateOne(430)
                    .tzid(),
            )
            assertEquals(mapOf("tzid" to "430") + detail, f.query())
        }

    @Test
    fun stateOneRaisesErrorNoOperationsForAnEmptyList() =
        runTest {
            val f = Fake().json("""{"response":1,"list":[]}""")
            assertEquals("ERROR_NO_OPERATIONS", assertFailsWith<ApiException> { f.client.rent.stateOne(1) }.code)
        }

    @Test
    fun extend() =
        runTest {
            val f = Fake().json(withItem(0)).json(withItem(0))
            assertEquals(
                430,
                f.client.rent
                    .extend(430, days = 3)
                    .tzid(),
            )
            f.client.rent.extend(430)
            assertEquals("/api/rent/extendRentState.php", f.calls[0].uri().path)
            assertEquals(
                listOf(
                    mapOf("tzid" to "430", "days" to "3") + detail,
                    mapOf("tzid" to "430", "days" to "1") + detail,
                ),
                f.calls.map { Fake.queryOf(it) - "lang" },
            )
        }

    @Test
    fun tariffs() =
        runTest {
            val f = Fake().json(Fake.fixture("rent_tariffsRent"))
            val tariffs = f.client.rent.tariffs()
            assertEquals("/api/rent/tariffsRent.php", f.path())
            assertEquals(emptyMap(), f.query())
            assertEquals(9.11, tariffs.getValue("7").days()["7"])
        }

    @Test
    fun tariffsOneDefaultsTo7AndRaisesNoCountryForTheEmptyArray() =
        runTest {
            val ru =
                com.google.gson.JsonParser
                    .parseString(Fake.fixture("rent_tariffsRent"))
                    .asJsonObject["7"]
                    .toString()
            val f = Fake().json(ru).json("[]")
            assertEquals(
                7,
                f.client.rent
                    .tariffsOne()
                    .code(),
            )
            assertEquals("NO_COUNTRY", assertFailsWith<ApiException> { f.client.rent.tariffsOne(999) }.code)
            assertEquals(mapOf("country" to "7", "lang" to "en"), Fake.queryOf(f.calls[0]))
        }

    @Test
    fun close() =
        runTest {
            val f = Fake().json("""{"response":"1"}""")
            f.client.rent.close(430)
            assertEquals("/api/rent/closeRentNum.php", f.path())
            assertEquals(mapOf("tzid" to "430"), f.query())
        }
}
