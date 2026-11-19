package ru.onlinesim.kotlin

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import ru.onlinesim.StateOptions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Numbers for receiving SMS: `client.numbers`. */
public class NumbersApi internal constructor(
    private val java: ru.onlinesim.NumbersApi,
    private val io: Io,
) {
    /** Price of one number, USD; `null` when the country has no price for the service. */
    public suspend fun price(
        service: String,
        country: Int = 7,
    ): Double? = io { java.price(service, country) }

    /**
     * Orders a number and returns the operation id (`tzid`).
     *
     * @param reject number prefixes to skip, e.g. `listOf("4470")`; `null` or empty for the profile list
     * @throws NoNumberException when no number is available
     */
    public suspend fun get(
        service: String,
        country: Int = 7,
        reject: Collection<String>? = null,
    ): Long = io { java.get(service, country, reject) }

    /**
     * Orders a number and returns it right away.
     *
     * @param reject number prefixes to skip; `null` or empty for the profile list
     * @throws NoNumberException when no number is available
     */
    public suspend fun getWithNumber(
        service: String,
        country: Int = 7,
        reject: Collection<String>? = null,
    ): NumberOrder = io { java.getWithNumber(service, country, reject) }

    /**
     * All operations of the account; empty when there are none.
     *
     * @param messageToCode `true`: `msg()` is the code only; `false`: the full SMS text
     * @param msgList `true`: `msgList()` holds all SMS instead of `msg()`
     */
    public suspend fun state(
        messageToCode: Boolean = true,
        orderBy: OrderBy = OrderBy.ASC,
        msgList: Boolean = false,
    ): List<Operation> = io { java.state(options(messageToCode, msgList).orderBy(orderBy)) }

    /**
     * One operation.
     *
     * @throws ApiException `ERROR_NO_OPERATIONS` when it does not exist (any more)
     */
    public suspend fun stateOne(
        tzid: Long,
        messageToCode: Boolean = true,
        msgList: Boolean = false,
    ): Operation = io { java.stateOne(tzid, options(messageToCode, msgList)) }

    /** Switches the operation to its next SMS and keeps it open. */
    public suspend fun next(tzid: Long): Unit = io { java.next(tzid) }

    /**
     * Finishes the operation. Without SMS it can be closed only after its guard interval.
     *
     * @throws ApiException e.g. `NO_COMPLETE_TZID`, `INTERVAL_CONCURRENT_REQUESTS_ERROR`
     */
    public suspend fun close(tzid: Long): Unit = io { java.close(tzid) }

    /** Finishes the operation and reports the number as bad (only before any SMS arrived). */
    public suspend fun ban(tzid: Long): Unit = io { java.ban(tzid) }

    /** Services, prices and stock of every country, keyed by country code. No key needed. */
    public suspend fun tariffs(): Map<String, CountryStat> = io { java.tariffs() }

    /**
     * Services, prices and stock of one country. No key needed.
     *
     * @throws ApiException `NO_COUNTRY`
     */
    public suspend fun tariffsOne(country: Int = 7): CountryStat = io { java.tariffsOne(country) }

    /**
     * Polls [stateOne] every [interval] until a code different from [since] arrives, then closes the operation (or
     * calls [next] with [keepOpen]) and returns the code. Suspends with `delay`, so it is cancellable and runs on
     * virtual time under `runTest`. A transient [TransportException] is retried until the timeout; any
     * [ApiException] is thrown at once.
     *
     * @param interval poll interval; [Duration.ZERO] polls back to back (with the mock's `addSmsAfter`)
     * @param timeout give up after this long
     * @param keepOpen call [next] instead of [close] after the code, to wait for another SMS
     * @param fullMessage return the full SMS text instead of the code
     * @param since ignore this value (the previous code when waiting for the next one)
     * @return the code (or the full text with [fullMessage])
     * @throws WaitTimeoutException when no code arrived in time
     * @throws ApiException `TZ_OVER_OK` / `ERROR_NO_ITEMS` when the operation ended without a new code, or any other
     *   API error
     */
    public suspend fun waitCode(
        tzid: Long,
        interval: Duration = 5.seconds,
        timeout: Duration = 10.minutes,
        keepOpen: Boolean = false,
        fullMessage: Boolean = false,
        since: String? = null,
    ): String {
        require(!interval.isNegative()) { "interval must not be negative" }
        require(timeout.isPositive()) { "timeout must be positive" }

        // The new code, null while there is none; a finished operation throws.
        suspend fun poll(): String? {
            val op =
                try {
                    stateOne(tzid, messageToCode = !fullMessage)
                } catch (e: TransportException) {
                    return null // transient: retried until the timeout
                }
            val msg = op.msg()
            if (!msg.isNullOrEmpty() && msg != since) return msg
            if (op.response() == "TZ_OVER_OK" || op.response() == "ERROR_NO_ITEMS") throw ApiException(op.response())
            return null
        }
        val code =
            withTimeoutOrNull(timeout) {
                var found = poll()
                while (found == null) {
                    // yield keeps a zero-interval loop cancellable
                    if (interval.isPositive()) delay(interval) else yield()
                    found = poll()
                }
                found
            }
                // One last poll at the deadline, like the Java SDK's loop.
                ?: poll()
                ?: throw WaitTimeoutException("No code for operation $tzid within $timeout")
        // Outside the timeout: a code that arrived is never lost to it.
        if (keepOpen) next(tzid) else close(tzid)
        return code
    }

    private fun options(
        messageToCode: Boolean,
        msgList: Boolean,
    ): StateOptions = StateOptions.defaults().messageToCode(messageToCode).msgList(msgList)
}
