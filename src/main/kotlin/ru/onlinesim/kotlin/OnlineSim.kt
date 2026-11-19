package ru.onlinesim.kotlin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * OnlineSim API client: suspend functions over the Java SDK `ru.onlinesim:onlinesim-api`. Immutable and thread-safe.
 * Every call runs the (blocking) Java client on [dispatcher].
 *
 * ```kotlin
 * val client = OnlineSim(System.getenv("ONLINESIM_KEY"))
 * val balance = client.user.balance()
 * ```
 *
 * @param apiKey sent as `Authorization: Bearer <apiKey>`, never in the URL; `null` for the endpoints that need none
 * @param baseUrl full API base; trailing slash optional
 * @param lang language of server texts (`lang` query parameter)
 * @param devId partner developer id (`dev_id` query parameter), sent only when set
 * @param timeout per-request timeout, positive
 * @param transport HTTP transport; `null` for the default [HttpTransport]
 * @param dispatcher where the blocking HTTP calls run; pass the test dispatcher under `runTest`
 */
public class OnlineSim(
    apiKey: String? = null,
    baseUrl: String = DEFAULT_BASE_URL,
    lang: String = "en",
    devId: String? = null,
    timeout: Duration = DEFAULT_TIMEOUT,
    transport: Transport? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /** The underlying Java client, for APIs that take it. */
    public val java: ru.onlinesim.OnlineSim =
        ru.onlinesim.OnlineSim
            .builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .lang(lang)
            .devId(devId)
            .timeout(timeout.toJavaDuration())
            .transport(transport)
            .build()

    private val io = Io(dispatcher)

    /** Numbers for receiving SMS. */
    public val numbers: NumbersApi = NumbersApi(java.numbers(), io)

    /** Rented numbers. */
    public val rent: RentApi = RentApi(java.rent(), io)

    /** Account: balance, profile, payments, webhook URL. */
    public val user: UserApi = UserApi(java.user(), io)

    /** Public free numbers (no key needed). */
    public val free: FreeApi = FreeApi(java.free(), io)

    public companion object {
        /** Default API base. */
        public const val DEFAULT_BASE_URL: String = ru.onlinesim.OnlineSim.DEFAULT_BASE_URL

        /** Default per-request timeout. */
        public val DEFAULT_TIMEOUT: Duration = 30.seconds
    }
}

/** Runs a blocking Java SDK call on the client's dispatcher: the one way sections reach the network. */
internal class Io(
    private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun <T> invoke(call: () -> T): T = withContext(dispatcher) { call() }
}

/**
 * Parses an inbound webhook body (`receiving_sms` or `rent_sms`). Lenient: numeric fields may arrive as strings,
 * missing or malformed fields become `null`, unknown fields are dropped.
 *
 * @throws OnlineSimException only when [json] is not a JSON object
 */
public fun parseWebhook(json: String): WebhookPayload = ru.onlinesim.OnlineSim.parseWebhook(json)
