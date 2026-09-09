"""
신발 관리 API 라우트
- 신발 등록, 조회, 수정, 삭제
- 신발별 누적 킬로 추적
"""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter()

# 신발 등록
@router.post("")
async def create_shoe(db: Session = Depends(get_db)):
    pass

# 신발 목록 조회
@router.get("")
async def get_shoes(db: Session = Depends(get_db)):
    pass

# 신발 상세 조회
@router.get("/{shoe_id}")
async def get_shoe(shoe_id: int, db: Session = Depends(get_db)):
    pass

# 신발 수정
@router.put("/{shoe_id}")
async def update_shoe(shoe_id: int, db: Session = Depends(get_db)):
    pass

# 신발 삭제
@router.delete("/{shoe_id}")
async def delete_shoe(shoe_id: int, db: Session = Depends(get_db)):
    pass

# 신발 통계
@router.get("/{shoe_id}/stats")
async def get_shoe_stats(shoe_id: int, db: Session = Depends(get_db)):
    pass
