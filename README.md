# onlinesim-kotlin-api

[![Maven Central](https://img.shields.io/maven-central/v/ru.onlinesim/onlinesim-kotlin.svg)](https://central.sonatype.com/artifact/ru.onlinesim/onlinesim-kotlin)
[![CI](https://github.com/s00d/onlinesim-kotlin-api/actions/workflows/ci.yml/badge.svg)](https://github.com/s00d/onlinesim-kotlin-api/actions/workflows/ci.yml)

Kotlin coroutines client for the [OnlineSim](https://onlinesim.io) API: SMS numbers, rent, account and free numbers.
`suspend` functions over the Java SDK [`ru.onlinesim:onlinesim-api`](https://github.com/s00d/onlinesim-java-api); Java 11+.

```kotlin
implementation("ru.onlinesim:onlinesim-kotlin:1.0.0")
```

```kotlin
import kotlinx.coroutines.runBlocking
import ru.onlinesim.kotlin.OnlineSim

fun main() = runBlocking {
    val client = OnlineSim(System.getenv("ONLINESIM_KEY"))

    val b = client.user.balance()
    println("available: ${b.balance() - maxOf(b.zbalance(), 0.0)} USD")

    val order = client.numbers.getWithNumber("google", 44)
    println("enter ${order.number()} in the app")
    println("code: ${client.numbers.waitCode(order.tzid())}")
}
```

## Testing

`MockOnlineSim` is a built-in offline fake of the API: test your integration without network, key or money.
More in the [testing guide](https://s00d.github.io/onlinesim-kotlin-api/testing).

```kotlin
import kotlinx.coroutines.runBlocking
import ru.onlinesim.kotlin.mock.MockOnlineSim
import kotlin.time.Duration

fun main() = runBlocking {
    val mock = MockOnlineSim(balance = 10.0)
    val client = mock.client()

    val tzid = client.numbers.get("google", 44)
    mock.addSmsAfter(tzid, "Your code: 123-456", 2) // arrives on the third poll
    val code = client.numbers.waitCode(tzid, interval = Duration.ZERO)

    check(code == "123456" && mock.balance() == 9.8) { "$code ${mock.balance()}" }
}
```

Documentation: **https://s00d.github.io/onlinesim-kotlin-api/** · Changes: [CHANGELOG.md](CHANGELOG.md) · License: Apache-2.0
