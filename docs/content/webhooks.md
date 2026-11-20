---
title: Webhooks
description: Receive SMS as HTTP callbacks and parse them with parseWebhook.
navigation:
  order: 5
---

Set a URL with [`client.user.setWebhookUrl`](/user#setwebhookurl). For each SMS the server POSTs a JSON body to it: `webhook_type` `receiving_sms` for numbers, `rent_sms` for rents. Delivery attempts are listed by [`client.user.webhookLogs`](/user#webhooklogs).

## parseWebhook

Parse an inbound webhook body. A top-level function (not `suspend`): no client needed.

```kotlin
fun parseWebhook(json: String): WebhookPayload
```

Lenient: numeric fields may arrive as strings, missing or malformed fields become `null`, unknown fields are dropped. It throws `OnlineSimException` only when the input is not a JSON object.

```kotlin
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import ru.onlinesim.kotlin.parseWebhook

fun main() {
    val server = HttpServer.create(InetSocketAddress(8080), 0)
    server.createContext("/onlinesim-hook") { exchange ->
        val hook = parseWebhook(exchange.requestBody.readBytes().decodeToString())
        if (hook.webhookType() == "receiving_sms") println("${hook.operationId()} ${hook.code()}")
        exchange.sendResponseHeaders(200, -1)
        exchange.close()
    }
    server.start()
}
```

## Payload fields

| Accessor | Type | Description |
|---|---|---|
| `webhookType()` | `String?` | `receiving_sms`, `rent_sms`, or `null` for anything else. |
| `operationId()` | `Long?` | `tzid` of the operation or rent. |
| `userId()` | `Long?` | Account id. |
| `countryCode()` | `Int?` | Country code of the number. |
| `number()` | `String?` | Numbers: `+` and the full number (`+79001234567`), or `hidden`. Rent: the full number in digits without `+` (`79001234567`). |
| `sender()` | `String?` | Sender of the SMS, e.g. `VK`. |
| `message()` | `String?` | Full SMS text. |
| `code()` | `String?` | Code extracted from the SMS; `""` when none was found. |
| `timeStart()` | `String?` | Numbers: when the operation was ordered. Rent: when the SMS arrived. `YYYY-MM-DD HH:MM:SS`. |
| `timeLeft()` | `Long?` | Time left: seconds for numbers, minutes for rent. |

Every accessor returns `null` when the field is missing or malformed.
