"""Reproducible synthetic-data experiment. Not evidence of real fraud performance."""
import hashlib
import json
from pathlib import Path

import numpy as np
import sklearn
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, brier_score_loss, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

ROOT = Path(__file__).resolve().parents[2]
FEATURES = ["amount_ratio", "account_age_days", "transactions_last_hour", "new_device", "cross_border"]


def train():
    rng = np.random.default_rng(20261106)
    n = 12000
    x = np.column_stack([
        np.clip(rng.lognormal(0, 0.9, n), 0.01, 30),
        rng.integers(0, 3651, n), rng.poisson(2, n),
        rng.binomial(1, 0.2, n), rng.binomial(1, 0.3, n),
    ])
    # Labels deliberately come from a documented simulation, not real payment outcomes.
    logit = -4 + 0.5 * x[:, 0] - 0.0008 * x[:, 1] + 0.5 * x[:, 2] + 1.4 * x[:, 3] + 0.6 * x[:, 4]
    y = rng.binomial(1, 1 / (1 + np.exp(-logit)))
    train_x, test_x, train_y, test_y = train_test_split(x, y, test_size=0.2, stratify=y, random_state=17)
    train_x, val_x, train_y, val_y = train_test_split(train_x, train_y, test_size=0.25, stratify=train_y, random_state=19)
    model = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000, random_state=23))
    model.fit(train_x, train_y)
    scaler, estimator = model.steps[0][1], model.steps[1][1]
    artifact = {
        "version": "synthetic-logistic-v1", "source": "SYNTHETIC_DEMO", "seed": 20261106,
        "features": FEATURES, "mean": scaler.mean_.tolist(), "scale": scaler.scale_.tolist(),
        "coefficients": estimator.coef_[0].tolist(), "intercept": float(estimator.intercept_[0]),
        "review_threshold": 0.25, "high_threshold": 0.6,
        "limitation": "Synthetic-data model. Scores are not validated probabilities of real fraud. Human review required.",
    }
    report = {"source": "Synthetic generator in ml/training/train_demo.py; no external data",
              "seed": 20261106, "rows": n, "train_rows": len(train_y), "validation_rows": len(val_y),
              "test_rows": len(test_y), "positive_rows": int(y.sum()), "sklearn_version": sklearn.__version__,
              "dataset_sha256": hashlib.sha256(x.tobytes() + y.tobytes()).hexdigest(),
              "thresholds": "Illustrative fixed bands 0.25 and 0.60, not optimized for real fraud costs.",
              "limitations": artifact["limitation"]}
    for name, xx, yy in [("validation", val_x, val_y), ("test", test_x, test_y)]:
        pred = model.predict_proba(xx)[:, 1]
        report[name] = {"roc_auc": float(roc_auc_score(yy, pred)),
                        "average_precision": float(average_precision_score(yy, pred)),
                        "brier_score": float(brier_score_loss(yy, pred)), "positive_rate": float(yy.mean())}
    # Safe JSON export: no executable pickle is loaded by the API.
    destination = ROOT / "services/ai-service/app/demo_model.json"
    destination.write_text(json.dumps(artifact, indent=2) + "\n")
    (ROOT / "ml/evaluation/synthetic-report.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    train()
