"""
환경설정 및 상수 정의
"""
import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    # 프로젝트
    PROJECT_NAME = "RunTrack API"
    VERSION = "0.1.0"
    
    # 데이터베이스
    DATABASE_URL = os.getenv(
        "DATABASE_URL",
        "postgresql://user:password@localhost:5432/runtrack"
    )
    
    # Redis
    REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")
    
    # API 키
    GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
    GOOGLE_MAPS_API_KEY = os.getenv("GOOGLE_MAPS_API_KEY")
    OPENWEATHER_API_KEY = os.getenv("OPENWEATHER_API_KEY")
    
    # JWT
    SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-change-in-production")
    ALGORITHM = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES = 30
    
    # CORS
    ALLOWED_ORIGINS = [
        "http://localhost:3000",
        "http://localhost:8080",
        "http://localhost:5173",
    ]
    
    # 개발 모드
    DEBUG = os.getenv("DEBUG", "False") == "True"

settings = Settings()
