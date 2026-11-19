---
title: Rent
description: Rent a number for days and receive every SMS sent to it — client.rent.
navigation:
  order: 2
---

A rent keeps a number for `days` days. Its SMS come in `RentItem.messages()`, newest first.

`RentItem`: `tzid()`, `status()`, `number()`, `country()`, `sum()`, `days()`, `hours()`, `time()`, `timeSeconds()`, `extension()`, `extend()`, `messages()`, `startTime()`, `stopTime()`, `dayExtend()`, … —
`sum()` is the price paid in USD (a `Double`; the server sends a decimal string), `time()` minutes left, `timeSeconds()` seconds left,
`extension()` `0` not extendable / `1` extendable / `2` auto-extension, `extend()` maps days to the extension price (empty when not extendable),
`messages()` is `List<RentSms>`: `id()`, `service()`, `text()`, `code()`, `createdAt()`.

## get

Rent a number and charge the balance.

```kotlin
suspend fun get(country: Int = 7, days: Int = 1, extension: Boolean = true): RentItem
```

| Param | Type | Default | Description |
|---|---|---|---|
| `country` | `Int` | `7` | Country code. |
| `days` | `Int` | `1` | One of the tariff's `days()` keys. |
| `extension` | `Boolean` | `true` | `false` makes the rent non-extendable for good. |

```kotlin
val rent = client.rent.get(7, days = 7)
println("${rent.tzid()} ${rent.number()} ${rent.sum()}")
```

Errors: `NoNumberException`; `ApiException` `UNDEFINED_DAYS`, `UNDEFINED_COUNTRY`, `WARNING_LOW_BALANCE`, `INTERVAL_CONCURRENT_REQUESTS_ERROR`, `TRY_AGAIN_LATER`.

## state

All rents of the account.

```kotlin
suspend fun state(): List<RentItem>
```

```kotlin
for (rent in client.rent.state()) {
    println("${rent.number()} ${rent.messages().firstOrNull()?.code() ?: "-"}")
}
```

## stateOne

One rent.

```kotlin
suspend fun stateOne(tzid: Long): RentItem
```

```kotlin
val messages = client.rent.stateOne(tzid).messages()
```

Errors: `ApiException` `ERROR_NO_OPERATIONS` for an unknown tzid.

## extend

Extend a rent by `days` days and charge the balance. Only rents created with `extension = true`.

```kotlin
suspend fun extend(tzid: Long, days: Int = 1): RentItem
```

```kotlin
val rent = client.rent.extend(tzid, days = 3)
println(rent.stopTime())
```

Errors: `ApiException` `TRY_AGAIN_LATER` (not extendable, unknown tzid or period, a repeat within 10 s), `WARNING_LOW_BALANCE`.

## close

Return the number before the period ends. A rent that is not confirmed yet and has no SMS can be closed only after 120 s; US numbers cannot be closed early.

```kotlin
suspend fun close(tzid: Long)
```

```kotlin
client.rent.close(tzid)
```

Errors: `ApiException` `ERROR_NO_OPERATIONS`, `NO_COMPLETE_TZID`, `NO_CLOSE_RENT_US`, `TRY_AGAIN_LATER`.

## tariffs

Rent prices of every country, keyed by country code. No API key needed.

```kotlin
suspend fun tariffs(): Map<String, RentTariff>
```

`RentTariff`: `code()`, `name()`, `enabled()`, `isNew()`, `position()`, `extend()`, `confirm()`, `days()`, `count()`, `extCounts()` — `days()` maps rent days to the price in USD, `count()` to the numbers available.

```kotlin
val tariffs = client.rent.tariffs()
```

## tariffsOne

Rent prices of one country.

```kotlin
suspend fun tariffsOne(country: Int = 7): RentTariff
```

```kotlin
val days = client.rent.tariffsOne(7).days()
println(days["7"]) // price of 7 days
```

Errors: `ApiException` `NO_COUNTRY` for a country without rent.
