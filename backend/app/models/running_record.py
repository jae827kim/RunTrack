"""
러닝 기록 모델
"""
from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class RunningRecord(Base):
    """러닝 기록 테이블"""
    __tablename__ = "running_records"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    shoe_id = Column(Integer, ForeignKey("shoes.id"))
    
    # 기본 정보
    title = Column(String)
    description = Column(Text)
    
    # 거리/시간
    distance_km = Column(Float, nullable=False)
    duration_minutes = Column(Integer, nullable=False)
    start_time = Column(DateTime, nullable=False)
    end_time = Column(DateTime, nullable=False)
    
    # 성능 지표
    avg_pace_min_per_km = Column(Float)  # 분/km
    max_speed_kmh = Column(Float)
    avg_speed_kmh = Column(Float)
    
    # 신체 정보
    avg_heart_rate = Column(Integer)
    max_heart_rate = Column(Integer)
    calories_burned = Column(Float)
    
    # GPS & 고도
    elevation_gain_m = Column(Float)
    elevation_loss_m = Column(Float)
    max_altitude_m = Column(Float)
    
    # GPS 경로 (GeoJSON 또는 polyline)
    gps_route = Column(Text)  # JSON 형식
    
    # 기상 조건
    temperature_c = Column(Float)
    humidity_percent = Column(Integer)
    wind_speed_kmh = Column(Float)
    
    # 러닝 조건
    route_type = Column(String)  # "road", "trail", "track", "mixed"
    surface_type = Column(String)  # "asphalt", "trail", "track", "mixed"
    difficulty = Column(String)  # "easy", "moderate", "hard"
    
    # 기분/감정
    feeling = Column(String)  # "great", "good", "okay", "tired", "bad"
    notes = Column(Text)
    
    # 타임스탐프
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # 관계
    user = relationship("User", back_populates="running_records")
    shoe = relationship("Shoe", back_populates="running_records")
