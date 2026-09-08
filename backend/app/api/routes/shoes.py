"""
신발 관리 API 라우트
- 신발 등록, 조회, 수정, 삭제
- 신발별 누적 킬로 추적
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter()

@router.post("")
async def create_shoe(db: Session = Depends(get_db)):
    """새 신발 등록"""
    return {"message": "신발 등록은 아직 구현 중입니다"}

@router.get("")
async def get_shoes(db: Session = Depends(get_db)):
    """사용자의 모든 신발 조회"""
    return {
        "shoes": [],
        "message": "신발 목록은 아직 구현 중입니다"
    }

@router.get("/{shoe_id}")
async def get_shoe(shoe_id: int, db: Session = Depends(get_db)):
    """신발 상세 정보 조회"""
    return {"message": "신발 상세 조회는 아직 구현 중입니다"}

@router.put("/{shoe_id}")
async def update_shoe(shoe_id: int, db: Session = Depends(get_db)):
    """신발 정보 수정"""
    return {"message": "신발 수정은 아직 구현 중입니다"}

@router.delete("/{shoe_id}")
async def delete_shoe(shoe_id: int, db: Session = Depends(get_db)):
    """신발 삭제"""
    return {"message": "신발 삭제는 아직 구현 중입니다"}

@router.get("/{shoe_id}/stats")
async def get_shoe_stats(shoe_id: int, db: Session = Depends(get_db)):
    """신발 통계 조회 (누적 킬로, 러닝 횟수 등)"""
    return {
        "total_kilometers": 0,
        "run_count": 0,
        "message": "신발 통계는 아직 구현 중입니다"
    }
