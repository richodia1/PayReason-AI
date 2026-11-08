# Verification record

Latest checks: 8 November 2026.

| Check | Result |
| --- | --- |
| Java 21 Maven verify | 17 tests passed: 12 HTTP integration tests and 5 security configuration tests |
| React form tests | 2 tests passed |
| TypeScript and Vite production build | Passed |
| Python model/API tests | 8 tests passed |
| Playwright on local services and on Docker | 2 end-to-end scenarios passed in each setup |
| Full Docker image build | All three images built on 8 November; the Java image compiled directly from source |
| Docker runtime | PostgreSQL, Java API, Python inference and web console started |
| API health and model readiness | Verified locally |
| Desktop/mobile screenshots | Inspected at 1440x960 and 390x844 |
| Submission gallery | Four actual 1440x960 screenshots prepared; fictional records only |
| Production npm dependency audit | 0 reported vulnerabilities |
| Git whitespace check | Passed |
| Credential checks | Current source and 103 historical blobs checked against local PayPal credentials and common private-key/token patterns; no matches. This is not a comprehensive security audit. |
| Real PayPal Sandbox authentication and order creation | Passed on 8 November; a GBP 1.00 test order and buyer-approval link were returned by PayPal |
| Public repository | Published at https://github.com/richodia1/PayReason-AI with Apache-2.0 licensing |

## Coverage

Java: authentication/CSRF; opt-in local demo login; draft precision, validation and deduplication; server-generated references preserved on retries; rejection of manual references and matching OpenAPI schema; cancellation; actor attribution; PayPal order/authorization; buyer-approval requirement; capture blocked before RELEASE; BLOCK leading to void; provider failure after capture with same-key retry; review lock during uncertain outcomes; immutable assessment; model-unavailable failure; query filters and database-backed questions; invalid and duplicate webhooks.

PayPal and Java-to-AI integration tests use a local HTTP fixture. The separate manual Sandbox check created a real test order through the running application, after an assessment by the real Python service. It stopped at the buyer login page: it does not verify buyer approval, authorization, capture, void or signed provider delivery. No real money was involved.

Python: trained-model inference, low/high synthetic scenarios, exact reconstruction from contributions, feature bounds, invalid money and no-score response when the artifact is absent.

Browser: sign-in, draft save/modal dismissal, persistence after reload, cancellation, audit, real Python synthetic scoring, persisted review reason, case queue, evidence queries, mobile navigation and responsive layout. Browser tests leave synthetic records identified by DEMO/RISK-DEMO customer references in the local PostgreSQL database; order references are server-generated.

The synthetic training command ran successfully and produced the committed model and evaluation JSON. The model card records the data generator and limitations.

## Remaining release gates

- Complete real Sandbox buyer approval, authorization, capture and void tests. Credentials are present locally, but are never distributed with the source.
- Register a secured HTTPS webhook endpoint and verify actual signed Sandbox events, or explicitly retain webhook delivery as an unverified integration limitation.
- Before final submission, confirm the submitted commit has a successful [GitHub Actions run](https://github.com/richodia1/PayReason-AI/actions).
- Record the final demonstration and add verified repository/demo/video links to the submission.
- Validate any future public deployment independently. This run is loopback-only, with local demo authentication.

No production financial transaction was attempted. Only fictional local records and a Sandbox test order were used. Sandbox credentials are held in the ignored local environment; no secrets or customer data are included in the repository. The original private project was not modified.

Non-blocking tooling warnings: Java/Jackson deprecated API notices, Mockito's future dynamic-agent warning, Starlette's httpx test-client deprecation, and Vite's single-bundle size advisory. These are not failed tests. The first published commit has a [successful GitHub Actions run](https://github.com/richodia1/PayReason-AI/actions/runs/42236739078); this does not substitute for verification of later commits.
