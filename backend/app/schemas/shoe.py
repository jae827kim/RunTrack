"""
신발 Pydantic 스키마
"""
from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class ShoeBase(BaseModel):
    brand: str
    model: str
    size: Optional[str] = None
    color: Optional[str] = None
    purchase_price_won: Optional[int] = None
    purchase_date: Optional[datetime] = None
    notes: Optional[str] = None

class ShoeCreate(ShoeBase):
    pass

class ShoeUpdate(BaseModel):
    brand: Optional[str] = None
    model: Optional[str] = None
    size: Optional[str] = None
    color: Optional[str] = None
    purchase_price_won: Optional[int] = None
    condition: Optional[str] = None
    notes: Optional[str] = None

class ShoeStats(BaseModel):
    cumulative_km: float
    run_count: int
    condition: str

class ShoeResponse(ShoeBase):
    id: int
    user_id: int
    cumulative_km: float
    run_count: int
    condition: str
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
