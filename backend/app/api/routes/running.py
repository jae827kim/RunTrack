"""
러닝 기록 API 라우트
- 러닝 세션 시작/종료, 기록 저장
- 러닝 기록 조회, 통계 분석
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter()

@router.post("/start")
async def start_running(db: Session = Depends(get_db)):
    """러닝 세션 시작"""
    return {
        "session_id": "session-uuid",
        "message": "러닝 세션 시작은 아직 구현 중입니다"
    }

@router.post("/{session_id}/end")
async def end_running(session_id: str, db: Session = Depends(get_db)):
    """러닝 세션 종료 및 저장"""
    return {
        "distance_km": 5.0,
        "duration_minutes": 35,
        "avg_pace": "7:00",
        "message": "러닝 세션 종료는 아직 구현 중입니다"
    }

@router.get("")
async def get_running_records(
    skip: int = 0,
    limit: int = 10,
    db: Session = Depends(get_db)
):
    """사용자의 러닝 기록 조회"""
    return {
        "records": [],
        "total": 0,
        "message": "러닝 기록 조회는 아직 구현 중입니다"
    }

@router.get("/statistics/summary")
async def get_summary_statistics(db: Session = Depends(get_db)):
    """전체 러닝 통계 요약"""
    return {
        "total_distance_km": 0,
        "total_runs": 0,
        "avg_distance_km": 0,
        "avg_pace": "0:00",
        "total_time_minutes": 0,
        "message": "통계 요약은 아직 구현 중입니다"
    }

@router.get("/statistics/weekly")
async def get_weekly_statistics(db: Session = Depends(get_db)):
    """주간 러닝 통계"""
    return {
        "week_data": [],
        "message": "주간 통계는 아직 구현 중입니다"
    }

@router.get("/statistics/monthly")
async def get_monthly_statistics(db: Session = Depends(get_db)):
    """월간 러닝 통계"""
    return {
        "month_data": [],
        "message": "월간 통계는 아직 구현 중입니다"
    }

@router.get("/{record_id}")
async def get_running_record(record_id: int, db: Session = Depends(get_db)):
    """러닝 기록 상세 조회"""
    return {"message": "러닝 기록 상세 조회는 아직 구현 중입니다"}
