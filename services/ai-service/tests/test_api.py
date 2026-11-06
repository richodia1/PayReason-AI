from uuid import uuid4
import math
from unittest.mock import patch
import pytest

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def payload(**features):
    return {"payment_id": str(uuid4()), "amount": "15.50", "currency": "GBP", "channel": "WEB",
            "features": {"amount_ratio": 1, "account_age_days": 1000, "transactions_last_hour": 1,
                         "new_device": False, "cross_border": False, **features}}


def test_model_reports_synthetic_source():
    assert client.get("/health").json()["status"] == "UP"
    assert client.get("/capabilities").json()["source"] == "SYNTHETIC_DEMO"


def test_scores_and_exact_explanation_reconstruction():
    response = client.post("/v1/scores", json=payload())
    assert response.status_code == 200
    result = response.json()
    reconstructed = 1 / (1 + math.exp(-(result["base_log_odds"] + sum(c["log_odds_contribution"] for c in result["contributions"])) ))
    assert result["score"] == pytest.approx(reconstructed)
    assert result["human_review_required"] is True
    assert result["band"] == "LOW"
    high = client.post("/v1/scores", json=payload(amount_ratio=15, account_age_days=1, transactions_last_hour=12, new_device=True)).json()
    assert high["band"] == "HIGH"
    assert high["score"] > result["score"]


def test_missing_model_fails_closed():
    with patch("app.main.load_model", return_value=None):
        assert client.post("/v1/scores", json=payload()).status_code == 503


@pytest.mark.parametrize("field,value", [("amount_ratio", -1), ("amount_ratio", 31), ("transactions_last_hour", 1.5), ("account_age_days", 5000)])
def test_invalid_features(field, value):
    assert client.post("/v1/scores", json=payload(**{field: value})).status_code == 422


def test_invalid_payment_is_rejected():
    response = client.post("/v1/scores", json={"payment_id": str(uuid4()),
        "amount": "-1.00", "currency": "GBP", "channel": "WEB"})
    assert response.status_code == 422
