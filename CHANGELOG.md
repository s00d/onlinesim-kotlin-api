# Changelog

## 1.0.1

Aligned with the regenerated backend API spec. Still on `ru.onlinesim:onlinesim-api:2.1.0`; moving to 2.1.1 (model and mock fixes) follows its release.

### Fixed

- `numbers.waitCode` retries `ApiException` `TRY_AGAIN_LATER` (HTTP 429 rate limit), `ERROR_UNDEFINED` and `INTERVAL_CONCURRENT_REQUESTS_ERROR` until its timeout instead of throwing at once.
- KDoc and docs: rent reservation (`get` reserves, the first non-`notify` SMS charges), `sum` incl. extensions, `extend`/`close` rules and errors, `ban` answer `"0"`, available = balance − max(zbalance, 0), HTTP 429 as `ApiException`, `NoNumberException` only for `NO_NUMBER`, webhook `number`/`code`/`time_start`/`time_left`, the `waitCode` retry policy, a note on the mock's 2.1.0 behaviour.

## 1.0.0

First release.

### Added

- `ru.onlinesim:onlinesim-kotlin`, package `ru.onlinesim.kotlin`: an idiomatic Kotlin layer over the Java SDK `ru.onlinesim:onlinesim-api:2.1.0` (HTTP, parsing, error mapping and the mock are the Java SDK's). Kotlin/JVM, runs on Java 11+.
- `OnlineSim(apiKey, baseUrl, lang, devId, timeout, transport, dispatcher)` with default arguments (`kotlin.time.Duration` timeout, `Dispatchers.IO` by default) and the sections `numbers`, `rent`, `user`, `free` as `suspend` functions with default arguments (`country = 7`, `days = 1`, `extension = true`, `page = 1`, …).
- `numbers.waitCode(tzid, interval = 5.seconds, timeout = 10.minutes, keepOpen, fullMessage, since)`: a coroutine loop with `delay` (cancellable, virtual time under `runTest`), one last poll at the deadline, transport errors retried, `ApiException` thrown at once.
- Top-level `parseWebhook(json)`.
- The Java SDK's response types (`Operation`, `RentItem`, `Balance`, …), `Transport`/`TransportRequest`/`TransportResponse`/`HttpTransport`, `OrderBy` and exceptions (`ApiException`, `NoNumberException`, `TransportException`, `WaitTimeoutException`, `OnlineSimException`) re-exported as type aliases.
- `MockOnlineSim` (`ru.onlinesim.kotlin.mock`): the Java SDK's offline fake with named options (`apiKey`, `balance`, `prices`, `rentTariffs`, `guardInterval`), `mock.client(...)` returning the Kotlin client, `transport`, `addSms`, `addSmsAfter`, `failNext`, `failNextTransport`, `setBalance`, `balance`, `operations`, `rents`, `requests`, `reset`.
- Docs site (https://s00d.github.io/onlinesim-kotlin-api/), CI (JDK 17/21/25, ktlint, tests, docs examples compiled against the SDK, smoke test of the published artifact on Java 11), release workflow publishing to Maven Central on `vX.Y.Z` tags.
