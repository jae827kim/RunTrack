"""
GPS 위치점 모델 (러닝 중 실시간 위치 데이터)
"""
from sqlalchemy import Column, Integer, Float, DateTime, ForeignKey, String
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class GPSPoint(Base):
    """GPS 위치점 테이블 (러닝 세션 중 수집된 위치 데이터)"""
    __tablename__ = "gps_points"
    
    id = Column(Integer, primary_key=True, index=True)
    running_record_id = Column(Integer, ForeignKey("running_records.id", ondelete="CASCADE"), nullable=False)
    
    # 위치 좌표
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    altitude_m = Column(Float)  # 고도 (미터)
    
    # 성능 지표
    speed_kmh = Column(Float)  # 현재 속도
    accuracy_m = Column(Float)  # GPS 정확도 (미터)
    bearing_degrees = Column(Float)  # 방향 (0-360도)
    
    # 시간 정보
    timestamp = Column(DateTime, nullable=False)
    
    # 관계
    running_record = relationship("RunningRecord", back_populates="gps_points")
    
    class Config:
        from_attributes = True
