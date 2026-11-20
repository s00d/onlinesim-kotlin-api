package ru.onlinesim.kotlin.mock

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import ru.onlinesim.kotlin.OnlineSim
import ru.onlinesim.kotlin.Transport
import ru.onlinesim.mock.MockOptions
import kotlin.time.Duration

/** A number operation as the mock holds it (`tzid()`, `status()`, `messages()`…). */
public typealias MockOperation = ru.onlinesim.mock.MockOperation

/** A rent as the mock holds it. */
public typealias MockRent = ru.onlinesim.mock.MockRent

/** One SMS the mock holds. */
public typealias MockSms = ru.onlinesim.mock.MockSms

/** One request the mock received: `method()`, `path()` (relative to `/api/`), `query()`, `body()`. */
public typealias MockRequest = ru.onlinesim.mock.MockRequest

/** Rent tariff of one country: `MockRentTariff(mapOf(1 to 1.0, 3 to 2.5), extend = true)`. */
public typealias MockRentTariff = ru.onlinesim.mock.MockRentTariff

/**
 * In-process fake OnlineSim API for tests: no network, no key, no money, no clock. A Kotlin face of the Java SDK's
 * `ru.onlinesim.mock.MockOnlineSim` (the same behaviour, the same state): [client] returns a normal [OnlineSim]
 * wired to it, the helpers add SMS, queue failures and expose the state. Thread-safe.
 *
 * ```kotlin
 * val mock = MockOnlineSim(balance = 10.0)
 * val client = mock.client()
 * val tzid = client.numbers.get("google", 44)
 * mock.addSmsAfter(tzid, "Your code: 123-456", 2)
 * client.numbers.waitCode(tzid, interval = Duration.ZERO) // "123456"
 * ```
 *
 * @param apiKey key the mock accepts as `Authorization: Bearer <apiKey>`
 * @param balance starting balance, USD
 * @param prices number prices: country → service slug → USD
 * @param rentTariffs rent tariffs: country → tariff
 * @param guardInterval seconds an operation without SMS, or an unconfirmed rent without any SMS, cannot be closed
 *   (`NO_COMPLETE_TZID`); 0 = never
 */
public class MockOnlineSim(
    public val apiKey: String = DEFAULT_API_KEY,
    balance: Double = 100.0,
    prices: Map<Int, Map<String, Double>> = MockOptions.defaults().prices(),
    rentTariffs: Map<Int, MockRentTariff> = MockOptions.defaults().rentTariffs(),
    guardInterval: Int = 0,
) {
    /** The underlying Java mock. */
    public val java: ru.onlinesim.mock.MockOnlineSim =
        ru.onlinesim.mock.MockOnlineSim(
            MockOptions
                .defaults()
                .apiKey(apiKey)
                .balance(balance)
                .prices(prices)
                .rentTariffs(rentTariffs)
                .guardInterval(guardInterval),
        )

    /** The fake transport, for a client built by hand (`OnlineSim(transport = mock.transport)`); any base URL works. */
    public val transport: Transport get() = java.transport()

    /**
     * A normal client wired to the mock, at [MOCK_BASE_URL] with the mock's transport.
     *
     * @param apiKey the mock's key by default; another one (or `null`) to test a wrong key
     * @param dispatcher where the calls run; pass the test dispatcher under `runTest`
     */
    public fun client(
        apiKey: String? = this.apiKey,
        lang: String = "en",
        devId: String? = null,
        timeout: Duration = OnlineSim.DEFAULT_TIMEOUT,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): OnlineSim = OnlineSim(apiKey, MOCK_BASE_URL, lang, devId, timeout, transport, dispatcher)

    /**
     * An SMS arrives now at number operation or rent [tzid].
     *
     * @param service sender service; `null` for the operation's (`other` for a rent)
     * @throws IllegalStateException when there is no open operation or rent with that id
     */
    public fun addSms(
        tzid: Long,
        text: String,
        service: String? = null,
    ): Unit = java.addSms(tzid, text, service)

    /**
     * An SMS appears after [polls] state requests for [tzid] (with 2 the third poll sees it): `waitCode` with
     * `interval = Duration.ZERO` gets it without any clock.
     */
    public fun addSmsAfter(
        tzid: Long,
        text: String,
        polls: Int,
        service: String? = null,
    ): Unit = java.addSmsAfter(tzid, text, polls, service)

    /**
     * The next request (only to [endpoint], e.g. `getNum.php`, when set) answers `{"response": code}`.
     */
    public fun failNext(
        code: String,
        endpoint: String? = null,
    ): Unit = java.failNext(code, endpoint)

    /** The next request (only to [endpoint] when set) fails at transport level: a non-JSON answer with [status]. */
    public fun failNextTransport(
        status: Int = 502,
        endpoint: String? = null,
    ): Unit = java.failNextTransport(status, endpoint)

    /** Sets the total balance, USD (what `getBalance` reports, the sums frozen by open operations and unconfirmed rents included). */
    public fun setBalance(usd: Double): Unit = java.setBalance(usd)

    /** The balance, USD, frozen sums included (like `getBalance`). */
    public fun balance(): Double = java.balance()

    /** Snapshot of the number operations, oldest first. */
    public fun operations(): List<MockOperation> = java.operations()

    /** Snapshot of the open rents, oldest first. */
    public fun rents(): List<MockRent> = java.rents()

    /** Snapshot of the requests received, oldest first. */
    public fun requests(): List<MockRequest> = java.requests()

    /** Back to the initial options: balance, no operations, rents, webhook, failures or requests. */
    public fun reset(): Unit = java.reset()

    public companion object {
        /** Base URL of clients made by [client]. Never contacted: the mock answers in-process. */
        public const val MOCK_BASE_URL: String = ru.onlinesim.mock.MockOnlineSim.MOCK_BASE_URL

        /** The default key. */
        public const val DEFAULT_API_KEY: String = "mock-key"
    }
}
