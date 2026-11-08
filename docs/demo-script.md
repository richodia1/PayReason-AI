# Three-minute demonstration plan

Target length: **2 minutes 45 seconds**, leaving a margin under the three-minute
limit. Use actual application footage, not slides masquerading as working payment
operations. No music or external artwork is needed.

## Before recording

- Start the application and reload old open browser tabs.
- Use a desktop viewport around 1440 x 960; close unrelated tabs and notifications.
- Sign in before recording. Do not record passwords, `.env`, developer credentials,
  buyer login, account balances or personal details.
- Prepare fictional GBP 1.00 Sandbox examples using distinct test buyer and
  merchant accounts. Complete buyer approval immediately before recording.
- Keep one low-risk, authorized example on HOLD ready for RELEASE/capture, and
  one high-risk, authorized example ready for BLOCK/void.
- If buyer approval is unavailable, show only order creation, model evidence and
  review controls, and explicitly state that settlement has not been demonstrated.
  Do not narrate capture/void as successful or use fixture footage as live evidence.

## 0:00-0:15 / Payment register

> PayReason AI connects payment risk explanations with human review before
> capture. This is a PayPal Sandbox prototype for merchants, not a production
> fraud detector. All examples and training data are synthetic.

Show the running merchant workspace and open the prepared sample.

## 0:15-0:50 / Risk assessment and evidence

> A logistic regression model evaluates five signals, including transaction
> velocity and a new-device flag. The interface shows the observed values and
> each feature's exact contribution to the score. This explains the model's
> calculation, not proof that a customer committed fraud.

Assess a fresh sample or show its immutable assessment. Keep the score, observed
features and contribution bars visible. Do not imply that PayPal supplies these
demo signals automatically.

## 0:50-1:35 / Human-controlled PayPal capture

Use this narration **only after live Sandbox verification**:

> This buyer-approved payment is authorized, but not captured. The assessment
> starts on HOLD. I record my review reason and release the payment. Capture is a
> separate explicit action. PayPal confirms the result, and the application stores
> the capture ID alongside its activity history.

Show authorized status and HOLD. Save RELEASE with a demonstration note,
confirm capture, and wait for the actual provider result before describing it.
If PayPal returns PENDING, say pending and reconcile; do not call it settled.

Fallback while buyer approval is pending:

> The application has created this order in real PayPal Sandbox and received a
> buyer-approval link. Buyer-approved settlement is still awaiting verification;
> the authorization, capture and void contracts are covered by fixture tests.

## 1:35-1:55 / Block and void

For a second authorized test payment, show high-risk evidence, record BLOCK with
a reason, and explicitly void. Show the actual VOIDED result and activity.
Skip this live claim if not verified; use the time to show the persisted BLOCK
review and explain that a review decision alone does not move money.

## 1:55-2:25 / Case queue and evidence query

> Every assessed payment appears in the case queue. I can filter by channel,
> decision and year, then inspect the evidence. These supported questions use
> database queries and link to matching cases. They are not a generative
> chatbot, and a case is not automatically confirmed fraud.

Ask `How many blocked cases from web this year?`, show its result and open a
cited case. Do not read a prewritten number; the count changes with local records.

## 2:25-2:45 / Reproducibility and close

> The public repository includes Docker instructions, the reproducible synthetic
> model and automated tests. PayReason keeps the score, the human decision and
> the payment outcome distinct, so reviewers can inspect what happened and why.

Show the repository and briefly mention any still-unverified integration stages.

## Export and submission

Record the application window with the operating system's screen recorder and a
microphone, or another recorder you already trust. Export a clear MP4 where possible.
Review the complete video before uploading; remove any accidental credential or
personal-data exposure. Keep it below three minutes.

Upload to YouTube with visibility that lets judges watch without requesting access
or entering a password, following the current event instructions. Test the final
link while signed out. Add that actual URL to Devpost, along with the repository
and the accurate text in [the submission pack](submission.md).

Current status: **script prepared; video not yet recorded or uploaded**.
