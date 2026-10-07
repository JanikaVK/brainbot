from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)

VALID = {
    "mood": 3,
    "energy": 3,
    "sleep_quality": 3,
    "workload": 3,
    "concentration": 3,
    "stress": 3,
    "social_connection": 3,
    "study_hours": 4,
    "break_frequency": 2,
}


def test_health_is_live_without_model():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"
    assert response.json()["model_available"] is False


def test_prediction_is_not_fabricated_without_artifact():
    response = client.post("/predict", json=VALID)
    assert response.status_code == 503
    assert "unavailable" in response.json()["detail"]


def test_prediction_schema_rejects_out_of_range_input():
    response = client.post("/predict", json={**VALID, "stress": 9})
    assert response.status_code == 422
