"""Train a versioned ensemble from a verified, permissioned CSV. No data is bundled."""
import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from lightgbm import LGBMClassifier
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier, VotingClassifier
from sklearn.impute import SimpleImputer
from sklearn.metrics import (accuracy_score, classification_report, confusion_matrix,
                             f1_score, precision_score, recall_score, roc_auc_score)
from sklearn.model_selection import cross_val_score, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import LabelEncoder, StandardScaler
from xgboost import XGBClassifier

NUMERIC = ["mood", "energy", "sleep_quality", "workload", "concentration", "stress", "social_connection", "study_hours", "break_frequency"]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", default=Path("artifacts"), type=Path)
    args = parser.parse_args()
    frame = pd.read_csv(args.input)
    required = set(NUMERIC + ["target"])
    missing = required - set(frame.columns)
    if missing:
        raise SystemExit(f"Missing required columns: {sorted(missing)}")
    frame = frame[NUMERIC + ["target"]].dropna(subset=["target"])
    for column in NUMERIC:
        frame[column] = pd.to_numeric(frame[column], errors="coerce")
    ordinal = ["mood", "energy", "sleep_quality", "workload", "concentration", "stress", "social_connection"]
    for column in ordinal:
        values = frame[column].dropna()
        if ((values < 1) | (values > 5)).any():
            raise SystemExit(f"Values for {column} must use the documented 1–5 scale.")
    for column, low, high in [("study_hours", 0, 24), ("break_frequency", 0, 30)]:
        values = frame[column].dropna()
        if ((values < low) | (values > high)).any():
            raise SystemExit(f"Values for {column} must be between {low} and {high}.")
    if frame["target"].nunique() < 2 or len(frame) < 50:
        raise SystemExit("Need at least 50 labeled rows and two target classes; no model was trained.")
    X = frame[NUMERIC]
    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(frame["target"].astype(str))
    if int(pd.Series(y).value_counts().min()) < 5:
        raise SystemExit("Each class needs at least five labeled rows for a stratified holdout and cross-validation.")
    X_train_val, X_test, y_train_val, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)
    X_train, X_val, y_train, y_val = train_test_split(X_train_val, y_train_val, test_size=0.25, random_state=43, stratify=y_train_val)
    pre = ColumnTransformer([("numeric", Pipeline([("impute", SimpleImputer(strategy="median")), ("scale", StandardScaler())]), NUMERIC)], remainder="drop")
    models = {
        "random_forest": RandomForestClassifier(n_estimators=400, class_weight="balanced", random_state=42),
        "xgboost": XGBClassifier(n_estimators=300, max_depth=4, learning_rate=0.04, subsample=0.85, colsample_bytree=0.85, eval_metric="mlogloss", random_state=42),
        "lightgbm": LGBMClassifier(n_estimators=300, class_weight="balanced", verbosity=-1, random_state=42),
    }
    estimators = [(name, model) for name, model in models.items()]
    models["ensemble"] = VotingClassifier(estimators=estimators, voting="soft", weights=[1, 1, 1])
    candidates = {}
    cv_n = min(5, int(y_train.value_counts().min()))
    if cv_n < 2:
        raise SystemExit("Each class needs at least two training examples for stratified cross-validation.")
    for name, estimator in models.items():
        pipe = Pipeline([("preprocess", pre), ("model", estimator)])
        scores = cross_val_score(pipe, X_train, y_train, cv=cv_n, scoring="f1_macro", n_jobs=1)
        pipe.fit(X_train, y_train)
        validation_f1 = float(f1_score(y_val, pipe.predict(X_val), average="macro"))
        candidates[name] = (validation_f1, pipe)
        print(f"{name} cross-validation macro-F1: mean={scores.mean():.4f} std={scores.std():.4f}; validation macro-F1: {validation_f1:.4f}")
    winner = max(candidates, key=lambda name: candidates[name][0])
    pipeline = candidates[winner][1].fit(X_train_val, y_train_val)
    predicted = pipeline.predict(X_test)
    probs = pipeline.predict_proba(X_test)
    labels = list(pipeline.classes_)
    metrics = {"accuracy": float(accuracy_score(y_test, predicted)), "precision_macro": float(precision_score(y_test, predicted, average="macro", zero_division=0)), "recall_macro": float(recall_score(y_test, predicted, average="macro", zero_division=0)), "f1_macro": float(f1_score(y_test, predicted, average="macro")), "confusion_matrix": confusion_matrix(y_test, predicted, labels=labels).tolist(), "classification_report": classification_report(y_test, predicted, output_dict=True, zero_division=0)}
    if len(labels) == 2:
        metrics["roc_auc"] = float(roc_auc_score(y_test, probs[:, 1]))
    version = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    args.output.mkdir(parents=True, exist_ok=True)
    class_labels = [str(label) for label in label_encoder.classes_]
    background = X_train_val.sample(n=min(32, len(X_train_val)), random_state=44)
    joblib.dump({"pipeline": pipeline, "version": version, "model_name": winner, "features": NUMERIC, "classes": labels, "class_labels": class_labels, "explanation_background": background}, args.output / "model.joblib")
    (args.output / "metrics.json").write_text(json.dumps({"version": version, "selected_model": winner, "held_out_metrics": metrics}, indent=2))
    print(json.dumps({"version": version, "selected_model": winner, "held_out_metrics": metrics}, indent=2))


if __name__ == "__main__":
    main()
