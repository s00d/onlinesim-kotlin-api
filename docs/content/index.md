---
title: onlinesim-kotlin-api
description: Kotlin coroutines client for the OnlineSim API — SMS numbers, rent, account and free numbers.
---

## Install

Kotlin/JVM on Java 11 or newer. `suspend` functions over the Java SDK [`ru.onlinesim:onlinesim-api`](https://s00d.github.io/onlinesim-java-api/) (pulled in automatically, with `kotlinx-coroutines-core`): the HTTP, parsing and error mapping are the Java SDK's, the API is Kotlin's.

::code-group
```kotlin [Gradle (Kotlin)]
implementation("ru.onlinesim:onlinesim-kotlin:1.0.0")
```

```groovy [Gradle]
implementation 'ru.onlinesim:onlinesim-kotlin:1.0.0'
```

```xml [Maven]
<dependency>
  <groupId>ru.onlinesim</groupId>
  <artifactId>onlinesim-kotlin</artifactId>
  <version>1.0.0</version>
</dependency>
```
::

## Quick start

Check the balance, order a number, wait for the SMS code.

```kotlin
import kotlinx.coroutines.runBlocking
import ru.onlinesim.kotlin.OnlineSim

fun main() = runBlocking {
    val client = OnlineSim(System.getenv("ONLINESIM_KEY"))

    val balance = client.user.balance()
    println("available: ${balance.balance() - maxOf(balance.zbalance(), 0.0)} USD")

    val order = client.numbers.getWithNumber("google", 44)
    println("enter ${order.number()} in the app")

    // Polls every 5 s for up to 10 min, then closes the operation.
    println("code: ${client.numbers.waitCode(order.tzid())}")
}
```

## Client options

```kotlin
import kotlin.time.Duration.Companion.seconds

val client = OnlineSim(apiKey = "your-api-key", lang = "en", timeout = 15.seconds)
```

| Option | Type | Default | Description |
|---|---|---|---|
| `apiKey` | `String?` | `null` | Sent as `Authorization: Bearer <apiKey>`, never in the URL. Not needed for tariffs and free numbers. |
| `baseUrl` | `String` | `https://onlinesim.host/api/` | API base; trailing slash optional. |
| `lang` | `String` | `"en"` | Language of server texts (`lang` query parameter). |
| `devId` | `String?` | `null` | Partner developer id (`dev_id`), sent only when set. |
| `timeout` | `kotlin.time.Duration` | 30 s | Per-request timeout. |
| `transport` | `Transport?` | `HttpTransport` | HTTP transport: your own `HttpClient` (proxy, TLS), or a fake in tests. |
| `dispatcher` | `CoroutineDispatcher` | `Dispatchers.IO` | Where the blocking HTTP calls run. Under `runTest` pass the test dispatcher. |

The client is immutable and thread-safe; share one instance. Every method is a `suspend fun`; calls are cancellable between requests (a request in flight finishes first).

Sections: [`client.numbers`](/numbers), [`client.rent`](/rent), [`client.user`](/user), [`client.free`](/free). Webhook bodies are parsed by the top-level [`parseWebhook`](/webhooks). `client.java` is the underlying Java client.

Durations (`timeout`, the `waitCode` interval) are `kotlin.time.Duration`. Response objects are the Java SDK's immutable classes, re-exported as type aliases in `ru.onlinesim.kotlin` (`Operation`, `RentItem`, `Balance`, …), with accessors named after the server's fields (`webhook_url` → `webhookUrl()`); nullable ones are platform types — treat `msg()`, `income()`, … as nullable. Money that the server sends as a decimal string (`balance`, rent `sum`) is a `Double`; maps that arrive as an empty PHP array `[]` are empty maps. Collections are never `null`.

A proxy or a custom TLS setup goes through your own `HttpClient`:

```kotlin
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.http.HttpClient

val http = HttpClient.newBuilder().proxy(ProxySelector.of(InetSocketAddress("proxy.local", 3128))).build()
val client = OnlineSim("your-api-key", transport = HttpTransport(http))
```

## Error handling

Every exception is unchecked and extends `OnlineSimException` (the Java SDK's classes, re-exported as type aliases).

| Class | When | Properties |
|---|---|---|
| `ApiException` | The API answered with an error code (also a wrong key, HTTP 456, and the rate limit, HTTP 429 `TRY_AGAIN_LATER`). | `code`, `message` |
| `NoNumberException` | `NO_NUMBER`: no number right now. Subclass of `ApiException`. | `code`, `message` |
| `TransportException` | Network error, request timeout, or a body that is not JSON. | `status` (`null` without a response), `body` (first 500 chars) |
| `WaitTimeoutException` | `waitCode` got no code within its `timeout`. | — |

```kotlin
try {
    client.numbers.get("google", 44)
} catch (e: NoNumberException) {
    // try another country or later
} catch (e: ApiException) {
    println("${e.code}: ${e.message}") // e.g. WARNING_LOW_BALANCE
} catch (e: TransportException) {
    println("network ${e.status} ${e.body}")
}
```

`message` is the server's text when it sends one, otherwise a short English description of the code (`ApiException.ERROR_MESSAGES`), otherwise the code itself. Profile validation failures become `ApiException` with code `VALIDATION_ERROR`.
