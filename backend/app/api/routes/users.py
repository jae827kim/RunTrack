"""
사용자 관련 API 라우트
- 회원가입, 로그인, 프로필 관리
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter()

@router.post("/register")
async def register(db: Session = Depends(get_db)):
    """사용자 회원가입"""
    return {"message": "회원가입 기능은 아직 구현 중입니다"}

@router.post("/login")
async def login(db: Session = Depends(get_db)):
    """사용자 로그인"""
    return {"message": "로그인 기능은 아직 구현 중입니다"}

@router.get("/me")
async def get_profile(db: Session = Depends(get_db)):
    """현재 사용자 프로필 조회"""
    return {"message": "프로필 조회는 아직 구현 중입니다"}

@router.put("/me")
async def update_profile(db: Session = Depends(get_db)):
    """사용자 프로필 수정"""
    return {"message": "프로필 수정은 아직 구현 중입니다"}
