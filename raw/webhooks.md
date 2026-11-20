# Webhooks

> Receive SMS as HTTP callbacks and parse them with parseWebhook.

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

<table>
<thead>
  <tr>
    <th>
      Accessor
    </th>
    
    <th>
      Type
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
        webhookType()
      </code>
    </td>
    
    <td>
      <code>
        String?
      </code>
    </td>
    
    <td>
      <code>
        receiving_sms
      </code>
      
      , <code>
        rent_sms
      </code>
      
      , or <code>
        null
      </code>
      
       for anything else.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        operationId()
      </code>
    </td>
    
    <td>
      <code>
        Long?
      </code>
    </td>
    
    <td>
      <code>
        tzid
      </code>
      
       of the operation or rent.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        userId()
      </code>
    </td>
    
    <td>
      <code>
        Long?
      </code>
    </td>
    
    <td>
      Account id.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        countryCode()
      </code>
    </td>
    
    <td>
      <code>
        Int?
      </code>
    </td>
    
    <td>
      Country code of the number.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        number()
      </code>
    </td>
    
    <td>
      <code>
        String?
      </code>
    </td>
    
    <td>
      Numbers: <code>
        +
      </code>
      
       and the full number (<code>
        +79001234567
      </code>
      
      ), or <code>
        hidden
      </code>
      
      . Rent: the full number in digits without <code>
        +
      </code>
      
       (<code>
        79001234567
      </code>
      
      ).
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        sender()
      </code>
    </td>
    
    <td>
      <code>
        String?
      </code>
    </td>
    
    <td>
      Sender of the SMS, e.g. <code>
        VK
      </code>
      
      .
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        message()
      </code>
    </td>
    
    <td>
      <code>
        String?
      </code>
    </td>
    
    <td>
      Full SMS text.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        code()
      </code>
    </td>
    
    <td>
      <code>
        String?
      </code>
    </td>
    
    <td>
      Code extracted from the SMS; <code>
        ""
      </code>
      
       when none was found.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        timeStart()
      </code>
    </td>
    
    <td>
      <code>
        String?
      </code>
    </td>
    
    <td>
      Numbers: when the operation was ordered. Rent: when the SMS arrived. <code>
        YYYY-MM-DD HH:MM:SS
      </code>
      
      .
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        timeLeft()
      </code>
    </td>
    
    <td>
      <code>
        Long?
      </code>
    </td>
    
    <td>
      Time left: seconds for numbers, minutes for rent.
    </td>
  </tr>
</tbody>
</table>

Every accessor returns `null` when the field is missing or malformed.
