"""
기상 데이터 & 조언 API 라우트
- 현재 기상 조건 기반 체감온도 계산
- 러닝 추천 점수 (0-100)
"""
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter()

@router.get("/advice")
async def get_running_advice(
    latitude: float = Query(...),
    longitude: float = Query(...),
    db: Session = Depends(get_db)
):
    """
    현재 위치의 기상 조건 기반 러닝 조언
    
    Response:
    - perceived_temperature: 체감온도
    - running_score: 러닝 추천 점수 (0-100)
    - condition: 최적/좋음/괜찮음/나쁨/매우나쁨
    - tips: 추천 조언 리스트
    """
    return {
        "latitude": latitude,
        "longitude": longitude,
        "temperature": 18.5,
        "perceived_temperature": 17.2,
        "humidity": 65,
        "wind_speed": 3.5,
        "uv_index": 4,
        "running_score": 85,
        "condition": "좋음",
        "tips": [
            "맑은 날씨로 러닝하기 좋습니다",
            "자외선 지수가 높으니 선크림을 바르세요",
            "충분한 수분 섭취를 권장합니다"
        ],
        "message": "기상 조언은 아직 구현 중입니다"
    }

@router.get("/history")
async def get_weather_history(
    latitude: float = Query(...),
    longitude: float = Query(...),
    days: int = Query(7),
    db: Session = Depends(get_db)
):
    """과거 기간의 기상 데이터 조회"""
    return {
        "location": {"latitude": latitude, "longitude": longitude},
        "days": days,
        "history": [],
        "message": "기상 이력은 아직 구현 중입니다"
    }
