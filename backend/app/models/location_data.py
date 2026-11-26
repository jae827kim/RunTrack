"""
GPS 위치 추적 모델
"""
from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class LocationData(BaseModel):
    """GPS 위치 데이터"""
    latitude: float
    longitude: float
    altitude: Optional[float] = None
    speed: Optional[float] = None
    accuracy: Optional[float] = None
    bearing: Optional[float] = None
    timestamp: datetime

class RunningSession(BaseModel):
    """러닝 세션 정보"""
    id: Optional[int] = None
    user_id: int
    shoe_id: Optional[int] = None
    start_time: datetime
    end_time: Optional[datetime] = None
    total_distance_km: Optional[float] = None
    duration_minutes: Optional[int] = None
    locations: list[LocationData] = []
