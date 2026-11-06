"""Synthetic research-model inference, never a production fraud decision."""

from decimal import Decimal
from typing import Literal
from uuid import UUID

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, ConfigDict, Field
from app.model import assess, load_model

app = FastAPI(title="PayReason AI Service", version="0.1.0")


class RiskFeatures(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    amount_ratio: float = Field(ge=0.01, le=30)
    account_age_days: int = Field(ge=0, le=3650)
    transactions_last_hour: int = Field(ge=0, le=50)
    new_device: bool
    cross_border: bool


class ScoreRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    payment_id: UUID
    amount: Decimal = Field(gt=0, max_digits=14, decimal_places=2)
    currency: Literal["GBP", "USD", "EUR"]
    channel: Literal["WEB", "MOBILE"]
    features: RiskFeatures


@app.get("/health")
def health():
    return {"status": "UP", "service": "payreason-ai-service"}


@app.get("/capabilities")
def capabilities():
    model = load_model()
    return {"scoring_available": model is not None, "explanations_available": model is not None,
            "model_version": model["version"] if model else None, "source": "SYNTHETIC_DEMO",
            "reason": "Synthetic research model only. Not validated on real payments."}


@app.post("/v1/scores")
def score(request: ScoreRequest):
    model = load_model()
    if model is None:
        raise HTTPException(status_code=503, detail={"code": "MODEL_NOT_AVAILABLE",
            "message": "No model artifact installed.", "payment_id": str(request.payment_id)})
    return {"payment_id": str(request.payment_id), **assess(request.features.model_dump(), model)}
