# Verification record

Checked locally on 6 November 2026.

| Check | Result |
| --- | --- |
| Java 21 Maven verify | 11 HTTP integration tests passed |
| React form tests | 2 tests passed |
| TypeScript and Vite production build | Passed |
| Python model/API tests | 8 tests passed |
| Playwright on local services and on Docker | 2 end-to-end scenarios passed in each setup |
| Full Docker image build | Java API, Python service and web image built |
| Docker runtime | PostgreSQL, Java API, Python inference and web console started |
| API health and model readiness | Verified locally |
| Desktop/mobile screenshots | Inspected at 1440x960 and 390x844 |
| Production npm dependency audit | 0 reported vulnerabilities |
| Git whitespace check | Passed |

## Coverage

Java: authentication/CSRF; draft precision, validation and deduplication; cancellation; actor attribution; PayPal order/authorization; buyer-approval requirement; capture blocked before RELEASE; BLOCK leading to void; provider failure after capture with same-key retry; review lock during uncertain outcomes; immutable assessment; model-unavailable failure; query filters and database-backed questions; invalid and duplicate webhooks.

PayPal and Java-to-AI integration tests use a local HTTP fixture. They are not evidence of real Sandbox credentials, buyer approval, capture/void or signed provider delivery.

Python: trained-model inference, low/high synthetic scenarios, exact reconstruction from contributions, feature bounds, invalid money and no-score response when the artifact is absent.

Browser: sign-in, draft save/modal dismissal, persistence after reload, cancellation, audit, real Python synthetic scoring, persisted review reason, case queue, evidence queries, mobile navigation and responsive layout. Browser tests leave clearly named synthetic DEMO/RISK-DEMO records in the local PostgreSQL database.

The synthetic training command ran successfully and produced the committed model and evaluation JSON. The model card records the data generator and limitations.

## Remaining release gates

- Configure PayPal Sandbox credentials and complete a real buyer approval, authorization, capture, void and signed webhook test.
- Provide the intended GitHub remote and publish the reviewed source.
- Record the final demonstration and add verified repository/demo/video links to the submission.
- Validate any future public deployment independently. This run is loopback-only, with local demo authentication.

No production financial transaction was attempted. No real customer data, PayPal secrets or LLM credentials were used. The original private project was not modified.

Non-blocking tooling warnings: Java/Jackson deprecated API notices, Mockito's future dynamic-agent warning, Starlette's httpx test-client deprecation, and Vite's single-bundle size advisory. These are not failed tests. GitHub Actions is configured but has not run remotely because this repository has not been pushed.
