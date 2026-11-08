# Judge walkthrough

PayReason is a local, reproducible Sandbox demonstration, not a production fraud
detector. The source contains the fitted synthetic model, migrations and all
application components. No hosted deployment is needed to run it.

## Start the application

Install Docker with Compose, then:

```sh
git clone https://github.com/richodia1/PayReason-AI.git
cd PayReason-AI
cp .env.example .env
docker compose up --build -d
```

Open http://127.0.0.1:5180 and sign in with `admin` / `payreason-local-only`.
Those are local-only example credentials from `.env.example`; the author's
private machine settings are not distributed. Wait for Java startup before
signing in. Health is at http://127.0.0.1:8180/actuator/health.

Containers bind to loopback. Do not expose example passwords publicly.
On a fresh checkout there are no pre-seeded payments or cases.

## Try AI review (no PayPal credentials required)

1. Choose **New draft**. Enter `10.00`, currency `GBP`, a fictional customer
   reference such as `JUDGE-SAMPLE`, and channel `Web`. Save the draft.
2. Open its generated `PR-...` reference.
3. Choose **Elevated-risk sample**, then **Assess sample**.
4. Inspect the score, five observed feature values and exact log-odds contributions.
   These come from the Python model, not hard-coded risk results. The inputs are
   editable demonstration signals, not telemetry automatically obtained from PayPal.
5. Record **Block** with a reason such as `Synthetic sample reviewed: new device
   and elevated velocity.` This records a reviewer decision; it does not label a
   real person as fraudulent or move money.
6. Close the detail panel and open **Cases**. Filter the assessed cases and reopen
   the evidence. Refresh the page to confirm persistence.
7. Open **Case insights** and ask `What is the latest case?` or
   `How many blocked cases from web this year?`. Open a cited record. These are
   constrained database queries, not a general-purpose chatbot or RAG.

## Try the PayPal Sandbox lifecycle

Create your own Sandbox REST app and distinct personal buyer account using the
[PayPal Developer Dashboard](https://developer.paypal.com/dashboard/applications/sandbox).
Set its client ID and secret in your local `.env`, then run:

```sh
docker compose up -d --force-recreate platform-api
```

Do not put credentials in an issue, public README, video or source archive.
For a new GBP 1.00 draft, follow the [Sandbox walkthrough](paypal-sandbox.md):
create an order, approve it with the personal test buyer, authorize, assess,
record **Release** with a review reason, then explicitly capture. Use a second
test order to demonstrate **Block**, followed by void. Keep everything in Sandbox.

Expected gates: without RELEASE, capture is rejected; BLOCK is required to void.
An authorization is a hold, not settlement. Network failures remain visible and
unresolved operations retain their original retry identity.

As of 8 November, real order creation is verified. Buyer-approved authorization,
capture/void and signed webhooks remain separate manual verification steps.
Automated tests cover those provider contracts using fixtures. Webhooks need a
secured reachable HTTPS endpoint; they are not necessary for this manual-command
walkthrough, which also exposes **Reconcile**.

## Troubleshooting

- Sign-in unavailable: wait for API startup and check `docker compose ps`.
- Missing credentials: AI review still works; PayPal operations return an error.
- Buyer approval fails: use a Sandbox personal buyer, distinct from the merchant.
- Old open browser tab: reload it after upgrading the application.
- Expired Sandbox order: create a fresh demonstration draft and order. Do not
  delete operation records to bypass recovery locks.
- Ports in use: change the relevant ports in `.env`. Keep `PAYPAL_RETURN_URL`
  aligned with the web port and use the actual URLs.
- Inspect logs locally without sharing credentials or personal data. Never post `.env`.

Use `docker compose stop` when finished; this keeps the local test database.

See [verification](verification.md), [model card](model-card.md) and
[architecture](architecture.md) for test scope and limitations.
