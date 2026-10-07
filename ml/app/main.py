from __future__ import annotations

import os
from pathlib import Path
from typing import Any

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException

from app.schema import PredictionResponse, WellnessFeatures

MODEL_DIR = Path(os.getenv("MODEL_DIR", "artifacts"))
MODEL_PATH = MODEL_DIR / "model.joblib"
app = FastAPI(title="BrainBot ML Inference", version="0.1.0")


def load_artifact() -> dict[str, Any] | None:
    if not MODEL_PATH.is_file():
        return None
    try:
        value = joblib.load(MODEL_PATH)
        if not isinstance(value, dict) or "pipeline" not in value or "version" not in value:
            return None
        return value
    except Exception:
        return None


@app.get("/health")
def health():
    return {"status": "ok", "model_available": load_artifact() is not None}


@app.get("/model/version")
def model_version():
    artifact = load_artifact()
    if artifact is None:
        return {"available": False, "version": None, "message": "A trained model artifact is not installed."}
    return {"available": True, "version": artifact["version"]}


@app.post("/predict", response_model=PredictionResponse)
def predict(features: WellnessFeatures):
    artifact = load_artifact()
    if artifact is None:
        raise HTTPException(status_code=503, detail="Prediction is unavailable until a verified dataset has been used to train and install a model.")
    try:
        pipeline = artifact["pipeline"]
        row = features.model_dump()
        probabilities = pipeline.predict_proba(pd.DataFrame([row]))[0]
        classes = pipeline.classes_
        best = int(probabilities.argmax())
        class_labels = artifact.get("class_labels", [str(label) for label in classes])
        return PredictionResponse(status="available", message="Model output is an estimate, not a medical diagnosis.", model_version=artifact["version"], prediction=class_labels[best], probabilities={class_labels[int(label)]: float(prob) for label, prob in zip(classes, probabilities)})
    except Exception as exc:
        raise HTTPException(status_code=503, detail="The installed model could not process this request.") from exc


@app.post("/explain")
def explain(features: WellnessFeatures):
    artifact = load_artifact()
    if artifact is None:
        raise HTTPException(status_code=503, detail="Explanations are unavailable until a trained model is installed.")
    try:
        import shap
        row = features.model_dump()
        pipeline = artifact["pipeline"]
        background = artifact["explanation_background"]
        def predict_batch(values):
            return pipeline.predict_proba(pd.DataFrame(values, columns=list(row)))
        explainer = shap.Explainer(predict_batch, shap.maskers.Independent(background, max_samples=len(background)), algorithm="permutation")
        values = explainer(pd.DataFrame([row]), max_evals=2 * len(row) + 1)
        predicted_class = int(predict_batch([row])[0].argmax())
        contributions = values.values[0]
        if getattr(contributions, "ndim", 1) > 1:
            contributions = contributions[:, predicted_class]
        ranked = sorted(zip(row.keys(), contributions), key=lambda item: abs(float(item[1])), reverse=True)[:5]
        return {"status": "available", "model_version": artifact["version"], "explanation": [{"feature": name, "contribution": float(value)} for name, value in ranked], "message": "Feature contributions describe this model output; they do not establish causation or diagnosis."}
    except Exception as exc:
        raise HTTPException(status_code=503, detail="An explanation method is not available for the installed model.") from exc
