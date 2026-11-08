# Development provenance

This hackathon edition starts from an existing fraud-management foundation and experience. The initial independent scaffold retains the Java 21 / Spring Boot and React / TypeScript stack, clean domain layering, immutable API records, activity tracking and a separate Python modelling boundary.

The initial scaffold creates a new payment-draft domain, local session authentication, merchant console and an explicit inference-service contract. It does not copy private client documents, local databases, trained research artifacts, secrets or Git history. It does not require access to the original private repository.

PayPal integration and the payment-focused AI workflow are new hackathon work. Record completed milestones in the repository and describe the significant additions accurately in the final submission.

Added on 6 November 2026: Sandbox adapter and operation journal; capture/void review gates; signed webhook verification and reconciliation; reproducible synthetic logistic-regression training and exact contributions; immutable risk records; review reasons, case queue and database-backed questions; browser tests and Docker/source-package instructions. Real PayPal Sandbox calls and final submission media still require separate verification. No previous private training dataset or fitted model was copied.

Dependency licenses remain applicable. The repository uses Apache-2.0; preserve notices on any source files imported later. The existing account-fraud research model must not be represented as validated for PayPal payments.

Verified on 8 November 2026: the running application created a GBP 1.00 order against real PayPal Sandbox and displayed its buyer-approval link. Buyer approval and settlement were not completed in that check. The submission materials distinguish this result from the broader fixture-tested integration. The latest refinements also include server-generated order references and an explicitly opt-in local-only demo login.
