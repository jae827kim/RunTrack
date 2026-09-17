# RunTrack 백엔드 가이드

## 📋 개요

RunTrack 백엔드는 **FastAPI** 기반의 비동기 REST API 서버입니다.

- **언어**: Python 3.9+
- **프레임워크**: FastAPI + Uvicorn
- **데이터베이스**: PostgreSQL + SQLAlchemy ORM
- **캐싱**: Redis
- **AI 서비스**: Google Gemini API

---

## 🚀 빠른 시작

### 1. 사전 설치

```bash
# Python 가상 환경 생성
python -m venv venv

# 활성화
# Windows:
venv\Scripts\activate
# Mac/Linux:
source venv/bin/activate

# 패키지 설치
pip install -r requirements.txt
```

### 2. 환경 설정

```bash
# .env 파일 생성
cp .env.example .env

# 편집 (필수 API 키 추가):
# - GEMINI_API_KEY: Google Gemini API 키
# - GOOGLE_MAPS_API_KEY: Google Maps API 키
# - OPENWEATHER_API_KEY: OpenWeather API 키
# - DATABASE_URL: PostgreSQL 연결 문자열
```

### 3. 데이터베이스 초기화

```bash
# PostgreSQL 데이터베이스 생성
createdb runtrack

# 테이블 생성
python -c "from app.database import Base, engine; Base.metadata.create_all(bind=engine)"
```

### 4. 서버 실행

```bash
# 개발 모드
uvicorn app.main:app --reload

# 프로덕션 모드
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

**API 문서**: http://localhost:8000/docs

---

## 👥 Backend 역할 분담

### Backend 1: 사용자 & 신발 관리 (User/Shoe API)

**담당 파일:**
- `app/api/routes/users.py` - 회원가입, 로그인, 프로필
- `app/api/routes/shoes.py` - 신발 추가, 수정, 삭제

**구현 API:**
```
POST   /users/signup              - 회원가입
POST   /users/login               - 로그인
GET    /users/me                  - 프로필 조회
PUT    /users/update              - 프로필 수정
POST   /shoes/add                 - 신발 추가
GET    /shoes/list                - 신발 목록
GET    /shoes/{id}                - 신발 상세
PUT    /shoes/{id}                - 신발 수정
DELETE /shoes/{id}                - 신발 삭제
```

**코드량:** ~300줄  
**테스트:** 10개

---

### Backend 2: 러닝기록 & AI & 날씨 (Running/Weather/AI API)

**담당 파일:**
- `app/api/routes/running.py` - 러닝 기록 저장/조회
- `app/api/routes/weather.py` - 날씨 정보
- `app/api/ai/recommendations.py` - Google Gemini AI 추천

**구현 API:**
```
POST   /running/create            - 러닝 기록 저장
GET    /running/list              - 러닝 기록 조회
GET    /running/stats             - 러닝 통계
PUT    /running/{id}              - 기록 수정
DELETE /running/{id}              - 기록 삭제
GET    /weather/info              - 기상 정보
POST   /recommendations/shoes     - AI 신발 추천
```

**코드량:** ~400줄  
**테스트:** 10개

---

## 📌 PM이 먼저 작성할 ORM & Pydantic

Backend 개발자들이 API를 구현하기 전에, PM이 다음 파일들을 먼저 작성합니다:

**ORM 모델** (`app/models/`):
- `user.py` - User 모델 정의
- `shoe.py` - Shoe 모델 정의
- `running_record.py` - RunningRecord 모델 정의

**Pydantic 스키마** (`app/schemas/`):
- `user.py` - UserCreate, UserResponse
- `shoe.py` - ShoeCreate, ShoeResponse
- `running_record.py` - RunningRecordCreate, RunningRecordResponse

이 파일들이 준비되면, Backend 개발자들은 이를 import하여 API를 구현합니다.

---

## 📁 프로젝트 구조

```
backend/
├── app/
│   ├── main.py              # FastAPI 진입점
│   ├── config.py            # 설정
│   ├── database.py          # DB 연결
│   ├── api/
│   │   ├── routes/          # API 엔드포인트
│   │   │   ├── users.py
│   │   │   ├── shoes.py
│   │   │   ├── running.py
│   │   │   └── weather.py
│   │   └── ai/
│   │       └── recommendations.py
│   ├── models/              # SQLAlchemy 모델
│   │   ├── user.py
│   │   ├── shoe.py
│   │   └── running_record.py
│   └── schemas/             # Pydantic 스키마
│       ├── user.py
│       ├── shoe.py
│       └── running_record.py
├── tests/                   # 테스트
├── requirements.txt         # 의존성
├── .env.example            # 환경변수 예시
└── pytest.ini              # pytest 설정
```

---

## 🔌 API 엔드포인트

### 사용자 관련

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST   | `/api/users/register` | 회원가입 |
| POST   | `/api/users/login` | 로그인 |
| GET    | `/api/users/me` | 프로필 조회 |
| PUT    | `/api/users/me` | 프로필 수정 |

### 신발 관리

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST   | `/api/shoes` | 신발 등록 |
| GET    | `/api/shoes` | 신발 목록 조회 |
| GET    | `/api/shoes/{id}` | 신발 상세 조회 |
| PUT    | `/api/shoes/{id}` | 신발 수정 |
| DELETE | `/api/shoes/{id}` | 신발 삭제 |
| GET    | `/api/shoes/{id}/stats` | 신발 통계 |

### 러닝 기록

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST   | `/api/running/start` | 러닝 시작 |
| POST   | `/api/running/{id}/end` | 러닝 종료 |
| GET    | `/api/running` | 러닝 기록 조회 |
| GET    | `/api/running/{id}` | 기록 상세 조회 |
| GET    | `/api/running/statistics/summary` | 통계 요약 |
| GET    | `/api/running/statistics/weekly` | 주간 통계 |
| GET    | `/api/running/statistics/monthly` | 월간 통계 |

### 기상 조언

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET    | `/api/weather/advice` | 러닝 조언 |
| GET    | `/api/weather/history` | 기상 이력 |

### AI 추천

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST   | `/api/recommendations/shoes` | 신발 추천 |

---

## 🧪 테스트

```bash
# 모든 테스트 실행
pytest

