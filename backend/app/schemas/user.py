"""
사용자 Pydantic 스키마
"""
from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List

class UserBase(BaseModel):
    username: str
    email: str
    weight_kg: Optional[float] = None
    height_cm: Optional[int] = None
    foot_size: Optional[str] = None
    foot_width: Optional[str] = None
    arch_type: Optional[str] = None
    running_style: Optional[str] = None
    budget_won: Optional[int] = None
    preferred_brands: Optional[List[str]] = None

class UserCreate(UserBase):
    password: str

class UserUpdate(BaseModel):
    weight_kg: Optional[float] = None
    height_cm: Optional[int] = None
    foot_size: Optional[str] = None
    foot_width: Optional[str] = None
    arch_type: Optional[str] = None
    running_style: Optional[str] = None
    budget_won: Optional[int] = None
    preferred_brands: Optional[List[str]] = None

class UserResponse(UserBase):
    id: int
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

class UserLogin(BaseModel):
    username: str
    password: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str
    expires_in: int
