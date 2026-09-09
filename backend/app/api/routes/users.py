"""
사용자 관련 API 라우트
- 회원가입, 로그인, 프로필 관리
"""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db

router = APIRouter()

# 회원가입
@router.post("/register")
async def register(db: Session = Depends(get_db)):
    pass

# 로그인
@router.post("/login")
async def login(db: Session = Depends(get_db)):
    pass

# 프로필 조회
@router.get("/me")
async def get_profile(db: Session = Depends(get_db)):
    pass

# 프로필 수정
@router.put("/me")
async def update_profile(db: Session = Depends(get_db)):
    pass
