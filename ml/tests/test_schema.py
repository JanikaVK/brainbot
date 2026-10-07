from app.schema import WellnessFeatures
from pydantic import ValidationError
import pytest

BASE = dict(mood=3, energy=3, sleep_quality=3, workload=3, concentration=3, stress=3, social_connection=3, study_hours=4, break_frequency=2)

def test_valid_features():
    assert WellnessFeatures(**BASE).mood == 3

def test_out_of_range_rejected():
    with pytest.raises(ValidationError):
        WellnessFeatures(**{**BASE, "stress": 9})
