# Architecture

## Runtime boundaries

React talks to the Java API through a same-origin proxy. Java owns authentication, the payment register, risk evidence, reviews, audit and PayPal operations. PostgreSQL stores these records. The private Python service loads a versioned JSON logistic-regression artifact and returns scores with exact feature contributions.

Java domains: identity, payments, paypal, risk, audit, configuration and shared error handling. Entities remain with their owning domain. No external fraud-management checkout or database is required.

## API contract

All routes below except session bootstrap/login and signed webhook delivery require an administrator session. Every browser mutation requires a fresh CSRF token from GET /api/auth/csrf. The UI does this automatically. Swagger users must authenticate and supply the returned header/token for mutations.

| Route | Purpose |
| --- | --- |
| GET /api/auth/csrf | CSRF token and header name |
| POST /api/auth/login | URL-encoded username/password |
| GET /api/auth/me | Current user |
| POST /api/auth/logout | End session |
| GET /api/payments | Paginated payment register and status filter |
| POST /api/payments | Create draft; UUID Idempotency-Key required |
| GET /api/payments/summary | Record counts, not mixed-currency totals |
| GET /api/payments/{id} | Payment and provider IDs |
| POST /api/payments/{id}/cancel | Cancel a local draft with no unresolved operation |
| GET /api/payments/{id}/activity | Actor/timestamp/review-reason history |
| POST /api/payments/{id}/paypal/order | Create AUTHORIZE-intent Sandbox order |
| POST /api/payments/{id}/paypal/authorize | Authorize a buyer-approved order |
| POST /api/payments/{id}/paypal/capture | Full capture; RELEASE review required |
| POST /api/payments/{id}/paypal/void | Void authorization; BLOCK review required |
| POST /api/payments/{id}/paypal/reconcile | Fetch authoritative provider state |
| GET /api/payments/{id}/paypal/operations | Durable operation identities and pending state |
| POST /api/payments/{id}/risk | Assess immutable supplied scenario features |
| GET /api/payments/{id}/risk | Persisted score and evidence |
| POST /api/payments/{id}/risk/review | HOLD, RELEASE or BLOCK plus reason |
| GET /api/cases | Paginated assessments; channel/year/decision filters |
| POST /api/insights/questions | Constrained supported questions with cited records |
| POST /api/paypal/webhooks | Signature-verified provider notifications |
| GET /api/integrations | Configuration / model readiness, not fake connectivity |

Payment creation:

```json
{"customerReference":"DEMO-CUSTOMER","amount":"75.25","currency":"GBP","channel":"WEB"}
```

The server assigns `reference` as `PR-` followed by the payment UUID's 32 uppercase hexadecimal characters. It is returned in the response, not accepted in the creation request. Reusing the same Idempotency-Key with the same payment details returns the original record and reference; different details return 409. Existing records retain their references.

Assessment input (synthetic sample signals):

```json
{"amount_ratio":10,"account_age_days":2,"transactions_last_hour":8,"new_device":true,"cross_border":true}
```

Review:

```json
{"decision":"BLOCK","notes":"Synthetic scenario reviewed: unusual velocity and new device."}
```

## Payment safety

A database row lock serializes review/cancellation with operation preparation. The operation UUID and retry lease commit before external I/O. The resulting provider state commits afterwards. Uncertain outcomes retain PENDING and the original key; neither reviewer edits nor cancellation can bypass that gate. Duplicate successful commands return the persisted payment. A bounded five-hour retry window prevents reusing keys beyond the conservative provider retention limit.

Capture requires an authorization and a persisted RELEASE review. Void requires BLOCK. HOLD never captures. Payment states are separate from assessment bands and review decisions. Pending captures are not displayed as completed. Provider amounts, currency, order reference and identifiers are checked before applying returned state.

Webhooks are verified through PayPal's verification API, deduplicated by event ID and used to trigger authoritative GET reconciliation. They do not initiate captures or trust event ordering. Only the exact webhook route bypasses browser session and CSRF checks.

## Risk and reporting

Python uses a deterministic synthetic-data model trained by scikit-learn. Evidence, input signals, model version, actor and timestamp persist with the payment ID. The assessment cannot be overwritten by altered inputs. Review reasons append to the activity history even when the current decision changes.

Case counts are database aggregates over the complete filtered result, not counts of the first page. Years use assessedAt in UTC with inclusive start/exclusive next-year bounds. Latest means latest assessment, with ID tie-breaking. Supported natural-language patterns map to these fixed filters; arbitrary SQL or generated answers are never executed. No LLM or vector index is connected.

## Known boundaries

Single administrator workspace, synthetic features, full Sandbox captures only. No outbox/broker consumers, automatic reconciliation worker, multitenancy, customer-facing checkout site, live payments or production identity controls. See paypal-sandbox.md and model-card.md before extending these boundaries.