# 테스트 커버리지 포함
pytest --cov=app tests/

# 특정 테스트 파일만 실행
pytest tests/test_shoes.py

# 상세 출력
pytest -v

# 실시간 출력
pytest -s
```

---

## 🔐 인증

JWT 토큰 기반 인증 사용:

```bash
# 로그인 후 받은 토큰으로 API 호출
curl -H "Authorization: Bearer {token}" http://localhost:8000/api/users/me
```

---

## 📊 데이터 모델

### 사용자 (User)
- 계정 정보 (username, email, password)
- 신체 정보 (체중, 키, 발크기, 아치타입)
- 러닝 선호도 (스타일, 예산, 선호 브랜드)

### 신발 (Shoe)
- 신발 정보 (브랜드, 모델, 사이즈)
- 사용 통계 (누적km, 러닝 횟수)
- 구매 정보 (가격, 날짜)

### 러닝 기록 (RunningRecord)
- 거리, 시간, 페이스
- GPS 경로, 고도
- 신체 데이터 (심박수, 칼로리)
- 기상 조건, 기분

---

## 🤖 AI 신발 추천

Google Gemini API를 활용하여 사용자 정보에 맞는 신발을 추천합니다.

**입력 정보**:
- 체중, 키, 발크기
- 발볼 너비, 아치 타입
- 러닝 스타일, 예산
- 선호 브랜드

**출력**:
- 상위 3개 신발 추천
- 추천 이유, 장단점
- 예상 가격, 내구성

---

## 🌡️ 기상 조언

OpenWeather API를 활용하여:
- 체감 온도 계산
- 러닝 추천 점수 (일정 여유 시 선택 구현)
- 조건별 안내 (최적/좋음/괜찮음/나쁨)

**최적 조건**:
- 온도: 15-20°C
- 습도: 50-70%
- 바람: 약함 (3-5m/s)

---

## 📝 개발 팁

### 로그 출력
```python
import logging
logger = logging.getLogger(__name__)
logger.info("메시지")
```

### 비동기 함수
```python
@router.get("/path")
async def async_endpoint():
    # async/await 사용
    pass
```

### 데이터 검증
```python
from pydantic import BaseModel

class MySchema(BaseModel):
    field: str  # 필수
    optional_field: Optional[int] = None
```

---

## 🐛 트러블슈팅

### PostgreSQL 연결 오류
```bash
# PostgreSQL 서비스 확인
sudo systemctl status postgresql

# 데이터베이스 생성
createdb runtrack
```

### Redis 연결 오류
```bash
# Redis 서버 실행
redis-server
```

### API 키 오류
- `.env` 파일 확인
- 올바른 API 키 입력
- 환경변수 새로고침 (서버 재시작)

---

## 📚 참고 링크

- [FastAPI 문서](https://fastapi.tiangolo.com/)
- [SQLAlchemy 문서](https://docs.sqlalchemy.org/)
- [Pydantic 문서](https://docs.pydantic.dev/)
- [Google Gemini API](https://ai.google.dev/)
- [OpenWeather API](https://openweathermap.org/api)

---

**Happy Coding! 🚀**
