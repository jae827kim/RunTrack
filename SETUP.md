# 📖 RunTrack - 완전 설정 가이드

> 🏃‍♂️ **AI 기반 맞춤형 러닝 조언 앱 - 풀 스택 설정 가이드**

**GitHub Repository**: https://github.com/jae827kim/RunTrack

---

## 📅 12주 개발 일정 및 팀 역할

**기간**: 9월 3주차 ~ 12월 1주차 (약 12주)

| 주차 | PM | Backend 1<br/>(조병현) | Backend 2<br/>(김경빈) | Frontend<br/>(이아림) | DevOps & QA<br/>(김주현) |
|------|-----|------|------|------|------|
| **1-2주** | 요구사항 정리, ERD | 인증/API 명세 | 러닝/기상 API 명세 | 화면 흐름, 디자인 초안 | 개발 환경, CI 초안 |
| **3-4주** | ORM, Pydantic, 공통 규칙 | 회원/프로필 API | 러닝 기록 API | 로그인/홈, 네비게이션 | Docker, 테스트 자동화 |
| **5-6주** | 통합 시나리오 정리 | 신발 CRUD, 예외 처리 | 기상 조언, AI 추천 초안 | 기록/신발 화면 | 배포 파이프라인 |
| **7-8주** | 통합 테스트 실행 | 프론트 연동, 버그 수정 | 통계 로직, 프론트 연동 | 추천 화면, API 연동 | 스테이징, 로그 설정 |
| **9-10주** | 범위 조정, 발표 초안 | 보안/응답 형식 정리 | 성능 개선, 선택 기능 판단 | UI 다듬기, 버그 수정 | 회귀 테스트, 성능 점검 |
| **11-12주** | 최종 점검, 발표 자료 | 최종 검수 지원 | 최종 검수 지원 | 시연 흐름 정리 | 최종 배포/검증 |

기상 조건 분석은 체감온도와 러닝 조언을 우선 구현하고, 러닝 추천 점수는 일정에 따라 선택 구현 범위로 둡니다.

---

