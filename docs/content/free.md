---
title: Free numbers
description: Public numbers and their SMS — client.free. No API key needed.
navigation:
  order: 4
---

## list

Countries, the numbers of the selected country and the SMS of the selected number, in one call.

```kotlin
suspend fun list(country: Int? = null, number: String? = null, page: Int? = null, count: Int? = null): FreeList
```

| Param | Type | Default | Description |
|---|---|---|---|
| `country` | `Int?` | `44` (server) | Country code; an unknown one falls back to the first available. |
| `number` | `String?` | first number | Number **without** the country code, as the keys of `numbers()`. |
| `page` | `Int?` | `1` | Page of `messages()`. |
| `count` | `Int?` | server default | SMS per page, 10–500. |

`null` parameters are not sent. `FreeList`:

- `countries()`: `List<FreeList.Country>` — `country()`, `countryText()`, `countryOriginal()`
- `numbers()`: `Map<String, FreeList.Phone>` keyed by the short number — `country()`, `countryOriginal()`, `fullNumber()`, `dataHumans()`, `isArchive()`
- `messages()`: `FreeList.Messages`, a `Paginator<FreeList.Message>` plus `number()` and `country()`; a message has `id()`, `text()`, `inNumber()`, `myNumber()`, `code()` (`-` when no code was found), `createdAt()`, `dataHumans()`

```kotlin
val uk = client.free.list(country = 44)
val first = uk.numbers().keys.firstOrNull()
if (first != null) {
    val page = client.free.list(country = 44, number = first, count = 20)
    for (sms in page.messages().data()) println("${sms.inNumber()} ${sms.code()} ${sms.text()}")
}
```
