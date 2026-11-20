# Free numbers

> Public numbers and their SMS — client.free. No API key needed.

## list

Countries, the numbers of the selected country and the SMS of the selected number, in one call.

```kotlin
suspend fun list(country: Int? = null, number: String? = null, page: Int? = null, count: Int? = null): FreeList
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
        country
      </code>
    </td>
    
    <td>
      <code>
        Int?
      </code>
    </td>
    
    <td>
      <code>
        44
      </code>
      
       (server)
    </td>
    
    <td>
      Country code; an unknown one falls back to the first available.
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        number
      </code>
    </td>
    
    <td>
      <code>
        String?
      </code>
    </td>
    
    <td>
      first number
    </td>
    
    <td>
      Number <strong>
        without
      </strong>
      
       the country code, as the keys of <code>
        numbers()
      </code>
      
      .
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        page
      </code>
    </td>
    
    <td>
      <code>
        Int?
      </code>
    </td>
    
    <td>
      <code>
        1
      </code>
    </td>
    
    <td>
      Page of <code>
        messages()
      </code>
      
      .
    </td>
  </tr>
  
  <tr>
    <td>
      <code>
        count
      </code>
    </td>
    
    <td>
      <code>
        Int?
      </code>
    </td>
    
    <td>
      server default
    </td>
    
    <td>
      SMS per page, 10–500.
    </td>
  </tr>
</tbody>
</table>

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
