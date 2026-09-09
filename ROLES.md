# 👥 RunTrack 팀 역할 정의 및 담당 업무

**프로젝트**: AI 기반 맞춤형 러닝 조언 앱 (RunTrack)  
**팀 구성**: 5명 (PM, Backend 1, Backend 2, Frontend, DevOps & QA)  
**개발 기간**: 7주  
**저장소**: https://github.com/jae827kim/RunTrack

---

## 📋 목차

1. [PM (프로젝트 매니저)](#-pm-프로젝트-매니저)
2. [Backend 1 (사용자 & 신발 관리)](#-backend-1-사용자--신발-관리)
3. [Backend 2 (러닝기록 & 기상정보 & AI)](#-backend-2-러닝기록--기상정보--ai)
4. [Frontend (Kotlin/Android)](#-frontend-kotlinandroid)
5. [DevOps & QA (배포 & 테스트)](#-devops--qa-배포--테스트)
6. [개발 일정](#-개발-일정)

---

## 🔴 PM (프로젝트 매니저)

### 담당 역할

- 전체 프로젝트 일정 관리
- 팀 회의 진행 및 조율
- 데이터베이스 설계
- ORM 모델 작성
- Pydantic 검증 스키마 작성
- 통합 테스트 작성
- 최종 보고서 및 발표 자료 준비

### 구현해야 할 파일

```
backend/app/models/
  ├── __init__.py
  ├── user.py              # User ORM 모델
  ├── shoe.py              # Shoe ORM 모델
  └── running_record.py    # RunningRecord ORM 모델

backend/app/schemas/
  ├── __init__.py
  ├── user.py              # UserCreate, UserResponse, UserUpdate
  ├── shoe.py              # ShoeCreate, ShoeResponse, ShoeUpdate
  └── running_record.py    # RunningRecordCreate, RunningRecordResponse

backend/tests/
  └── test_integration.py  # 통합 테스트 (20개)
```

### DB 설계 (1주차)

**ERD (Entity Relationship Diagram)**
```
User (1) ─── (N) Shoe
User (1) ─── (N) RunningRecord
Shoe (1) ─── (N) RunningRecord
```

**User 테이블**
```
- id (PK, Integer)
- email (String, Unique)
- password (String, Hashed)
- name (String)
- weight_kg (Float, 선택)
- height_cm (Integer, 선택)
- created_at (DateTime)
- updated_at (DateTime)
```

**Shoe 테이블**
```
- id (PK, Integer)
- user_id (FK, Integer) → User.id
- brand (String)
- model (String)
- size (String)
- color (String, 선택)
- purchase_date (Date)
- cumulative_km (Float, default=0)
- created_at (DateTime)
- updated_at (DateTime)
```

**RunningRecord 테이블**
```
- id (PK, Integer)
- user_id (FK, Integer) → User.id
- shoe_id (FK, Integer) → Shoe.id
- distance_km (Float)
- duration_minutes (Integer)
- date (Date)
- start_time (DateTime)
- end_time (DateTime)
- avg_pace_min_per_km (Float)
- temperature_c (Float, 선택)
- humidity_percent (Integer, 선택)
- wind_speed_kmh (Float, 선택)
- feeling (String, 선택)
- created_at (DateTime)
- updated_at (DateTime)
```

### ORM 모델 작성 (2주차)

**app/models/user.py**
```python
from sqlalchemy import Column, Integer, String, Float, DateTime
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class User(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    password = Column(String)  # hashed
    name = Column(String)
    weight_kg = Column(Float, nullable=True)
    height_cm = Column(Integer, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # 관계
    shoes = relationship("Shoe", back_populates="user", cascade="all, delete-orphan")
    running_records = relationship("RunningRecord", back_populates="user", cascade="all, delete-orphan")
```

**app/models/shoe.py**
```python
from sqlalchemy import Column, Integer, String, Float, Date, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class Shoe(Base):
    __tablename__ = "shoes"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    brand = Column(String)
    model = Column(String)
    size = Column(String)
    color = Column(String, nullable=True)
    purchase_date = Column(Date)
    cumulative_km = Column(Float, default=0.0)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # 관계
    user = relationship("User", back_populates="shoes")
    running_records = relationship("RunningRecord", back_populates="shoe")
```

**app/models/running_record.py**
```python
from sqlalchemy import Column, Integer, Float, String, Date, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime
from app.database import Base

class RunningRecord(Base):
    __tablename__ = "running_records"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    shoe_id = Column(Integer, ForeignKey("shoes.id"))
    distance_km = Column(Float)
    duration_minutes = Column(Integer)
    date = Column(Date)
    start_time = Column(DateTime)
    end_time = Column(DateTime)
    avg_pace_min_per_km = Column(Float, nullable=True)
    temperature_c = Column(Float, nullable=True)
    humidity_percent = Column(Integer, nullable=True)
    wind_speed_kmh = Column(Float, nullable=True)
    feeling = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # 관계
    user = relationship("User", back_populates="running_records")
    shoe = relationship("Shoe", back_populates="running_records")
```

### Pydantic 스키마 작성 (2주차)

**app/schemas/user.py**
```python
from pydantic import BaseModel, EmailStr
from datetime import datetime
from typing import Optional

class UserBase(BaseModel):
    email: EmailStr
    name: str
    weight_kg: Optional[float] = None
    height_cm: Optional[int] = None

class UserCreate(UserBase):
    password: str

class UserUpdate(BaseModel):
    name: Optional[str] = None
    weight_kg: Optional[float] = None
    height_cm: Optional[int] = None

class UserResponse(UserBase):
    id: int
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True
```

**app/schemas/shoe.py**
```python
from pydantic import BaseModel
from datetime import datetime, date
from typing import Optional

class ShoeBase(BaseModel):
    brand: str
    model: str
    size: str
    color: Optional[str] = None
    purchase_date: date

class ShoeCreate(ShoeBase):
    pass

class ShoeUpdate(BaseModel):
    brand: Optional[str] = None
    model: Optional[str] = None
    size: Optional[str] = None
    color: Optional[str] = None
    purchase_date: Optional[date] = None

class ShoeResponse(ShoeBase):
    id: int
    user_id: int
    cumulative_km: float
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True
```

**app/schemas/running_record.py**
```python
from pydantic import BaseModel
from datetime import datetime, date
from typing import Optional

class RunningRecordBase(BaseModel):
    distance_km: float
    duration_minutes: int
    date: date
    start_time: datetime
    end_time: datetime
    temperature_c: Optional[float] = None
    humidity_percent: Optional[int] = None
    feeling: Optional[str] = None

class RunningRecordCreate(RunningRecordBase):
    shoe_id: int

class RunningRecordResponse(RunningRecordBase):
    id: int
    user_id: int
    shoe_id: int
    avg_pace_min_per_km: Optional[float]
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True
```

### 통합 테스트 작성 (3-4주차)

**backend/tests/test_integration.py**
```python
import pytest
from app.main import app
from app.database import SessionLocal

# 20개의 통합 테스트 예정
# - 회원가입 → 로그인 → 신발 추가 → 러닝 기록 저장 → 통계 조회
# - 에러 처리 (중복 이메일, 존재하지 않는 신발 등)
# - 권한 검증 (타인의 데이터 조회 불가)
# - 데이터 정합성 검증
```

### 일정

| 주차 | 작업 | 완료도 |
|------|------|--------|
| 1주 | DB 설계 (ERD), 요구사항 정의 | 100% |
| 2주 | ORM 모델 작성, Pydantic 스키마 | 100% |
| 3주 | 통합 테스트 작성 시작 | 50% |
| 4주 | 통합 테스트 완성 및 검증 | 100% |
| 5주 | 통합 시스템 최종 점검 | 100% |
| 6주 | 발표 자료 준비 | 100% |
| 7주 | 최종 발표 | 100% |

---

## 🔵 Backend 1 (사용자 & 신발 관리)

### 담당 역할

- 사용자 인증 시스템 구축 (회원가입/로그인)
- 신발 관리 CRUD API 구현
- JWT 토큰 발급 및 검증
- 비밀번호 해싱 (bcrypt)

### 구현해야 할 파일

```
backend/app/api/routes/
  ├── users.py            # 사용자 인증 API
  └── shoes.py            # 신발 관리 API

backend/tests/
  ├── test_users.py       # 사용자 API 테스트 (6개)
  └── test_shoes.py       # 신발 API 테스트 (4개)
```

### 구현할 API 엔드포인트 (9개)

```
사용자 인증:
  POST   /api/users/signup              - 회원가입
  POST   /api/users/login               - 로그인
  GET    /api/users/me                  - 프로필 조회 (인증필요)
  PUT    /api/users/update              - 프로필 수정 (인증필요)

신발 관리:
  POST   /api/shoes                     - 신발 추가 (인증필요)
  GET    /api/shoes                     - 신발 목록 조회 (인증필요)
  GET    /api/shoes/{shoe_id}           - 신발 상세 조회
  PUT    /api/shoes/{shoe_id}           - 신발 수정 (인증필요)
  DELETE /api/shoes/{shoe_id}           - 신발 삭제 (인증필요)
```

### 구현 예시 (users.py)

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.models import User
from app.schemas import UserCreate, UserResponse
from app.database import get_db
from app.dependencies import get_current_user
from passlib.context import CryptContext
from jose import JWTError, jwt
from datetime import datetime, timedelta

router = APIRouter(prefix="/api/users", tags=["users"])

# 비밀번호 해싱
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)

def create_access_token(data: dict, expires_delta: timedelta = None):
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(hours=24)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, "SECRET_KEY", algorithm="HS256")
    return encoded_jwt

@router.post("/signup", response_model=UserResponse)
async def signup(user: UserCreate, db: Session = Depends(get_db)):
    # 이메일 중복 검증
    db_user = db.query(User).filter(User.email == user.email).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # 새 사용자 생성
    hashed_password = hash_password(user.password)
    db_user = User(email=user.email, password=hashed_password, name=user.name)
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

@router.post("/login")
async def login(email: str, password: str, db: Session = Depends(get_db)):
    # 사용자 찾기
    user = db.query(User).filter(User.email == email).first()
    if not user or not verify_password(password, user.password):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    
    # 토큰 발급
    access_token = create_access_token(data={"sub": user.email})
    return {"access_token": access_token, "token_type": "bearer"}

@router.get("/me", response_model=UserResponse)
async def get_me(current_user: User = Depends(get_current_user)):
    return current_user

@router.put("/update", response_model=UserResponse)
async def update_profile(user_data: dict, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    # 사용자 정보 업데이트
    for key, value in user_data.items():
        setattr(current_user, key, value)
    db.commit()
    db.refresh(current_user)
    return current_user
```

### 구현 예시 (shoes.py)

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.models import Shoe, User
from app.schemas import ShoeCreate, ShoeResponse
from app.database import get_db
from app.dependencies import get_current_user

router = APIRouter(prefix="/api/shoes", tags=["shoes"])

@router.post("", response_model=ShoeResponse)
async def add_shoe(shoe: ShoeCreate, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    db_shoe = Shoe(user_id=current_user.id, **shoe.dict())
    db.add(db_shoe)
    db.commit()
    db.refresh(db_shoe)
    return db_shoe

@router.get("", response_model=list[ShoeResponse])
async def get_shoes(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    return db.query(Shoe).filter(Shoe.user_id == current_user.id).all()

@router.get("/{shoe_id}", response_model=ShoeResponse)
async def get_shoe(shoe_id: int, db: Session = Depends(get_db)):
    shoe = db.query(Shoe).filter(Shoe.id == shoe_id).first()
    if not shoe:
        raise HTTPException(status_code=404, detail="Shoe not found")
    return shoe

@router.put("/{shoe_id}", response_model=ShoeResponse)
async def update_shoe(shoe_id: int, shoe_data: dict, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    shoe = db.query(Shoe).filter(Shoe.id == shoe_id).first()
    if not shoe:
        raise HTTPException(status_code=404, detail="Shoe not found")
    if shoe.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized")
    
    for key, value in shoe_data.items():
        setattr(shoe, key, value)
    db.commit()
    db.refresh(shoe)
    return shoe

@router.delete("/{shoe_id}")
async def delete_shoe(shoe_id: int, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    shoe = db.query(Shoe).filter(Shoe.id == shoe_id).first()
    if not shoe:
        raise HTTPException(status_code=404, detail="Shoe not found")
    if shoe.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized")
    
    db.delete(shoe)
    db.commit()
    return {"message": "Shoe deleted"}
```

### 테스트 작성 (6개)

**backend/tests/test_users.py**
```python
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_signup():
    # 회원가입 성공
    pass

def test_signup_duplicate_email():
    # 이메일 중복 실패
    pass

def test_login():
    # 로그인 성공
    pass

def test_login_wrong_password():
    # 잘못된 비밀번호
    pass

def test_get_profile():
    # 프로필 조회
    pass

def test_update_profile():
    # 프로필 수정
    pass
```

**backend/tests/test_shoes.py**
```python
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_add_shoe():
    # 신발 추가
    pass

def test_get_shoes():
    # 신발 목록 조회
    pass

def test_get_shoe_detail():
    # 신발 상세 조회
    pass

def test_update_shoe():
    # 신발 수정
    pass

def test_delete_shoe():
    # 신발 삭제
    pass

def test_unauthorized_delete():
    # 타인 신발 삭제 불가
    pass
```

### 사용할 Dependencies

```python
# PM이 제공할 것
from app.models import User, Shoe
from app.schemas import UserCreate, UserResponse, ShoeCreate, ShoeResponse

# 인증
from app.dependencies import get_current_user

# 데이터베이스
from app.database import get_db, SessionLocal
```

### 일정

| 주차 | 작업 | 완료도 |
|------|------|--------|
| 1주 | API 설계, 요구사항 정리 | 100% |
| 2주 | 회원가입/로그인 API 구현 | 100% |
| 3주 | 신발 관리 API 구현 | 100% |
| 4주 | 테스트 작성 및 최적화 | 100% |
| 5주 | 버그 수정 및 보안 점검 | 100% |
| 6주 | 최종 최적화 | 100% |
| 7주 | 최종 발표 | 100% |

---

## 🟣 Backend 2 (러닝기록 & 기상정보 & AI)

### 담당 역할

- 러닝 기록 관리 API 구현
- 기상 정보 API 통합 (OpenWeather)
- Google Gemini를 이용한 AI 신발 추천 엔진 구현

### 구현해야 할 파일

```
backend/app/api/routes/
  ├── running.py          # 러닝 기록 API
  └── weather.py          # 기상 정보 API

backend/app/api/ai/
  └── recommendations.py  # AI 신발 추천

backend/tests/
  ├── test_running.py             # 러닝 기록 테스트 (7개)
  ├── test_weather.py             # 기상 정보 테스트 (3개)
  └── test_ai_recommendations.py  # AI 추천 테스트 (5개)
```

### 구현할 API 엔드포인트 (11개)

```
러닝 기록:
  POST   /api/running                       - 러닝 기록 저장 (인증필요)
  GET    /api/running                       - 러닝 기록 조회 (인증필요)
  GET    /api/running/{record_id}           - 러닝 기록 상세 조회
  PUT    /api/running/{record_id}           - 러닝 기록 수정 (인증필요)
  DELETE /api/running/{record_id}           - 러닝 기록 삭제 (인증필요)
  GET    /api/running/statistics/summary    - 전체 통계
  GET    /api/running/statistics/weekly     - 주간 통계
  GET    /api/running/statistics/monthly    - 월간 통계

기상 정보:
  GET    /api/weather/advice                - 현재 위치 기반 기상 조언
  GET    /api/weather/history               - 기상 이력 조회

AI 추천:
  POST   /api/recommendations/shoes         - AI 신발 추천 (인증필요)
```

### 구현 예시 (running.py)

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from datetime import datetime, timedelta
from app.models import RunningRecord, Shoe, User
from app.schemas import RunningRecordCreate, RunningRecordResponse
from app.database import get_db
from app.dependencies import get_current_user

router = APIRouter(prefix="/api/running", tags=["running"])

@router.post("", response_model=RunningRecordResponse)
async def create_running_record(
    record: RunningRecordCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    # 신발 존재 여부 확인
    shoe = db.query(Shoe).filter(Shoe.id == record.shoe_id, Shoe.user_id == current_user.id).first()
    if not shoe:
        raise HTTPException(status_code=404, detail="Shoe not found")
    
    # 러닝 기록 생성
    avg_pace = record.duration_minutes / record.distance_km if record.distance_km > 0 else 0
    db_record = RunningRecord(
        user_id=current_user.id,
        **record.dict(),
        avg_pace_min_per_km=avg_pace
    )
    
    # 신발 누적 거리 업데이트
    shoe.cumulative_km += record.distance_km
    
    db.add(db_record)
    db.commit()
    db.refresh(db_record)
    return db_record

@router.get("", response_model=list[RunningRecordResponse])
async def get_running_records(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    return db.query(RunningRecord).filter(RunningRecord.user_id == current_user.id).all()

@router.get("/statistics/summary")
async def get_summary_statistics(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    records = db.query(RunningRecord).filter(RunningRecord.user_id == current_user.id).all()
    
    if not records:
        return {"total_km": 0, "total_minutes": 0, "avg_pace": 0}
    
    total_km = sum(r.distance_km for r in records)
    total_minutes = sum(r.duration_minutes for r in records)
    avg_pace = total_minutes / total_km if total_km > 0 else 0
    
    return {
        "total_km": total_km,
        "total_minutes": total_minutes,
        "avg_pace": avg_pace,
        "record_count": len(records)
    }

@router.get("/{record_id}", response_model=RunningRecordResponse)
async def get_running_record(record_id: int, db: Session = Depends(get_db)):
    record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not record:
        raise HTTPException(status_code=404, detail="Record not found")
    return record

@router.put("/{record_id}", response_model=RunningRecordResponse)
async def update_running_record(
    record_id: int,
    record_data: dict,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not record:
        raise HTTPException(status_code=404, detail="Record not found")
    if record.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized")
    
    for key, value in record_data.items():
        setattr(record, key, value)
    db.commit()
    db.refresh(record)
    return record

@router.delete("/{record_id}")
async def delete_running_record(
    record_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not record:
        raise HTTPException(status_code=404, detail="Record not found")
    if record.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized")
    
    db.delete(record)
    db.commit()
    return {"message": "Record deleted"}
```

### 구현 예시 (weather.py)

```python
from fastapi import APIRouter, HTTPException
import requests
import os

router = APIRouter(prefix="/api/weather", tags=["weather"])

OPENWEATHER_API_KEY = os.getenv("OPENWEATHER_API_KEY")

@router.get("/advice")
async def get_weather_advice(latitude: float, longitude: float):
    """
    위도, 경도를 받아 현재 기상 정보 반환
    기상 조언 점수 계산 (0-100점)
    """
    
    # OpenWeather API 호출
    url = f"https://api.openweathermap.org/data/2.5/weather?lat={latitude}&lon={longitude}&appid={OPENWEATHER_API_KEY}&units=metric"
    
    try:
        response = requests.get(url)
        data = response.json()
        
        temp = data['main']['temp']
        humidity = data['main']['humidity']
        wind_speed = data['wind']['speed']
        weather = data['weather'][0]['main']
        
        # 체감 온도 계산 (簡易版)
        feels_like = data['main']['feels_like']
        
        # 러닝 추천 점수 계산
        score = 100
        
        # 온도 (최적: 15-20°C)
        if temp < 5 or temp > 30:
            score -= 30
        elif temp < 10 or temp > 25:
            score -= 15
        
        # 습도 (최적: 50-70%)
        if humidity < 30 or humidity > 80:
            score -= 20
        
        # 풍속 (약한 바람 선호)
        if wind_speed > 5:
            score -= 15
        
        # 날씨
        if weather in ["Rain", "Snow", "Thunderstorm"]:
            score -= 30
        elif weather == "Clouds":
            score -= 5
        
        score = max(0, score)
        
        return {
            "temperature": temp,
            "feels_like": feels_like,
            "humidity": humidity,
            "wind_speed": wind_speed,
            "weather": weather,
            "advice_score": score,
            "advice": "러닝하기 좋은 날씨입니다" if score > 70 else "보통 날씨입니다" if score > 40 else "러닝이 어려운 날씨입니다"
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

### 구현 예시 (recommendations.py)

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.models import User, RunningRecord, Shoe
from app.database import get_db
from app.dependencies import get_current_user
import google.generativeai as genai
import os

router = APIRouter(prefix="/api/recommendations", tags=["recommendations"])

genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

@router.post("/shoes")
async def get_shoe_recommendations(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    사용자의 러닝 패턴을 분석하여 Google Gemini API로 신발 추천
    """
    
    # 사용자의 러닝 기록 조회
    records = db.query(RunningRecord).filter(RunningRecord.user_id == current_user.id).all()
    
    if not records:
        raise HTTPException(status_code=400, detail="No running records found")
    
    # 러닝 패턴 분석
    total_km = sum(r.distance_km for r in records)
    avg_distance = total_km / len(records)
    avg_pace = sum(r.avg_pace_min_per_km for r in records if r.avg_pace_min_per_km) / len([r for r in records if r.avg_pace_min_per_km])
    
    # Gemini API 호출을 위한 프롬프트 작성
    prompt = f"""
    러너의 데이터를 분석해서 최적의 신발을 추천해주세요.
    
    사용자 정보:
    - 체중: {current_user.weight_kg}kg
    - 키: {current_user.height_cm}cm
    
    러닝 패턴:
    - 총 거리: {total_km}km
    - 평균 거리: {avg_distance:.1f}km
    - 평균 페이스: {avg_pace:.1f}분/km
    - 러닝 횟수: {len(records)}회
    
    다음 신발 중에서 3개를 추천해주세요:
    1. Nike Air Zoom Pegasus
    2. Adidas Ultraboost
    3. ASICS Gel-Kayano
    4. New Balance 990v5
    5. Brooks Ghost
    
    각 신발에 대해 추천 이유를 짧게 설명해주세요.
    """
    
    # Gemini로 추천받기
    model = genai.GenerativeModel('gemini-pro')
    response = model.generate_content(prompt)
    
    return {
        "recommendation": response.text,
        "analysis": {
            "total_km": total_km,
            "avg_distance": avg_distance,
            "avg_pace": avg_pace,
            "record_count": len(records)
        }
    }
```

### 테스트 작성 (15개)

**backend/tests/test_running.py** (7개)
- 러닝 기록 저장
- 러닝 기록 조회
- 러닝 기록 상세 조회
- 러닝 기록 수정
- 러닝 기록 삭제
- 타인 기록 수정 불가능
- 통계 계산 정확성

**backend/tests/test_weather.py** (3개)
- 기상 정보 조회
- 체감 온도 계산
- 러닝 추천 점수 계산

**backend/tests/test_ai_recommendations.py** (5개)
- AI 추천 요청
- 추천 결과 형식
- 추천 신발 수 확인
- 사용자 패턴 분석
- 기록이 없을 때 에러

### 사용할 Dependencies

```python
# PM이 제공할 것
from app.models import User, Shoe, RunningRecord
from app.schemas import RunningRecordCreate, RunningRecordResponse

# 인증
from app.dependencies import get_current_user

# 데이터베이스
from app.database import get_db

# Google Gemini API
from google.generativeai import GenerativeModel

# 외부 API
import requests  # OpenWeather API
```

### 일정

| 주차 | 작업 | 완료도 |
|------|------|--------|
| 1주 | API 설계, Google Gemini/OpenWeather 문서 학습 | 100% |
| 2주 | 러닝 기록 API 구현 | 100% |
| 3주 | 기상 정보, AI 추천 엔진 구현 | 100% |
| 4주 | 통계 로직, AI 프롬프트 최적화 | 100% |
| 5주 | 버그 수정 및 API 성능 개선 | 100% |
| 6주 | 최종 테스트 | 100% |
| 7주 | 최종 발표 | 100% |

---

## 🟢 Frontend (Kotlin/Android)

### 담당 역할

- Android 모바일 앱 화면 설계 및 구현
- Backend API 연동 (Retrofit)
- 사용자 인터페이스 및 사용성 개선

### 구현해야 할 화면 (6개)

#### 1. 로그인/회원가입 화면

**Features:**
- 로그인: 이메일 & 비밀번호 입력
- 회원가입: 이메일, 비밀번호, 이름 입력
- 토큰 저장 (SharedPreferences)
- 입력값 유효성 검증 (이메일 형식, 비밀번호 길이)
- 에러 메시지 표시
- 로그인 상태 유지 (토큰 재사용)

**Files:**
```
ui/auth/
  ├── LoginScreen.kt
  ├── SignupScreen.kt
  ├── AuthViewModel.kt
  └── AuthRepository.kt
```

#### 2. 홈 화면 (대시보시보드)

**Features:**
- 사용자 프로필 정보 표시 (이름, 프로필 사진)
- 최근 러닝 기록 3개 요약
- 이번 주 러닝 거리/시간
- "새로운 러닝 기록" 버튼
- "신발 관리" 버튼
- "AI 추천받기" 버튼

**Files:**
```
ui/home/
  ├── HomeScreen.kt
  ├── HomeViewModel.kt
  └── HomeRepository.kt
```

#### 3. 러닝 기록 리스트 화면

**Features:**
- 모든 러닝 기록 목록 (최신순)
- 각 기록: 날짜, 거리, 시간, 사용 신발
- 기록 클릭 → 상세화면
- 스크롤 (많은 기록 지원)
- 필터링 (날짜 범위, 신발 종류)

**Files:**
```
ui/running/
  ├── RunningListScreen.kt
  ├── RunningViewModel.kt
  └── RunningRepository.kt
```

#### 4. 러닝 기록 상세 & 추가 화면

**Features:**
- 거리 입력
- 시간 입력
- 사용한 신발 선택 (드롭다운)
- 기상 정보 자동 조회 (위치 기반)
- 기록 저장 버튼
- 기록 수정 기능
- 기록 삭제 기능

**Files:**
```
ui/running/
  ├── RunningDetailScreen.kt
  ├── RunningEditScreen.kt
  └── RunningViewModel.kt
```

#### 5. 신발 관리 화면

**Features:**
- 사용자의 모든 신발 목록
- 각 신발: 브랜드, 모델, 누적 거리
- 신발 추가 버튼
- 신발 클릭 → 상세/수정 화면
- 신발 삭제 기능
- 신발별 러닝 통계 (총 거리, 회수)

**Files:**
```
ui/shoes/
  ├── ShoeListScreen.kt
  ├── ShoeDetailScreen.kt
  ├── ShoeAddScreen.kt
  ├── ShoeViewModel.kt
  └── ShoeRepository.kt
```

#### 6. AI 추천 결과 화면

**Features:**
- "AI 추천받기" 버튼
- 로딩 표시 (AI 처리 중)
- 추천 신발 목록 (상위 3-5개)
- 각 신발: 이미지, 브랜드, 모델, 추천 이유
- 추천 신발 구매 링크 (선택)
- 추천 다시받기 버튼

**Files:**
```
ui/recommendations/
  ├── RecommendationScreen.kt
  ├── RecommendationViewModel.kt
  └── RecommendationRepository.kt
```

### 구현해야 할 Data 계층

**API 클라이언트:**
```
data/api/
  ├── RetrofitClient.kt          # HTTP 클라이언트 설정
  ├── ApiServices.kt             # API 인터페이스
  ├── AuthService.kt             # 인증 API
  ├── RunningService.kt           # 러닝 기록 API
  ├── ShoeService.kt              # 신발 관리 API
  └── RecommendationService.kt    # AI 추천 API
```

**데이터 클래스:**
```
data/models/
  ├── User.kt
  ├── Shoe.kt
  ├── RunningRecord.kt
  └── Recommendation.kt
```

**Repository 패턴:**
```
data/repository/
  ├── AuthRepository.kt
  ├── RunningRepository.kt
  ├── ShoeRepository.kt
  └── RecommendationRepository.kt
```

### 구현 예시 (Kotlin)

**RetrofitClient.kt**
```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.Interceptor

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/api/"
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = PreferencesManager.getToken()
            val request = chain.request().newBuilder()
                .apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()
            chain.proceed(request)
        }
        .build()
    
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()
}
```

**AuthViewModel.kt**
```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    
    private val _loginResult = MutableLiveData<LoginResponse>()
    val loginResult: LiveData<LoginResponse> = _loginResult
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.login(email, password)
                _loginResult.postValue(response)
            } catch (e: Exception) {
                _error.postValue(e.message)
            }
        }
    }
    
    fun signup(email: String, password: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.signup(email, password, name)
                _loginResult.postValue(response)
            } catch (e: Exception) {
                _error.postValue(e.message)
            }
        }
    }
}
```

**RunningRepository.kt**
```kotlin
import retrofit2.HttpException

class RunningRepository {
    private val apiService = RetrofitClient.retrofit.create(RunningService::class.java)
    
    suspend fun getRunningRecords(): List<RunningRecord> {
        return apiService.getRunningRecords()
    }
    
    suspend fun createRunningRecord(record: RunningRecordCreate): RunningRecord {
        return apiService.createRunningRecord(record)
    }
    
    suspend fun getStatistics(): StatisticsResponse {
        return apiService.getStatistics()
    }
    
    suspend fun deleteRunningRecord(recordId: Int) {
        return apiService.deleteRunningRecord(recordId)
    }
}
```

**ShoeListScreen.kt**
```kotlin
@Composable
fun ShoeListScreen(viewModel: ShoeViewModel = hiltViewModel()) {
    val shoes by viewModel.shoes.collectAsState(emptyList())
    val isLoading by viewModel.isLoading.collectAsState(false)
    
    LaunchedEffect(Unit) {
        viewModel.loadShoes()
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* 신발 추가 */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Shoe")
            }
        }
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(shoes) { shoe ->
                    ShoeListItem(shoe)
                }
            }
        }
    }
}
```

### 일정

| 주차 | 작업 | 완료도 |
|------|------|--------|
| 1주 | 화면 와이어프레임 설계 | 100% |
| 2주 | 로그인/회원가입 & 홈 화면 구현 | 100% |
| 3주 | 러닝 기록, 신발 관리 화면 | 100% |
| 4주 | AI 추천 화면, 네비게이션 | 100% |
| 5주 | Backend API 연동, 버그 수정 | 100% |
| 6주 | UI 개선, 성능 최적화 | 100% |
| 7주 | 최종 발표 | 100% |

---

## 🟠 DevOps & QA (배포 & 테스트)

### 담당 역할

- Docker 컨테이너화
- CI/CD 파이프라인 구축
- 자동 테스트 실행
- 배포 자동화
- 성능 & 모니터링

### 구현해야 할 파일

```
RunTrack/
├── Dockerfile                 # Docker 이미지 정의
├── docker-compose.yml         # 멀티 컨테이너 오케스트레이션
├── .dockerignore              # Docker 무시 파일
│
└── .github/workflows/
    ├── ci.yml                 # 지속적 통합 (테스트)
    ├── deploy.yml             # 지속적 배포 (배포)
    └── lint.yml               # 코드 검사
```

### 1. Dockerfile

```dockerfile
# Backend Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 시스템 의존성 설치
RUN apt-get update && apt-get install -y \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성
COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 코드
COPY backend/app ./app

# 헬스 체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import requests; requests.get('http://localhost:8000/docs')"

# 포트 노출
EXPOSE 8000

# 실행
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 2. docker-compose.yml

```yaml
version: '3.8'

services:
  backend:
    build: ./backend
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql://user:password@postgres:5432/runtrack
      - REDIS_URL=redis://redis:6379
      - GEMINI_API_KEY=${GEMINI_API_KEY}
      - OPENWEATHER_API_KEY=${OPENWEATHER_API_KEY}
    depends_on:
      - postgres
      - redis
    networks:
      - runtrack_network
    volumes:
      - ./backend:/app

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=password
      - POSTGRES_DB=runtrack
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - runtrack_network

  redis:
    image: redis:7
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - runtrack_network

volumes:
  postgres_data:
  redis_data:

networks:
  runtrack_network:
    driver: bridge
```

### 3. GitHub Actions CI (ci.yml)

```yaml
name: CI (Continuous Integration)

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.11'
    
    - name: Install dependencies
      run: |
        cd backend
        python -m pip install --upgrade pip
        pip install -r requirements.txt
    
    - name: Lint with flake8
      run: |
        cd backend
        pip install flake8
        flake8 app --count --select=E9,F63,F7,F82 --show-source --statistics
    
    - name: Run tests
      run: |
        cd backend
        pytest --cov=app tests/
    
    - name: Upload coverage
      uses: codecov/codecov-action@v3
      with:
        files: ./backend/coverage.xml
```

### 4. GitHub Actions CD (deploy.yml)

```yaml
name: CD (Continuous Deployment)

on:
  push:
    branches: [ main ]
  release:
    types: [published]

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    
    - name: Build Docker image
      run: |
        docker build -t runtrack-backend:${{ github.sha }} ./backend
    
    - name: Push to Docker Hub
      run: |
        echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
        docker tag runtrack-backend:${{ github.sha }} ${{ secrets.DOCKER_USERNAME }}/runtrack-backend:latest
        docker push ${{ secrets.DOCKER_USERNAME }}/runtrack-backend:latest
    
    - name: Deploy to server
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.DEPLOY_HOST }}
        username: ${{ secrets.DEPLOY_USER }}
        key: ${{ secrets.DEPLOY_KEY }}
        script: |
          cd ~/runtrack
          docker pull ${{ secrets.DOCKER_USERNAME }}/runtrack-backend:latest
          docker-compose down
          docker-compose up -d
          
          # Health check
          sleep 5
          curl http://localhost:8000/docs || exit 1
    
    - name: Notify Slack
      if: always()
      run: |
        curl -X POST ${{ secrets.SLACK_WEBHOOK }} \
          -H 'Content-Type: application/json' \
          -d '{"text":"배포 완료: ${{ job.status }}"}'
```

### 5. GitHub Actions Lint (lint.yml)

```yaml
name: Lint

on: [push, pull_request]

jobs:
  lint:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.11'
    
    - name: Install dependencies
      run: |
        pip install black flake8 isort mypy
    
    - name: Run Black
      run: |
        cd backend
        black --check app/
    
    - name: Run isort
      run: |
        cd backend
        isort --check-only app/
    
    - name: Run Flake8
      run: |
        cd backend
        flake8 app/ --max-line-length=100
    
    - name: Run MyPy
      run: |
        cd backend
        mypy app/ --ignore-missing-imports
```

### 자동화 체크리스트

```yaml
✅ 코드 스타일 검사 (Black, Flake8)
✅ Import 정렬 (isort)
✅ 타입 체크 (MyPy)
✅ 단위 테스트 실행 (pytest)
✅ 통합 테스트 실행
✅ 테스트 커버리지 리포트 (Codecov)
✅ Docker 이미지 빌드
✅ 이미지 보안 스캔 (Trivy)
✅ 서버 배포
✅ 헬스 체크 (배포 성공 확인)
✅ Slack 알림
```

### 성능 테스트

**tests/performance/load_test.py**
```python
from locust import HttpUser, task, between

class LoadTest(HttpUser):
    wait_time = between(1, 3)
    
    @task
    def get_shoes(self):
        self.client.get("/api/shoes")
    
    @task
    def get_running_records(self):
        self.client.get("/api/running")
    
    @task
    def get_statistics(self):
        self.client.get("/api/running/statistics/summary")
```

실행:
```bash
locust -f tests/performance/load_test.py --host=http://localhost:8000
```

### 모니터링 설정

**구조화된 로깅:**
```python
# app/logging.py
import logging
import json
from datetime import datetime

class JSONFormatter(logging.Formatter):
    def format(self, record):
        log_data = {
            'timestamp': datetime.utcnow().isoformat(),
            'level': record.levelname,
            'message': record.getMessage(),
            'module': record.module,
        }
        return json.dumps(log_data)
```

### 일정

| 주차 | 작업 | 완료도 |
|------|------|--------|
| 1주 | Dockerfile, docker-compose.yml 작성 | 100% |
| 2주 | GitHub Actions CI/CD 파이프라인 구축 | 100% |
| 3주 | 자동 테스트 실행 설정 | 100% |
| 4주 | 배포 파이프라인 테스트 | 100% |
| 5주 | 성능 테스트, 모니터링 설정 | 100% |
| 6주 | 프로덕션 배포 준비 | 100% |
| 7주 | 최종 배포 | 100% |

---

## 📅 개발 일정

### 전체 일정 (7주)

| 주차 | PM | Backend 1 | Backend 2 | Frontend | DevOps & QA |
|------|-----|------|------|------|------|
| **1주** | DB 설계, ERD | API 설계 | API 설계 | 화면 설계 | 환경 구축 |
| **2주** | ORM/Pydantic | 회원가입/로그인 | 러닝 API | 로그인/홈 | Docker Compose, CI/CD |
| **3주** | 통합 테스트 | 신발 API | 기상/AI API | 기록/신발 | 테스트 자동화 |
| **4주** | 통합 검증 | API 완성 | AI 추천 완성 | 추천 화면 | 배포 파이프라인 |
| **5주** | 시스템 점검 | 버그 수정 | 버그 수정 | UI 수정 | 최종 테스트 |
| **6주** | 발표 준비 | 최적화 | 최적화 | 최적화 | 배포 |
| **7주** | 최종 발표 | 최종 발표 | 최종 발표 | 최종 발표 | 배포 검증 |

### 주간 회의 (매주 월요일 2시)

- PM: 진행 상황 보고
- Backend: 구현 현황, 이슈 공유
- Frontend: 화면 진행도 공유
- DevOps: 인프라 상태 점검
- 다음 주 목표 설정

---

## ✅ 최종 체크리스트

```
프로젝트 시작 전:
□ 모든 팀원이 이 문서를 읽음
□ GitHub 리포지터리 접근 권한 확인
□ 개인 개발 환경 설정 완료
□ 슬랙/카톡 채널 생성
□ 첫 주 회의 일정 확인

1주 완료:
□ PM: ERD 설계 완료
□ Backend1, 2: API 명세 정의
□ Frontend: 와이어프레임 완성
□ DevOps: Docker 환경 준비
□ 주간 회의 진행

최종 발표 준비:
□ 모든 기능 테스트 완료
□ 배포 성공 확인
□ 발표 자료 준비
□ 데모 시나리오 준비
```

---

**Happy Coding! 🚀**

