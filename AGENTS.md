# PayReason AI

Independent hackathon application. Do not edit or import runtime paths from the private OEFMS checkout.

- Java 21, Spring Boot 4, React/TypeScript, PostgreSQL and a private Python inference API.
- Keep each Java domain layered: entities, enums, repository, services, controller and dto.
- Payment status and risk decisions are separate concepts. A local draft is not a PayPal order.
- Never fabricate scores, payment success, model readiness or retrieved evidence.
- Money uses BigDecimal and a currency; do not aggregate different currencies.
- All credentials and generated data stay out of Git. Sandbox credentials belong on the backend.
- New integrations need bounded timeouts, meaningful failure states and focused tests.
- Run Maven verify, web tests/build and Python tests for the components changed.
- Do not publish, push, or create a remote without the user's request.
