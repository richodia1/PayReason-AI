# Devpost submission pack

Prepared 8 November 2026. This is draft submission text, not evidence that a
Devpost entry or YouTube upload has been completed.

## General information

Project name: **PayReason AI**

Elevator pitch (under 200 characters):

> Explain payment risk before capture: PayReason AI combines PayPal Sandbox, transparent model evidence and human review to help merchants make informed payment decisions.

## Project story

### Inspiration

A risk score alone does not explain what a merchant should do next. I wanted to
connect payment operations with understandable model evidence and a deliberate
human decision before settlement. PayReason explores that workflow using PayPal
Sandbox, without pretending that a research score proves fraud.

### What it does

PayReason provides a merchant workspace for registering payment drafts, assessing
synthetic risk signals, reviewing cases and inspecting decision history. Every
assessment starts on HOLD. A reviewer must record a reason and choose RELEASE
before the application permits capture, or BLOCK before it permits voiding an
authorization. Review status and payment status remain separate.

The integration implements PayPal Sandbox order creation, buyer approval,
authorization, capture, void and reconciliation. Real Sandbox order creation and
its buyer-approval link have been verified. The remaining settlement operations
are covered by automated provider fixtures and still require buyer-approved
Sandbox verification before they can be described as demonstrated end to end.

### How I built it

The platform uses Java 21, Spring Boot, PostgreSQL and Flyway behind a React and
TypeScript console. A separate Python FastAPI service evaluates a logistic
regression model trained with scikit-learn on 12,000 generated records. Exact
feature contributions reconstruct the model's score in log-odds space, allowing
reviewers to inspect the observed signals and their effects.

The application persists assessment snapshots, review reasons and audit events.
Idempotency keys protect draft creation, while durable provider-operation IDs
support controlled retries. Case insights answer supported count and latest-case
questions from database records and link back to the evidence; they do not rely
on generated answers or claim to implement RAG.

### Challenges and learning

The most important challenge was keeping uncertain provider responses separate
from successful payments. A timeout must not be treated as proof that no action
occurred. The implementation keeps unresolved operations visible and reuses
their retry identities. Another challenge was presenting explanations honestly:
feature contributions explain this model, not real-world causality or confirmed
fraud. Synthetic experiment metrics are not evidence of real-payment accuracy.

### What I am proud of

The working prototype connects risk evidence, reviewer reasons and payment
controls in one persisted workflow. The repository includes a reproducible
model, automated Java, Python and browser tests, and Docker setup instructions
so others can inspect and run it.

### Existing foundation and new work

This project draws on my previous fraud-management work and engineering
experience. The independent payment-focused application, PayPal integration,
synthetic model workflow and review-gated settlement are the additions developed
for this hackathon edition. Private documents, customer datasets, fitted research
models and the original repository's history were not copied into this project.

### What's next

Complete buyer-approved Sandbox settlement and signed-webhook verification, then
explore representative permitted datasets, stronger model evaluation, reviewer
roles and operational recovery tools. This is a Sandbox research prototype,
not a production fraud prevention product.

## Built-with tags

Java, Spring Boot, React, TypeScript, Python, FastAPI, PostgreSQL, Docker,
PayPal, scikit-learn, Machine Learning, Vite, Material UI, Flyway, Maven,
Playwright, pytest, JUnit, GitHub Actions.

Do not add OpenAI, GPT, RAG, SHAP, XGBoost or other tools that this edition does
not actually use. Choose available Devpost tag equivalents where necessary.

## Links

- Code: https://github.com/richodia1/PayReason-AI
- Run instructions: https://github.com/richodia1/PayReason-AI/blob/main/docs/judge-guide.md
- Video: **pending recording and YouTube upload**. Do not submit a placeholder.
- Hosted demo: none. Do not use a localhost address as a public demo link.

## Final checklist

- [x] Public Apache-2.0 source repository.
- [x] Local application and automated tests.
- [x] Real Sandbox order creation and buyer-approval URL.
- [x] Reproducible model, limitations and judge setup instructions.
- [x] Four actual application screenshots prepared in `docs/images/`; see [gallery and captions](gallery.md).
- [ ] At submission time, confirm the latest source is on GitHub and its [CI checks](https://github.com/richodia1/PayReason-AI/actions) are green.
- [ ] Buyer-approved Sandbox authorization, capture and void demonstrated.
- [ ] Signed webhooks verified, or clearly disclosed as unverified.
- [ ] Previously shared Sandbox secret rotated locally; no secrets in submission materials.
- [ ] Record a video shorter than three minutes and upload it to YouTube.
- [ ] Check video visibility and playback without account access or a password.
- [ ] Replace draft verification claims with the final observed results.
- [ ] Enter all required Devpost fields and confirm final submission, not just draft saving.

The event page lists a deadline of **12 November 2026, 12:00 PST / 20:00 GMT**.
Recheck the official page before submission. The rules permit a runnable
repository with complete instructions instead of a hosted demo; a video remains
required. Never place API secrets in the public repository to provide judge access.

Sources: [event requirements](https://paypalaihackathon.devpost.com/),
[official rules](https://paypalaihackathon.devpost.com/rules).
