---
title: Testing
description: Test your integration offline with MockOnlineSim — no network, key or money.
navigation:
  order: 6
---

`MockOnlineSim` (package `ru.onlinesim.kotlin.mock`) is an in-process fake of the OnlineSim API. It is the Java SDK's [`MockOnlineSim`](https://s00d.github.io/onlinesim-java-api/testing) with a Kotlin face: named options, `mock.client()` returning the Kotlin client. It is a `Transport` that answers like the real backend — same shapes, same error codes, HTTP 456 for a wrong key — and keeps a balance, operations and rents. Your code talks to a normal `OnlineSim` client, so nothing in it changes for tests. It is deterministic: no randomness, no clock, numbers and ids come from a counter. Thread-safe.

## Setup

```kotlin
import ru.onlinesim.kotlin.mock.MockOnlineSim

val mock = MockOnlineSim(balance = 10.0)
val client = mock.client() // an OnlineSim wired to the mock
```

```kotlin
MockOnlineSim(
    apiKey: String = "mock-key",
    balance: Double = 100.0,
    prices: Map<Int, Map<String, Double>> = /* see below */,
    rentTariffs: Map<Int, MockRentTariff> = /* see below */,
    guardInterval: Int = 0,
)
```

| Option | Default | Description |
|---|---|---|
| `apiKey` | `"mock-key"` | Key the mock accepts. `mock.client()` uses it; `mock.client(apiKey = "x")` tests a wrong key. |
| `balance` | `100.0` | Starting balance, USD. |
| `prices` | `{7: {vkcom: 0.5, telegram: 1.2, google: 0.3}, 44: {google: 0.2, vkcom: 0.36}}` | Number prices; replaces the defaults. Also feeds `numbers.tariffs()`. |
| `rentTariffs` | `{7: MockRentTariff({1: 1.0, 3: 2.5, 7: 5.0}, extend = true)}` | Rent prices; `extend = false` makes rents of that country non-extendable. |
| `guardInterval` | `0` | `> 0`: an operation without SMS cannot be closed (`NO_COMPLETE_TZID`). |

