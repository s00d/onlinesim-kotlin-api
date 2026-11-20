---
title: User
description: Balance, profile, payments and the webhook URL — client.user.
navigation:
  order: 3
---

## balance

Balance in USD.

```kotlin
suspend fun balance(): Balance
```

`Balance`: `balance()` includes the sum frozen by numbers and unconfirmed rents in progress (`zbalance()`, which may be negative): available = `balance() − max(zbalance(), 0)`. `income()` is the partner income (`null` when not sent). The server caches the answer for 60 s.

```kotlin
val b = client.user.balance()
val available = b.balance() - maxOf(b.zbalance(), 0.0)
```

Errors: `ApiException` `ERROR_WRONG_KEY`, `ACCOUNT_BLOCKED`, `API_ACCESS_DISABLED`, `API_ACCESS_IP`.

## profile

The account profile.

```kotlin
suspend fun profile(): Profile
```

`Profile`: `id()`, `name()`, `username()`, `email()`, `locale()`, `currency()`, `numberCountry()`, `numberReject()` (`List<String>`), `webhookUrl()`, `apiAccess()` (`true`, `false` when API access is disabled in the profile, or a map of allowed IPs, as a Gson `JsonElement`), `number()` (`Long`, `null` when no phone is linked), `country()` (`String` or `null`), ….

```kotlin
val p = client.user.profile()
println("${p.email()} ${p.webhookUrl()}")
```

## paymentHistory

Top-up orders (paginated, newest first) and the available payment methods.

```kotlin
suspend fun paymentHistory(page: Int = 1): PaymentHistory
```

`PaymentHistory`: `orders()` (`Paginator<PaymentOrder>`), `paylist()`, `domains()`, `forms()`, `currency()`. `PaymentOrder.sum()` stays a decimal string (`"89.00"`); `currency()` and `formText()` may be `null`. `paylist()` maps a group id to its payment methods; when the ids are 0, 1, 2… the server sends a JSON array, whose items are keyed `"0"`, `"1"`, ….
`Paginator<T>`: `data()`, `currentPage()`, `lastPage()`, `perPage()`, `total()`, `from()`, `to()`, `nextPageUrl()`, ….

```kotlin
val orders = client.user.paymentHistory().orders()
println("${orders.total()} orders, page ${orders.currentPage()}/${orders.lastPage()}")
```

## setWebhookUrl

Set the URL that receives SMS webhooks (see [Webhooks](/webhooks)).

```kotlin
suspend fun setWebhookUrl(url: String)
```

```kotlin
client.user.setWebhookUrl("https://example.com/onlinesim-hook")
```

Errors: `ApiException` `VALIDATION_ERROR` for an invalid URL; `message` holds the server's field messages.

## clearWebhookUrl

Remove the webhook URL, also from operations in progress.

```kotlin
suspend fun clearWebhookUrl()
```

```kotlin
client.user.clearWebhookUrl()
```

## webhookLogs

Webhook delivery log, newest first, 10 per page.

```kotlin
suspend fun webhookLogs(page: Int = 1): Paginator<WebhookLog>
```

`WebhookLog`: `id()`, `userId()`, `webhookUrl()`, `type()`, `params()`, `status()`, `error()`, `createdAt()`, `updatedAt()` — `type()` is `receiving_sms` or `rent_sms`, `status()` is `success`, `fail` or `pending`, `params()` the payload that was sent (`WebhookPayload`).

```kotlin
for (log in client.user.webhookLogs().data()) {
    println("${log.createdAt()} ${log.status()} ${log.error()}")
}
```
