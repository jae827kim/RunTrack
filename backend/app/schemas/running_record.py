"""
러닝 기록 Pydantic 스키마
"""
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator
from datetime import datetime, timezone
from typing import Literal, Optional
from uuid import UUID

class RunningRecordBase(BaseModel):
    model_config = ConfigDict(allow_inf_nan=False)
    title: Optional[str] = None
    description: Optional[str] = None
    distance_km: float = Field(gt=0)
    duration_minutes: int = Field(gt=0, le=2147483647)
    start_time: datetime
    end_time: datetime

class RunningRecordCreate(RunningRecordBase):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)
    shoe_id: Optional[int] = Field(default=None, gt=0, le=2147483647)
    avg_pace_min_per_km: Optional[float] = Field(default=None, gt=0)
    max_speed_kmh: Optional[float] = Field(default=None, ge=0)
    avg_speed_kmh: Optional[float] = Field(default=None, ge=0)
    avg_heart_rate: Optional[int] = Field(default=None, gt=0, le=2147483647)
    elevation_gain_m: Optional[float] = Field(default=None, ge=0)
    temperature_c: Optional[float] = None
    feeling: Optional[str] = None
    notes: Optional[str] = None

    @field_validator("start_time", "end_time")
    @classmethod
    def normalize_time(cls, value):
        # Existing DB columns are timezone-naive; consistently store UTC.
        return value.astimezone(timezone.utc).replace(tzinfo=None) if value.tzinfo else value

    @model_validator(mode="after")
    def ordered_times(self):
        if self.end_time <= self.start_time:
            raise ValueError("end_time must be after start_time")
        return self

class RunningRecordUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)

    title: Optional[str] = None
    description: Optional[str] = None
    shoe_id: Optional[int] = Field(default=None, gt=0, le=2147483647)
    distance_km: Optional[float] = Field(default=None, gt=0)
    duration_minutes: Optional[int] = Field(default=None, gt=0, le=2147483647)
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    feeling: Optional[str] = None
    notes: Optional[str] = None

    @field_validator("distance_km", "duration_minutes", "start_time", "end_time")
    @classmethod
    def required_if_supplied(cls, value):
        if value is None:
            raise ValueError("field cannot be null")
        if isinstance(value, datetime) and value.tzinfo:
            return value.astimezone(timezone.utc).replace(tzinfo=None)
        return value

class RunningRecordResponse(RunningRecordBase):
    id: int
    user_id: int
    shoe_id: Optional[int]
    avg_pace_min_per_km: Optional[float]
    max_speed_kmh: Optional[float]
    avg_speed_kmh: Optional[float]
    avg_heart_rate: Optional[int]
    calories_burned: Optional[float]
    elevation_gain_m: Optional[float]
    temperature_c: Optional[float]
    feeling: Optional[str]
    notes: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)

class RunningStatistics(BaseModel):
    total_distance_km: float
    total_runs: int
    avg_distance_km: float
    avg_pace_min_per_km: float
    total_duration_minutes: int
    avg_duration_minutes: float
    total_calories: float
    avg_heart_rate: Optional[float]
    avg_elevation_gain_m: Optional[float]


class RunningSessionStart(BaseModel):
    model_config = ConfigDict(extra="forbid")

    shoe_id: Optional[int] = Field(default=None, gt=0, le=2147483647)
    start_time: Optional[datetime] = None

    @field_validator("start_time")
    @classmethod
    def normalize_time(cls, value):
        if value is not None and value.tzinfo:
            return value.astimezone(timezone.utc).replace(tzinfo=None)
        return value


class RunningSessionResponse(BaseModel):
    session_id: UUID
    start_time: datetime
    shoe_id: Optional[int]
    status: Literal["active"] = "active"


class RunningSessionEnd(BaseModel):
    model_config = ConfigDict(extra="forbid", allow_inf_nan=False)

    distance_km: float = Field(gt=0)
    duration_minutes: int = Field(gt=0, le=2147483647)
    end_time: datetime
    title: Optional[str] = None
    description: Optional[str] = None
    max_speed_kmh: Optional[float] = Field(default=None, ge=0)
    avg_heart_rate: Optional[int] = Field(default=None, gt=0, le=2147483647)
    elevation_gain_m: Optional[float] = Field(default=None, ge=0)
    temperature_c: Optional[float] = None
    feeling: Optional[str] = None
    notes: Optional[str] = None

    @field_validator("end_time")
    @classmethod
    def normalize_time(cls, value):
        return value.astimezone(timezone.utc).replace(tzinfo=None) if value.tzinfo else value
