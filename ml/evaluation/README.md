# Payment model evaluation

A trained synthetic logistic-regression demonstration model is included in `services/ai-service/app/demo_model.json`. Reproduce it with `ml/training/train_demo.py`. The generated `synthetic-report.json` records held-out metrics and provenance. See `docs/model-card.md` for the generator, feature definitions, exact explanation method and limitations.

Before enabling real-world decisions, obtain representative labelled payment data and record source/licensing, authoritative feature availability at authorization time, time-aware splits, leakage checks, average precision, precision/recall at chosen thresholds, calibration and a model card. Select business thresholds using validation data and report untouched test results. The demo's random synthetic split and illustrative bands do not meet that production requirement.

The existing account-application fraud dataset is not a drop-in source of PayPal checkout features. Synthetic demonstration data and measured research results must be identified separately. A sandbox payment outcome is not a fraud label.

Keep private raw data and research artifacts in ignored folders. The included JSON artifact is a redistributable synthetic fixture generated for this project, not copied from private research.
