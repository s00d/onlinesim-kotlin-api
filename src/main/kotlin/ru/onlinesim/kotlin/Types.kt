// The response types, the transport and the exceptions are the Java SDK's (immutable, null-safe accessors such as
// `order.tzid()`), re-exported here so `import ru.onlinesim.kotlin.*` is all a Kotlin user needs.
package ru.onlinesim.kotlin

/** Balance, USD: `balance()` includes the frozen `zbalance()`; available = `balance() - max(zbalance(), 0)`. */
public typealias Balance = ru.onlinesim.Balance

/** A number ordered with [NumbersApi.getWithNumber]. */
public typealias NumberOrder = ru.onlinesim.NumberOrder

/** A number operation, from [NumbersApi.state] / [NumbersApi.stateOne]. */
public typealias Operation = ru.onlinesim.Operation

/** One SMS of an operation when `msgList = true`. */
public typealias OperationSms = ru.onlinesim.OperationSms

/** Order of [NumbersApi.state]: `ASC` (oldest first, default) or `DESC`. */
public typealias OrderBy = ru.onlinesim.StateOptions.Order

/** A rented number; `sum()` is the cost in USD incl. extensions. */
public typealias RentItem = ru.onlinesim.RentItem

/** One SMS of a rent. */
public typealias RentSms = ru.onlinesim.RentSms

/** Rent prices of one country. */
public typealias RentTariff = ru.onlinesim.RentTariff

/** Services, prices and stock of one country. */
public typealias CountryStat = ru.onlinesim.CountryStat

/** Price and stock of one service in a country. */
public typealias ServiceStat = ru.onlinesim.ServiceStat

/** The account profile. */
public typealias Profile = ru.onlinesim.Profile

/** Top-up orders and payment methods. */
public typealias PaymentHistory = ru.onlinesim.PaymentHistory

/** One top-up order. */
public typealias PaymentOrder = ru.onlinesim.PaymentOrder

/** A Laravel-style page: `data()`, `currentPage()`, `lastPage()`, `total()`… */
public typealias Paginator<T> = ru.onlinesim.Paginator<T>

/** One webhook delivery. */
public typealias WebhookLog = ru.onlinesim.WebhookLog

/** An inbound webhook body, from [parseWebhook]. */
public typealias WebhookPayload = ru.onlinesim.WebhookPayload

/** Countries, numbers and SMS of the free numbers. */
public typealias FreeList = ru.onlinesim.FreeList

/** Sends one HTTP request; inject your own (proxy, TLS, fake in tests). Blocking: runs on the client's dispatcher. */
public typealias Transport = ru.onlinesim.Transport

/** A request built by the client: `method()`, `uri()`, `headers()`, `body()`, `timeout()`. */
public typealias TransportRequest = ru.onlinesim.Transport.Request

/** A response for the client: `TransportResponse(status, body)`. */
public typealias TransportResponse = ru.onlinesim.Transport.Response

/** The default [Transport] (`java.net.http`). */
public typealias HttpTransport = ru.onlinesim.HttpTransport

/** Base of every exception thrown by the SDK. */
public typealias OnlineSimException = ru.onlinesim.OnlineSimException

/**
 * The API answered an error code: `code` (raw string) and `message`. A wrong key (HTTP 456) and the rate limit
 * (HTTP 429 `TRY_AGAIN_LATER`) are ones too.
 */
public typealias ApiException = ru.onlinesim.ApiException

/** No number available right now (`NO_NUMBER`). A subclass of [ApiException]. */
public typealias NoNumberException = ru.onlinesim.NoNumberException

/** Network failure, request timeout or a non-JSON answer: `status` (null without a response) and `body` excerpt. */
public typealias TransportException = ru.onlinesim.TransportException

/** [NumbersApi.waitCode] got no code within its timeout. */
public typealias WaitTimeoutException = ru.onlinesim.WaitTimeoutException
