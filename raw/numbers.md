# Numbers

> Order a number, read its SMS, close it — client.numbers.

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

<table>
<thead>
  <tr>
    <th>
      Param
    </th>
    
    <th>
      Type
    </th>
    
    <th>
      Default
    </th>
    
    <th>
      Description
    </th>
  </tr>
</thead>

<tbody>
  <tr>
    <td>
      <code>
        service
      </code>
    </td>
    
    <td>
      <code>
        String
      </code>
    </td>
    
    <td>
      —
    </td>
    
    <td>
      Service slug.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        country
      </code>
    </td>
    
    <td>
      <code>
        Int
      </code>
    </td>
    
    <td>
      <code>
        7
      </code>
    </td>
    
    <td>
      Country code.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        reject
      </code>
    </td>
    
    <td>
      <code>
        Collection<String>?
      </code>
    </td>
    
    <td>
      profile list
    </td>
    
    <td>
      Number prefixes to skip, e.g. <code>
        listOf("4470")
      </code>
      
      .
    </td>
  </tr>
</tbody>
</table>

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

<table>
<thead>
  <tr>
    <th>
      Param
    </th>
    
    <th>
      Type
    </th>
    
    <th>
      Default
    </th>
    
    <th>
      Description
    </th>
  </tr>
</thead>

<tbody>
  <tr>
    <td>
      <code>
        messageToCode
      </code>
    </td>
    
    <td>
      <code>
        Boolean
      </code>
    </td>
    
    <td>
      <code>
        true
      </code>
    </td>
    
    <td>
      <code>
        msg()
      </code>
      
       is the code only; <code>
        false
      </code>
      
       — the full SMS text.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        orderBy
      </code>
    </td>
    
    <td>
      <code>
        OrderBy
      </code>
    </td>
    
    <td>
      <code>
        ASC
      </code>
    </td>
    
    <td>
      Order of operations (<code>
        ASC
      </code>
      
      , <code>
        DESC
      </code>
      
      ).
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        msgList
      </code>
    </td>
    
    <td>
      <code>
        Boolean
      </code>
    </td>
    
    <td>
      <code>
        false
      </code>
    </td>
    
    <td>
      All SMS in <code>
        msgList()
      </code>
      
       (<code>
        List<OperationSms>
      </code>
      
      ) instead of <code>
        msg()
      </code>
      
      .
    </td>
  </tr>
</tbody>
</table>

`Operation`: `tzid()`, `response()`, `number()`, `service()`, `country()`, `sum()`, `time()`, `msg()`, `msgList()`, `guardIntervalRemainingSeconds()`, ….
`response()` is the state: `TZ_INPOOL` (number being assigned), `TZ_NUM_WAIT` (waiting for SMS), `TZ_NUM_ANSWER` (SMS arrived), `TZ_OVER_OK` (finished), `ERROR_NO_ITEMS` (failed), or `null` when the server sent no state (no number could be issued). `msg()` is `null` until the first SMS. The server caches answers for a few seconds.

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

<table>
<thead>
  <tr>
    <th>
      Param
    </th>
    
    <th>
      Type
    </th>
    
    <th>
      Default
    </th>
    
    <th>
      Description
    </th>
  </tr>
</thead>

<tbody>
  <tr>
    <td>
      <code>
        interval
      </code>
    </td>
    
    <td>
      <code>
        Duration
      </code>
    </td>
    
    <td>
      5 s
    </td>
    
    <td>
      Poll interval; <code>
        Duration.ZERO
      </code>
      
       polls back to back (with the mock).
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        timeout
      </code>
    </td>
    
    <td>
      <code>
        Duration
      </code>
    </td>
    
    <td>
      10 min
    </td>
    
    <td>
      Give up after; one last poll happens at the deadline.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        keepOpen
      </code>
    </td>
    
    <td>
      <code>
        Boolean
      </code>
    </td>
    
    <td>
      <code>
        false
      </code>
    </td>
    
    <td>
      Call <code>
        next(tzid)
      </code>
      
       instead of <code>
        close(tzid)
      </code>
      
      , to wait for another SMS.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        fullMessage
      </code>
    </td>
    
    <td>
      <code>
        Boolean
      </code>
    </td>
    
    <td>
      <code>
        false
      </code>
    </td>
    
    <td>
      Return the full SMS text instead of the code.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        since
      </code>
    </td>
    
    <td>
      <code>
        String?
      </code>
    </td>
    
    <td>
      <code>
        null
      </code>
    </td>
    
    <td>
      Ignore this value (the previous code).
    </td>
  </tr>
</tbody>
</table>

Transient errors are retried until the timeout: `TransportException`, and `ApiException` `TRY_AGAIN_LATER` (the HTTP 429 rate limit), `ERROR_UNDEFINED` and `INTERVAL_CONCURRENT_REQUESTS_ERROR`. Any other `ApiException` is thrown at once.

```kotlin
import kotlin.time.Duration.Companion.minutes

val first = client.numbers.waitCode(tzid, keepOpen = true)
val second = client.numbers.waitCode(tzid, since = first, timeout = 5.minutes)
```

Errors: `WaitTimeoutException`; `ApiException` with code `TZ_OVER_OK` or `ERROR_NO_ITEMS` when the operation ended without a new code; any other non-transient `ApiException` from `stateOne`, and any `ApiException` from `close`/`next`.

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

Errors: as `close`; `ApiException` with code `"0"` when the operation already has an SMS — nothing is changed then.

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
