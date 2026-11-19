package ru.onlinesim.kotlin

/** Account: `client.user`. */
public class UserApi internal constructor(
    private val java: ru.onlinesim.UserApi,
    private val io: Io,
) {
    /** Balance, USD; `balance()` includes the frozen `zbalance()`. */
    public suspend fun balance(): Balance = io { java.balance() }

    /** The account profile. */
    public suspend fun profile(): Profile = io { java.profile() }

    /** Top-up orders (paginated, newest first) and the available payment methods. */
    public suspend fun paymentHistory(page: Int = 1): PaymentHistory = io { java.paymentHistory(page) }

    /**
     * Sets the URL that receives SMS webhooks.
     *
     * @throws ApiException `VALIDATION_ERROR` for an invalid URL
     */
    public suspend fun setWebhookUrl(url: String): Unit = io { java.setWebhookUrl(url) }

    /** Removes the webhook URL (also from operations in progress). */
    public suspend fun clearWebhookUrl(): Unit = io { java.clearWebhookUrl() }

    /** Webhook delivery log, newest first, 10 per page. */
    public suspend fun webhookLogs(page: Int = 1): Paginator<WebhookLog> = io { java.webhookLogs(page) }
}
