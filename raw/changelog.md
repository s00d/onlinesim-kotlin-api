# Changelog

> Release notes of onlinesim-kotlin-api.

The full history is in [CHANGELOG.md](https://github.com/s00d/onlinesim-kotlin-api/blob/main/CHANGELOG.md).

## 1.0.1

- On `onlinesim-api` 2.1.1: fixed `Profile.number`/`country`, `paylist` kept when it arrives as a list, `RentSms.code` `"0"` for `notify` SMS, the current error messages.
- `waitCode` retries `TRY_AGAIN_LATER` (rate limit), `ERROR_UNDEFINED` and `INTERVAL_CONCURRENT_REQUESTS_ERROR`.
- Docs follow the current API: rent reservation and confirmation, `extend`/`close` rules, `ban` answer `"0"`, the available-balance formula, webhook fields.
- `MockOnlineSim` follows the backend more closely: rent reservation and confirmation, `rent.close`/`rent.extend` answers, missing vs wrong key — see [Testing](/testing).

## 1.0.0

First release.

- `ru.onlinesim:onlinesim-kotlin:1.0.0`, package `ru.onlinesim.kotlin`. Kotlin/JVM, Java 11+.
- Suspend functions over the Java SDK `ru.onlinesim:onlinesim-api:2.1.0`: `client.numbers`, `client.rent`, `client.user`, `client.free`, with default arguments; blocking calls run on an injectable dispatcher (`Dispatchers.IO` by default).
- `numbers.waitCode` as a coroutine: `delay` between polls, `kotlin.time.Duration` interval and timeout, `keepOpen`, `since`, `fullMessage`; cancellable, virtual time under `runTest`.
- `parseWebhook`; the Java SDK's response types and exceptions re-exported as type aliases.
- `MockOnlineSim` (`ru.onlinesim.kotlin.mock`): the Java SDK's offline fake with named options and `mock.client()` returning the Kotlin client — see [Testing](/testing).
