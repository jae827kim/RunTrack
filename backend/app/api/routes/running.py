"""
러닝 기록 API 라우트
- 러닝 세션 시작/종료, 기록 저장
- 러닝 기록 조회, 통계 분석
"""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter()

# 러닝 세션 시작
@router.post("/start")
async def start_running(db: Session = Depends(get_db)):
    pass

# 러닝 세션 종료
@router.post("/{session_id}/end")
async def end_running(session_id: str, db: Session = Depends(get_db)):
    pass

# 러닝 기록 조회
@router.get("")
async def get_running_records(skip: int = 0, limit: int = 10, db: Session = Depends(get_db)):
    pass

# 러닝 기록 상세
@router.get("/{record_id}")
async def get_running_record(record_id: int, db: Session = Depends(get_db)):
    pass

# 전체 통계 요약
@router.get("/statistics/summary")
async def get_summary_statistics(db: Session = Depends(get_db)):
    pass

# 주간 통계
@router.get("/statistics/weekly")
async def get_weekly_statistics(db: Session = Depends(get_db)):
    pass

# 월간 통계
@router.get("/statistics/monthly")
async def get_monthly_statistics(db: Session = Depends(get_db)):
    pass