## 📋 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [백엔드 설정](#백엔드-설정-python-fastapi)
3. [프론트엔드 설정](#프론트엔드-설정-kotlin-android)
4. [API 통합](#api-통합)
5. [데이터베이스 설정](#데이터베이스-설정)
6. [배포](#배포)
7. [트러블슈팅](#트러블슈팅)

---

## 프로젝트 개요

### 구조
```
RunTrack/
├── backend/              # Python FastAPI
├── android/              # Kotlin Android App
├── PROJECT_SUMMARY.md    # 현재 구조 요약
├── ROLES.md              # 역할 및 일정
├── SETUP.md              # 통합 설정 가이드
└── README.md             # 프로젝트 소개
```

### 기술 스택

| 계층 | 기술 |
|------|------|
| **Frontend** | Kotlin, Jetpack Compose, Android 24+ |
| **Backend** | Python, FastAPI, PostgreSQL, Redis |
| **AI** | Google Gemini API |
| **External** | Google Maps, OpenWeather |
| **DevOps** | Docker, Docker Compose, GitHub Actions |

### PM의 초기 작업 (1-2주)

**1단계: ERD 설계**
```
User 테이블 ─── 1:N ─── Shoe 테이블
User 테이블 ─── 1:N ─── RunningRecord 테이블
Shoe 테이블 ─── 1:N ─── RunningRecord 테이블
```

**2단계: ORM 모델 작성 (SQLAlchemy)**
- `app/models/user.py` - User 모델
- `app/models/shoe.py` - Shoe 모델  
- `app/models/running_record.py` - RunningRecord 모델

**3단계: Pydantic 검증 스키마 작성**
- `app/schemas/user.py` - UserCreate, UserResponse
- `app/schemas/shoe.py` - ShoeCreate, ShoeResponse
- `app/schemas/running_record.py` - RunningRecordCreate, RunningRecordResponse

---

## 백엔드 설정 (Python FastAPI)

### 단계 1: Python 환경 설정

```bash
cd backend

# 가상 환경 생성
python -m venv venv

# 활성화
# Windows:
venv\Scripts\activate
# Mac/Linux:
source venv/bin/activate

# 패키지 설치
pip install -r requirements.txt
```

### 단계 2: 데이터베이스 준비

```bash
# PostgreSQL 설치 (없는 경우)
# macOS: brew install postgresql
# Ubuntu: sudo apt-get install postgresql
# Windows: https://www.postgresql.org/download/windows/

# 데이터베이스 생성
createdb runtrack

# 또는 psql로 접속 후:
CREATE DATABASE runtrack;
```

### 단계 3: 환경 변수 설정

```bash
# .env 파일 생성
cp .env.example .env

# 편집 (필수):
# GEMINI_API_KEY=your-key
# DATABASE_URL=postgresql://user:password@localhost:5432/runtrack
# GOOGLE_MAPS_API_KEY=your-key
# OPENWEATHER_API_KEY=your-key
```

### 단계 4: 데이터베이스 테이블 생성

```bash
python -c "from app.database import Base, engine; Base.metadata.create_all(bind=engine)"
```

### 단계 5: 서버 시작

```bash
# 개발 모드 (자동 새로고침)
uvicorn app.main:app --reload

# 프로덕션 모드
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

**확인**: http://localhost:8000/docs (Swagger UI)

---

## 프론트엔드 설정 (Kotlin Android)

### 단계 1: Android Studio 설정

1. **Android Studio** 최신 버전 설치
2. **프로젝트 열기**: `android` 폴더 선택
3. **SDK 관리자**에서 다음 설치:
   - Android SDK 34
   - SDK Build-Tools 34.x.x
   - Android Emulator

### 단계 2: 환경 설정

**build.gradle.kts 수정:**
```kotlin
buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"YOUR_KEY\"")
buildConfigField("String", "GOOGLE_GEMINI_API_KEY", "\"YOUR_KEY\"")
buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
```

**AndroidManifest.xml 수정:**
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY" />
```

### 단계 3: 의존성 설치

```bash
cd android
./gradlew build
```

### 단계 4: 에뮬레이터 생성 및 실행

**Android Studio에서**:
1. AVD Manager 열기
2. "Create Virtual Device" 클릭
3. Pixel 5, Android 12+ 선택
4. "Start" 클릭

### 단계 5: 앱 실행

```bash
# 또는 Android Studio에서 Run 버튼 클릭
./gradlew installDebug
```

---

## API 통합

### 사용자 인증 흐름

```
1. 회원가입: POST /api/users/register
   ├─ username, email, password
   └─ Response: User 객체

2. 로그인: POST /api/users/login
   ├─ username, password
   └─ Response: {access_token, token_type, expires_in}

3. API 호출 시 헤더에 토큰 추가:
   ├─ Authorization: Bearer {access_token}
   └─ Content-Type: application/json
```

### 핵심 엔드포인트

**신발 관리**
```
POST   /api/shoes              - 신발 등록
GET    /api/shoes              - 신발 목록
GET    /api/shoes/{id}         - 신발 상세
PUT    /api/shoes/{id}         - 신발 수정
DELETE /api/shoes/{id}         - 신발 삭제
GET    /api/shoes/{id}/stats   - 신발 통계
```

**러닝 기록**
```
POST   /api/running/start                  - 세션 시작
POST   /api/running/{id}/end               - 세션 종료
GET    /api/running                        - 기록 조회
GET    /api/running/statistics/summary     - 통계 요약
GET    /api/running/statistics/weekly      - 주간 통계
GET    /api/running/statistics/monthly     - 월간 통계
```

**기상 조언**
```
GET    /api/weather/advice     - 러닝 조언 (위도, 경도 필요)
GET    /api/weather/history    - 기상 이력
```

**AI 추천**
```
POST   /api/recommendations/shoes - 신발 추천
```

---

## 데이터베이스 설정

### PostgreSQL 연결

```python
# app/config.py
DATABASE_URL = "postgresql://user:password@localhost:5432/runtrack"
```

### 주요 테이블

**users** - 사용자 계정
- id, username, email, hashed_password
- weight_kg, height_cm, foot_size, foot_width, arch_type
- running_style, budget_won, preferred_brands
- created_at, updated_at

**shoes** - 신발 정보
- id, user_id, brand, model, size, color
- purchase_price_won, purchase_date
- cumulative_km, run_count
- condition, notes
- created_at, updated_at

**running_records** - 러닝 기록
- id, user_id, shoe_id
- title, description
- distance_km, duration_minutes, start_time, end_time
- avg_pace_min_per_km, max_speed_kmh, avg_speed_kmh
- avg_heart_rate, max_heart_rate, calories_burned
- elevation_gain_m, elevation_loss_m, max_altitude_m
- temperature_c, humidity_percent, wind_speed_kmh
- feeling, notes
- created_at, updated_at

---

## 배포

### 백엔드 배포 (Heroku 예시)

```bash
cd backend

# Heroku CLI 설치 후
heroku login
heroku create runtrack-api

# Procfile 생성
echo "web: uvicorn app.main:app --host 0.0.0.0 --port \$PORT" > Procfile

# 환경 변수 설정
heroku config:set GEMINI_API_KEY=your-key
heroku config:set DATABASE_URL=postgresql://...
heroku config:set SECRET_KEY=your-secret

# 배포
git push heroku main
```

### 프론트엔드 배포 (Google Play Store)

1. **서명된 APK 생성**:
   - Android Studio > Build > Build Bundle(s) / APK(s)
   
2. **Play Store 등록**:
   - Google Play Console에서 앱 등록
   - 서명된 APK 업로드
   - 앱 정보 및 스크린샷 작성
   - 검수 신청

---

## 트러블슈팅

### 백엔드 문제

**PostgreSQL 연결 오류**
```bash
# PostgreSQL 서비스 확인
systemctl status postgresql  # Linux
brew services list          # macOS
services.msc               # Windows
```

**GEMINI_API_KEY 오류**
- Google AI Studio에서 API 키 생성: https://makersuite.google.com/app/apikey
- .env 파일에 추가
- 서버 재시작

**포트 충돌**
```bash
# 8000 포트 사용 중인 프로세스 확인
lsof -i :8000  # macOS/Linux
netstat -ano | findstr :8000  # Windows
```

### 프론트엔드 문제

**"Cannot connect to localhost:8000"**
```
에뮬레이터에서:
- localhost 대신 10.0.2.2 사용
- build.gradle.kts: API_BASE_URL="http://10.0.2.2:8000/"
```

**GPS 권한 오류**
- AndroidManifest.xml에서 권한 확인
- 에뮬레이터 설정 > Permissions > Location 활성화

**Gradle 동기화 실패**
```bash
./gradlew clean
./gradlew sync
```

---

## 개발 팁

### 빠른 테스트

```bash
# 백엔드 테스트
cd backend
pytest -v

# 프론트엔드 테스트
cd android
./gradlew test
```

### 로깅

```python
# 백엔드
import logging
logger = logging.getLogger(__name__)
logger.info("메시지")

# 프론트엔드
import timber.log.Timber
Timber.d("메시지")
```

### 성능 모니터링

```bash
# 백엔드 - Prometheus 통합
pip install prometheus-client

# 프론트엔드 - Android Profiler
Android Studio > Profiler
```

---

## 📚 추가 리소스

- [FastAPI 공식 문서](https://fastapi.tiangolo.com/)
- [Jetpack Compose 가이드](https://developer.android.com/jetpack/compose)
- [Google Gemini API](https://ai.google.dev/)
- [PostgreSQL 튜토리얼](https://www.postgresql.org/docs/)

---

## 라이선스

MIT License - 자유롭게 사용, 수정, 배포 가능

---

## 지원

문제나 제안사항:
- GitHub Issues: [생성](https://github.com/your-repo/runtrack/issues)
- 이메일: support@runtrack.com

---

```
최종 수정: 2026년
프로젝트 버전: 0.1.0
```
