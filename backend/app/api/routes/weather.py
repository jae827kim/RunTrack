"""
기상 데이터 & 조언 API 라우트
- 현재 기상 조건 기반 체감온도 계산
- 러닝 추천 점수 (0-100)
"""
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter()

# 러닝 조언
@router.get("/advice")
async def get_running_advice(
    latitude: float = Query(...),
    longitude: float = Query(...),
    db: Session = Depends(get_db)
):
    pass

# 기상 이력
@router.get("/history")
async def get_weather_history(
    latitude: float = Query(...),
    longitude: float = Query(...),
    days: int = Query(7),
    db: Session = Depends(get_db)
):
    pass
