# Demonstration outline

Keep the final recording below three minutes. Only claim the flows actually shown running. Never reveal `.env`, passwords, client secrets or real customer data in the video.

## 0:00-0:20 / Problem and boundaries

PayReason AI is a PayPal Sandbox payment-risk review prototype. It combines a synthetic-data ML experiment with exact model contributions and a human-controlled capture gate. Explain that the model is not validated on real fraud.

## 0:20-1:10 / Assessment

Open a payment draft, assess the elevated-risk sample, show the score and the actual feature values/contributions. Explain that payment state and review decision are distinct. Show the immutable assessment, reviewer reason and audit timeline.

## 1:10-2:10 / PayPal flow

With configured Sandbox credentials, create an order, complete buyer approval, authorize, and inspect the PayPal IDs. Show HOLD preventing capture, then explicitly RELEASE and capture. Use a second pre-authorized Sandbox example to show BLOCK followed by void when time permits. Do not replace a missing real integration with fixture footage labelled as PayPal.

## 2:10-2:45 / Case evidence

Open the case queue, filter by channel/decision, then ask the latest-case or count question. Open a cited record. Counts come from the database; this is constrained query reporting, not generative RAG.

## 2:45-2:55 / Close

Show the repository README, reproducible synthetic-training report and Docker instructions. State outstanding limitations briefly. Include only verified links in the submission.

## Upload checklist

- Source archive inspected for credentials, local databases, private documents and dependency folders.
- README and model card accurately distinguish implemented code, fixture-tested integration and real Sandbox verification.
- Configure Sandbox credentials locally and run capture, void and real signed webhook tests before claiming the PayPal demo is complete.
- Create the intended GitHub repository and set the visibility required by the event rules.
- Record and upload the final video to YouTube, then add its verified link to the submission.
- Review the current event eligibility, existing-project disclosure and deadline rules before submitting.
