package ru.onlinesim.kotlin

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FreeTest {
    @Test
    fun listHasNoPhpSuffixAndSendsOnlyTheSetParameters() =
        runTest {
            val f = Fake().json(Fake.fixture("getFreeList"))
            val list = f.client.free.list(country = 44, number = "7459608320", count = 20)
            assertEquals("/api/getFreeList", f.path())
            assertEquals(mapOf("country" to "44", "number" to "7459608320", "count" to "20"), f.query())
            assertEquals("447459608320", list.numbers().getValue("7459608320").fullNumber())
            assertEquals("195895", list.messages().data()[0].code())
        }

    @Test
    fun listWithoutOptionsSendsNoneAndPage() =
        runTest {
            val f = Fake().json(Fake.fixture("getFreeList")).json(Fake.fixture("getFreeList"))
            f.client.free.list()
            f.client.free.list(page = 3)
            assertEquals(listOf(emptyMap(), mapOf("page" to "3")), f.calls.map { Fake.queryOf(it) - "lang" })
        }
}