`mock.client(apiKey, lang, devId, timeout, dispatcher)` takes the usual [client options](/#client-options); the transport is always the mock's. To build the client yourself, pass `mock.transport`; any `baseUrl` works:

```kotlin
import ru.onlinesim.kotlin.OnlineSim
import ru.onlinesim.kotlin.mock.MockOnlineSim

val mock = MockOnlineSim()
val client = OnlineSim("mock-key", transport = mock.transport)
```

## A full example

Order a number, let an SMS arrive while `waitCode` polls, check the code and the money. Under `runTest`, give the client the test dispatcher: then the mock calls run on the test scheduler and `waitCode`'s `delay` uses virtual time — the 5 s interval passes instantly, and nothing races the timeout. `addSmsAfter` delivers the SMS after a number of polls, so the test needs no clock at all.

```kotlin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import ru.onlinesim.kotlin.mock.MockOnlineSim
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiveCodeTest {
    @Test
    fun receivesTheCode() = runTest {
        val mock = MockOnlineSim(balance = 10.0)
        val client = mock.client(dispatcher = StandardTestDispatcher(testScheduler))

        val order = client.numbers.getWithNumber("google", 44)
        assertEquals("+44900000001", order.number())

        mock.addSmsAfter(order.tzid(), "Your Google code: 123-456", 2) // the third poll sees it
        val code = client.numbers.waitCode(order.tzid()) // 10 s of virtual time

        assertEquals("123456", code)
        assertEquals("TZ_OVER_OK", mock.operations()[0].status()) // waitCode closed it
        assertEquals(9.8, mock.balance()) // the price is spent once an SMS arrived
    }
}
```

Outside `runTest` (a `main`, `runBlocking`), `waitCode(tzid, interval = Duration.ZERO)` polls back to back with no waiting.

The `msg` of an operation is the code by default: the first run of 4–8 digits, with spaces and dashes between digits removed (`123-456` → `123456`); a text without such a run comes back whole. `messageToCode = false` (or `fullMessage = true` in `waitCode`) gives the full text.

## Errors

`failNext(code)` makes the next request answer `{"response": code}` — the client turns it into `ApiException` (or `NoNumberException`) exactly as for the real server. Pass `endpoint` to fail only that endpoint. `failNextTransport()` makes the next request fail with a non-JSON HTTP 502 (`TransportException`).

```kotlin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import ru.onlinesim.kotlin.NoNumberException
import ru.onlinesim.kotlin.TransportException
import ru.onlinesim.kotlin.mock.MockOnlineSim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FailuresTest {
    @Test
    fun handlesAMissingNumberAndANetworkHiccup() = runTest {
        val mock = MockOnlineSim()
        val client = mock.client(dispatcher = StandardTestDispatcher(testScheduler))

        mock.failNext("NO_NUMBER", endpoint = "getNum.php")
        assertFailsWith<NoNumberException> { client.numbers.get("google") }

        mock.failNextTransport(status = 503)
        assertFailsWith<TransportException> { client.user.balance() }

        assertEquals(100.0, client.user.balance().balance()) // one-shot
    }
}
```

The state itself produces the real error codes too: `WARNING_LOW_BALANCE` when the balance is short, `NO_NUMBER` when `reject` matches the next number, `UNDEFINED_COUNTRY` / `ERROR_NO_SERVICE` for unknown prices, `ERROR_WRONG_TZID` for an unknown or closed operation, `TRY_AGAIN_LATER` when extending a non-extendable rent, `ERROR_WRONG_KEY` (HTTP 456) for a wrong key.

## Inspecting requests

`mock.requests()` lists every request received as `MockRequest`: `method()`, `path()` (relative to `/api/`), `query()` (decoded, in the order sent) and `body()` (the JSON of a POST as sent, `null` for a GET).

```kotlin
import kotlinx.coroutines.test.runTest
import ru.onlinesim.kotlin.mock.MockOnlineSim
import kotlin.test.Test
import kotlin.test.assertEquals

class RejectTest {
    @Test
    fun sendsRejectAsACommaList() = runTest {
        val mock = MockOnlineSim()
        mock.client().numbers.get("google", 44, reject = listOf("4470", "4471"))

        val req = mock.requests()[0]
        assertEquals("getNum.php", req.path())
        assertEquals(mapOf("service" to "google", "country" to "44", "reject" to "4470,4471", "lang" to "en"), req.query())
    }
}
```

## Control API

| Member | Description |
|---|---|
| `client(apiKey, lang, devId, timeout, dispatcher)` | A normal `OnlineSim` wired to the mock; every argument has a default. |
| `transport` | The fake `Transport`. |
| `addSms(tzid, text, service = null)` | An SMS arrives now at a number operation or a rent (detected by `tzid`). Throws `IllegalStateException` for an unknown or closed one. |
| `addSmsAfter(tzid, text, polls, service = null)` | The SMS arrives after `polls` state requests for `tzid` (`numbers.state`/`stateOne`, `rent.state`/`stateOne`). |
| `failNext(code, endpoint = null)` | The next request (to `endpoint` only, e.g. `"getNum.php"`) answers `{"response": code}`. |
| `failNextTransport(status = 502, endpoint = null)` | The next request fails with a non-JSON body and HTTP `status`. |
| `setBalance(usd)` / `balance()` | Set / read the total balance, USD (it includes the part frozen by open operations, as `user.balance()` reports it). |
| `operations()` / `rents()` | Snapshots of the current number operations (`MockOperation`) / open rents (`MockRent`), for assertions. |
| `requests()` | Log of the requests received (`MockRequest`). |
| `reset()` | Back to the constructor's state. |
| `java` | The underlying Java `ru.onlinesim.mock.MockOnlineSim`. |

## What the mock does

- **Numbers**: `get` freezes the price; `close` after an SMS spends it, `close` without one refunds it; `ban` after an SMS answers `"0"`, like the server. Numbers are `+<country>9000000NN`, ids count from 1. `next` switches to the following SMS. The operation's `time` stays at 1200 s — there is no clock.
- **Rent**: `get` charges at once and keeps the `extension` flag; `extend` charges again; `close` removes the rent. SMS appear in `messages()`, newest first.
- **User**: `balance` (frozen part included), a fixed profile, `setWebhookUrl` validates `http(s)` URLs (`VALIDATION_ERROR` otherwise), `webhookLogs` lists the deliveries the mock *recorded* when an SMS arrived while a webhook URL was set — it makes no HTTP call. `paymentHistory` is empty.
- **Free**: `free.list()` returns two fixed countries with one number and a few SMS each.
- Not imitated: the server's 5 s lock after `setOperationOk` (`INTERVAL_CONCURRENT_REQUESTS_ERROR`) and anything else that needs real time.
