from pydantic import BaseModel, Field


class WellnessFeatures(BaseModel):
    mood: int = Field(ge=1, le=5)
    energy: int = Field(ge=1, le=5)
    sleep_quality: int = Field(ge=1, le=5)
    workload: int = Field(ge=1, le=5)
    concentration: int = Field(ge=1, le=5)
    stress: int = Field(ge=1, le=5)
    social_connection: int = Field(ge=1, le=5)
    study_hours: float = Field(ge=0, le=24)
    break_frequency: int = Field(ge=0, le=30)


class PredictionResponse(BaseModel):
    status: str
    message: str
    model_version: str | None = None
    prediction: str | None = None
    probabilities: dict[str, float] | None = None
    explanation: list[dict[str, str | float]] | None = None
