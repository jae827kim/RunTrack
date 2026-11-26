"""
GPS 위치점 Pydantic 스키마
"""
from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class GPSPointBase(BaseModel):
    latitude: float
    longitude: float
    altitude_m: Optional[float] = None
    speed_kmh: Optional[float] = None
    accuracy_m: Optional[float] = None
    bearing_degrees: Optional[float] = None
    timestamp: datetime

class GPSPointCreate(GPSPointBase):
    pass

class GPSPointResponse(GPSPointBase):
    id: int
    running_record_id: int
    
    class Config:
        from_attributes = True

class GPSRouteResponse(BaseModel):
    """러닝 경로 (모든 GPS 포인트)"""
    running_record_id: int
    points: list[GPSPointResponse]
    total_distance_km: float
    start_point: GPSPointResponse
    end_point: GPSPointResponse
    
    class Config:
        from_attributes = True
