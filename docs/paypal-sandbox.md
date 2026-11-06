# PayPal Sandbox walkthrough

## Configure credentials

Create a Sandbox REST application in the PayPal Developer Dashboard. Put the Sandbox client ID and secret into the ignored root `.env`, never a `VITE_*` variable or Git. Use distinct Sandbox business (merchant) and personal (buyer) accounts. No live endpoint is supported by this application.

```dotenv
PAYPAL_CLIENT_ID=
PAYPAL_CLIENT_SECRET=
PAYPAL_WEBHOOK_ID=
PAYPAL_RETURN_URL=http://127.0.0.1:5180/
```

Restart the Java API after changing credentials. With Compose: `docker compose up -d --force-recreate platform-api`. With the local launcher: `node scripts/dev.mjs stop`, then `node scripts/dev.mjs start`.

Configured means values are present, not that PayPal connectivity has been verified. Without credentials, the AI review/case/reporting demo works, but checkout fails explicitly with 503. No mock payment is substituted at runtime.

## Complete a payment

1. Create a draft in USD, GBP or EUR. Only its persisted amount and currency are sent to PayPal; the browser cannot replace them during checkout.
2. Open the payment, choose a sample scenario and assess it. Inspect the score, features and model limitations.
3. Create a Sandbox order. The server uses Orders v2 with `intent=AUTHORIZE` and a durable PayPal-Request-Id.
4. Open Buyer approval, sign in with the Sandbox buyer and approve. The return page is navigation only; query-string values do not authorize or settle anything. Return to the original workspace tab.
5. Select Authorize approved order. PayPal must have recorded buyer approval. A successful authorization is a hold, not a capture.
6. Record a review with a reason. HOLD prevents capture. RELEASE enables the explicit capture command. BLOCK enables void after authorization.
7. Confirm capture or void. Check the provider IDs and activity. Reconcile refreshes authoritative status from PayPal.

## Webhooks

Expose a secured HTTPS endpoint, `https://YOUR_HOST/api/paypal/webhooks`, and register it against the same Sandbox app. Set PAYPAL_WEBHOOK_ID to the registration's ID. Subscribe to checkout-order approval and relevant authorization/capture events. The local Compose services intentionally bind to loopback; public deployment or a securely configured temporary tunnel is a separate operator step.

This endpoint alone is exempt from session authentication and CSRF. It requires PayPal signature headers and successful PayPal postback signature verification. Unverified events cannot change state. Verified known-order events trigger a provider GET/reconciliation, not blind application of the event payload. Receipt IDs deduplicate successful deliveries. Processing failures return errors for provider retry. Unrelated order IDs are acknowledged without altering local payments.

PayPal simulator webhooks do not support postback verification. Test this implementation with actual Sandbox app events. Automated tests use a local HTTP fixture and do not prove real PayPal connectivity.

## Retry and recovery boundaries

Operation IDs are committed before network I/O. Concurrent requests use a 45-second lease; retry the same command after an uncertain result, never a fresh draft to work around it. A pending operation locks cancellation and review changes. Retries retain the same PayPal request ID and stop after five hours, conservatively inside the default order idempotency window. If no provider ID was returned and that window expires, manual Sandbox-account reconciliation is required. Do not delete database rows to bypass it.

Full captures only. Refunds, disputes, partial captures, reauthorization, live PayPal, multi-merchant onboarding, scheduled reconciliation and production operation recovery tooling are outside this demo. Unsupported provider states fail visibly instead of being guessed. External refunds/disputes must be investigated in PayPal.

References: [authorize then capture](https://developer.paypal.com/v5/checkout/auth-capture), [Orders v2](https://developer.paypal.com/api/orders/v2), [Payments v2](https://developer.paypal.com/api/payments/v2), [idempotency](https://developer.paypal.com/api/rest/reference/idempotency/), [webhook verification](https://developer.paypal.com/api/rest/webhooks/rest/).
