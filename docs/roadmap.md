# Demo milestones

Progress is tracked for this independent PayReason edition. Percentages are rough planning estimates, not production-readiness measurements.

1. Implemented and locally tested: independent console, API, database, authentication, drafts and audit.
2. Verified against real PayPal Sandbox on 8 November 2026: authenticated order creation and buyer-approval link. Buyer approval still needs a Sandbox personal account.
3. Implemented with HTTP fixture tests: authorization, review-gated full capture/void, durable retry identity and recovery locks.
4. Implemented with HTTP fixture tests: signature verification, webhook receipt deduplication and manual/event-driven reconciliation. Real signed Sandbox delivery remains unverified.
5. Implemented: five-feature synthetic-data contract and reproducible training, split, metrics and model card.
6. Implemented: Python inference using the committed trained synthetic model. No validation on real fraud data.
7. Implemented: exact feature contributions, persisted immutable evidence and human review reasons.
8. Implemented: basic case queue, channel/year/decision filters, review workflow and audit history. Assignment, attachments and enterprise case lifecycle are future work.
9. Implemented: constrained latest-case/count questions against real database records. Generative RAG remains future work.
10. Public source repository published with Apache-2.0 licensing. Setup instructions, source packaging, submission text and a timed recording script are provided. Final buyer-approved Sandbox proof and a YouTube recording remain operator steps.

Current planning estimate: approximately **85% toward the submission-ready demo**, with the remaining gates above. This is a narrower demonstration than a production fraud platform.

Do not mark this 100% submission-ready while live Sandbox verification and the actual submission assets are missing.
