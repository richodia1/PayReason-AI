import json
import math
from pathlib import Path

MODEL_PATH = Path(__file__).with_name("demo_model.json")


def load_model():
    return json.loads(MODEL_PATH.read_text()) if MODEL_PATH.exists() else None


def assess(features, model):
    contributions = []
    for name, mean, scale, coefficient in zip(model["features"], model["mean"], model["scale"], model["coefficients"], strict=True):
        value = float(features[name])
        contribution = coefficient * ((value - mean) / scale)
        contributions.append({"feature": name, "value": value, "log_odds_contribution": contribution,
                              "direction": "INCREASES" if contribution > 0 else "DECREASES"})
    log_odds = model["intercept"] + sum(item["log_odds_contribution"] for item in contributions)
    score = 1 / (1 + math.exp(-max(-700, min(700, log_odds))))
    band = "HIGH" if score >= model["high_threshold"] else "MEDIUM" if score >= model["review_threshold"] else "LOW"
    contributions.sort(key=lambda item: abs(item["log_odds_contribution"]), reverse=True)
    leading = contributions[0]
    return {"score": score, "band": band, "model_version": model["version"], "source": model["source"],
            "base_log_odds": model["intercept"], "contributions": contributions, "features": features,
            "human_review_required": True, "limitation": model["limitation"],
            "explanation": f"{band.title()} synthetic risk band. The strongest contribution is {leading['feature'].replace('_', ' ')}, which {leading['direction'].lower()} the model's log-odds relative to its training baseline. This is model evidence, not proof of fraud."}
