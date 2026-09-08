"""
신발 모델
"""
from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class Shoe(Base):
    """신발 테이블"""
    __tablename__ = "shoes"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    
    # 신발 정보
    brand = Column(String, nullable=False)  # "Nike", "Adidas" 등
    model = Column(String, nullable=False)
    size = Column(String)
    color = Column(String)
    
    # 구매 정보
    purchase_price_won = Column(Integer)
    purchase_date = Column(DateTime)
    
    # 사용 통계
    cumulative_km = Column(Float, default=0.0)
    run_count = Column(Integer, default=0)
    
    # 신발 상태
    condition = Column(String, default="good")  # "good", "fair", "worn"
    notes = Column(String)
    
    # 타임스탐프
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # 관계
    user = relationship("User", back_populates="shoes")
    running_records = relationship("RunningRecord", back_populates="shoe")
