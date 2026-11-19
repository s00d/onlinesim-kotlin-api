package ru.onlinesim.kotlin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.net.URLDecoder
import kotlin.test.assertEquals

/** Fake transport: answers canned replies in order and records every request (and the thread it ran on). */
internal class Fake(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    private val clock: () -> Long = { 0 },
    options: (
        Transport,
    ) -> OnlineSim = { OnlineSim("test-key", "https://api.test/api/", transport = it, dispatcher = dispatcher) },
) : Transport {
    val calls = mutableListOf<TransportRequest>()
    val threads = mutableListOf<String>()

    /** [clock] at each request (runTest's virtual time in the waitCode tests). */
    val times = mutableListOf<Long>()
    private val replies = ArrayDeque<Any>()
    val client: OnlineSim = options(this)

    fun json(json: String) = reply(200, json)

    fun reply(
        status: Int,
        body: String,
    ) = apply { replies.add(TransportResponse(status, body)) }

    fun fail(error: IOException) = apply { replies.add(error) }

    override fun send(request: TransportRequest): TransportResponse {
        calls.add(request)
        threads.add(Thread.currentThread().name)
        times.add(clock())
        return when (
            val reply =
                replies.removeFirstOrNull() ?: throw AssertionError("unexpected request ${request.uri()}")
        ) {
            is IOException -> throw reply
            else -> reply as TransportResponse
        }
    }

    /** The only request made. */
    fun last(): TransportRequest {
        assertEquals(1, calls.size, "number of requests")
        return calls[0]
    }

    /** Query of the only request, without the `lang` every request carries. */
    fun query(): Map<String, String> = queryOf(last()) - "lang"

    fun path(): String = last().uri().path

    companion object {
        fun queryOf(request: TransportRequest): Map<String, String> =
            request.uri().rawQuery?.split("&")?.associate {
                val (k, v) = it.split("=", limit = 2)
                URLDecoder.decode(k, Charsets.UTF_8) to URLDecoder.decode(v, Charsets.UTF_8)
            } ?: emptyMap()

        fun fixture(name: String): String = Fake::class.java.getResource("/fixtures/$name.json")!!.readText()
    }
}
