---
title: Numbers
description: Order a number, read its SMS, close it — client.numbers.
navigation:
  order: 1
---

An operation (`tzid`) is one ordered number. Order it with `get`/`getWithNumber`, read the SMS with `stateOne` or `waitCode`, finish it with `close` (or `ban`). Countries are phone codes (`7`, `44`, …), services are slugs (`google`, `vkcom`, …) as returned by `tariffs`. Every `country` parameter defaults to `7`.

## price

Price of one number, USD; `null` when the country has no price for the service.

```kotlin
suspend fun price(service: String, country: Int = 7): Double?
```

```kotlin
val usd = client.numbers.price("google", 44)
```

Errors: `ApiException` `ERROR_NO_SERVICE`, `UNDEFINED_COUNTRY`.

## get

Order a number; returns the operation id. Freezes the price on the balance.

```kotlin
suspend fun get(service: String, country: Int = 7, reject: Collection<String>? = null): Long
```

| Param | Type | Default | Description |
|---|---|---|---|
| `service` | `String` | — | Service slug. |
| `country` | `Int` | `7` | Country code. |
| `reject` | `Collection<String>?` | profile list | Number prefixes to skip, e.g. `listOf("4470")`. |

```kotlin
val tzid = client.numbers.get("google", 44, reject = listOf("4470"))
```

Errors: `NoNumberException`; `ApiException` `WARNING_LOW_BALANCE`, `ERROR_NO_SERVICE`, `UNDEFINED_COUNTRY`, `EXCEEDED_CONCURRENT_OPERATIONS`, `INTERVAL_CONCURRENT_REQUESTS_ERROR`, `TRY_AGAIN_LATER`.

## getWithNumber

Same as `get`, but returns the number right away.

```kotlin
suspend fun getWithNumber(service: String, country: Int = 7, reject: Collection<String>? = null): NumberOrder
```

`NumberOrder`: `tzid()`, `number()` (with `+`), `country()`, `time()` (seconds left), `service()`, `title()`, `guardIntervalRemainingSeconds()`.

```kotlin
val order = client.numbers.getWithNumber("google", 44)
println("${order.tzid()} ${order.number()}")
```

Errors: as `get`.

## state

All operations of the account; an empty list when there are none.

```kotlin
suspend fun state(messageToCode: Boolean = true, orderBy: OrderBy = OrderBy.ASC, msgList: Boolean = false): List<Operation>
```

| Param | Type | Default | Description |
|---|---|---|---|
| `messageToCode` | `Boolean` | `true` | `msg()` is the code only; `false` — the full SMS text. |
| `orderBy` | `OrderBy` | `ASC` | Order of operations (`ASC`, `DESC`). |
| `msgList` | `Boolean` | `false` | All SMS in `msgList()` (`List<OperationSms>`) instead of `msg()`. |

`Operation`: `tzid()`, `response()`, `number()`, `service()`, `country()`, `sum()`, `time()`, `msg()`, `msgList()`, `guardIntervalRemainingSeconds()`, ….
`response()` is the state: `TZ_INPOOL` (number being assigned), `TZ_NUM_WAIT` (waiting for SMS), `TZ_NUM_ANSWER` (SMS arrived), `TZ_OVER_OK` (finished), `ERROR_NO_ITEMS` (failed). `msg()` is `null` until the first SMS. The server caches answers for a few seconds.

```kotlin
for (op in client.numbers.state(orderBy = OrderBy.DESC)) {
    println("${op.tzid()} ${op.response()} ${op.msg()}")
}
```

## stateOne

One operation.

```kotlin
suspend fun stateOne(tzid: Long, messageToCode: Boolean = true, msgList: Boolean = false): Operation
```

```kotlin
val op = client.numbers.stateOne(tzid)
if (op.response() == "TZ_NUM_ANSWER") println(op.msg())
```

Errors: `ApiException` `ERROR_NO_OPERATIONS` when the operation does not exist (any more).

## waitCode

Poll the operation until a code arrives, then close it and return the code. Suspends with `delay` between polls (no thread is blocked while waiting), so it is cancellable and runs on virtual time under `runTest`.

```kotlin
suspend fun waitCode(
    tzid: Long,
    interval: Duration = 5.seconds,
    timeout: Duration = 10.minutes,
    keepOpen: Boolean = false,
    fullMessage: Boolean = false,
    since: String? = null,
): String
```

| Param | Type | Default | Description |
|---|---|---|---|
| `interval` | `Duration` | 5 s | Poll interval; `Duration.ZERO` polls back to back (with the mock). |
| `timeout` | `Duration` | 10 min | Give up after; one last poll happens at the deadline. |
| `keepOpen` | `Boolean` | `false` | Call `next(tzid)` instead of `close(tzid)`, to wait for another SMS. |
| `fullMessage` | `Boolean` | `false` | Return the full SMS text instead of the code. |
| `since` | `String?` | `null` | Ignore this value (the previous code). |

A transient `TransportException` is retried until the timeout; any `ApiException` is thrown at once.

```kotlin
import kotlin.time.Duration.Companion.minutes

val first = client.numbers.waitCode(tzid, keepOpen = true)
val second = client.numbers.waitCode(tzid, since = first, timeout = 5.minutes)
```

Errors: `WaitTimeoutException`; `ApiException` with code `TZ_OVER_OK` or `ERROR_NO_ITEMS` when the operation ended without a new code; any other `ApiException` from `stateOne`/`close`/`next`.

## next

Switch the operation to its next SMS and keep it open (for services that send several SMS).

```kotlin
suspend fun next(tzid: Long)
```

```kotlin
client.numbers.next(tzid)
```

## close

Finish the operation. An operation without SMS can be closed only after its guard interval (`guardIntervalRemainingSeconds()`); every attempt locks the tzid for 5 s.

```kotlin
suspend fun close(tzid: Long)
```

```kotlin
client.numbers.close(tzid)
```

Errors: `ApiException` `NO_COMPLETE_TZID`, `INTERVAL_CONCURRENT_REQUESTS_ERROR`, `ERROR_WRONG_TZID`.

## ban

Finish the operation and report the number as bad. Only before any SMS arrived.

```kotlin
suspend fun ban(tzid: Long)
```

```kotlin
client.numbers.ban(tzid)
```

Errors: as `close`.

## tariffs

Services, prices and stock of every country, keyed by country code. No API key needed. The answer is large (a few MB); prefer `tariffsOne`.

```kotlin
suspend fun tariffs(): Map<String, CountryStat>
```

`CountryStat`: `name()`, `localeName()`, `position()`, `code()`, `other()`, `isNew()`, `enabled()`, `services()`; `services()` maps `service_<slug>` to `ServiceStat`: `count()` (`null` when unknown), `popular()`, `code()`, `price()`, `id()`, `service()`, `slug()`.

```kotlin
val all = client.numbers.tariffs()
println("${all.size} countries")
```

## tariffsOne

Services, prices and stock of one country.

```kotlin
suspend fun tariffsOne(country: Int = 7): CountryStat
```

```kotlin
val uk = client.numbers.tariffsOne(44)
for (s in uk.services().values) println("${s.slug()} ${s.price()} ${s.count()}")
```

Errors: `ApiException` `NO_COUNTRY`.
