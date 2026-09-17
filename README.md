# 🏃‍♂️ RunTrack: AI 기반 맞춤형 러닝 조언 앱

> **학습데이터 특화 AI를 활용한 맞춤형 러닝 성과 관리 애플리케이션**

## 📌 프로젝트 개요

**RunTrack**는 러너의 개인 러닝 데이터를 분석하고, AI를 통해 다음을 제공합니다:
- 🥾 **맞춤형 신발 추천** (체중, 발크기, 발볼 유형, 아치 분석)
- 🗺️ **GPS & 지도 연동** (구글맵 기반 루트 추적)
- 🌡️ **기상 조건 분석** (체감온도, 러닝 조언, 추천 점수는 선택 구현)
- 📊 **성장 추적** (주간/월간/연간 통계)
- 🛡️ **건강 & 안전** (야간 러닝 알림, 긴급 연락처 공유)

현재 저장소는 백엔드/안드로이드 기본 골격과 API 초안이 구성된 상태이며, 아래 항목은 학기말까지 순차 구현할 목표 기준입니다.

---

## 👥 팀 구성 (5명)

| 역할 | 담당자 | 주요 업무 |
|------|--------|----------|
| **PM** | 김재원 | DB 설계, ORM/Pydantic 스키마, 통합 테스트, 팀 조율 |
| **Backend 1** | 조병현 | 사용자/신발 관리 API (users.py, shoes.py) |
| **Backend 2** | 김경빈 | 러닝/날씨/AI API (running.py, weather.py, recommendations.py) |
| **Frontend** | 이아림 | Kotlin 모바일 앱 UI 구현 및 API 연동 |
| **DevOps & QA** | 김주현 | Docker, CI/CD, 테스트 자동화, 배포 |

**GitHub Repository**: https://github.com/jae827kim/RunTrack

---

## 📅 개발 일정

**기간**: 9월 3주차 ~ 12월 1주차 (약 12주)

| 구간 | 목표 |
|------|------|
| 1-2주 | 요구사항 확정, ERD/기본 구조 정리, 개발 환경 통일 |
| 3-5주 | 핵심 API와 주요 화면 뼈대 구현 |
| 6-8주 | AI 추천, 기상 조언, 화면/API 연동 |
| 9-10주 | 통합 테스트, 버그 수정, 선택 기능 범위 확정 |
| 11-12주 | 발표 자료 준비, 최종 점검, 제출 |

기상 조건 분석은 체감온도와 러닝 조언을 우선 구현하고, 러닝 추천 점수는 일정에 따라 선택 기능으로 둡니다.

---

## 🛠️ 기술 스택

### Frontend (모바일)
```
- Language: Kotlin
- Platform: Android (API 24+)
- UI Framework: Jetpack Compose
- IDE: Android Studio
- HTTP Client: Retrofit + OkHttp
- Architecture: MVVM + Repository Pattern
- DI: Hilt
```

### Backend API
```
- Language: Python
- Framework: FastAPI
- Database: PostgreSQL
- Cache: Redis
- ORM: SQLAlchemy
- Auth: JWT
- Async: uvicorn + AsyncIO
```

### AI Integration
```
- LLM API: Google Gemini (무료)
- Library: google-generativeai
- Features: 신발 추천, 기상 분석
```

---

## 📁 프로젝트 구조

```
RunTrack/
├── PROJECT_SUMMARY.md
├── README.md
├── ROLES.md
├── SETUP.md
├── android/
│   ├── ANDROID_SETUP.md
│   ├── settings.gradle.kts
│   └── app/
│       ├── build.gradle.kts
│       └── src/
│           └── main/
│               ├── AndroidManifest.xml
│               └── kotlin/com/runtrack/
│                   ├── MainActivity.kt
│                   ├── RunTrackApp.kt
│                   ├── data/
│                   │   ├── api/
│                   │   │   ├── ApiServices.kt
│                   │   │   └── RetrofitClient.kt
│                   │   ├── models/
│                   │   │   ├── RunningRecord.kt
│                   │   │   ├── Shoe.kt
│                   │   │   └── User.kt
│                   │   └── repository/
│                   │       └── ShoeRepository.kt
│                   ├── di/
│                   │   └── AppModule.kt
│                   └── ui/theme/
│                       ├── Theme.kt
│                       └── Type.kt
└── backend/
    ├── BACKEND_SETUP.md
    ├── pytest.ini
    ├── requirements.txt
    ├── app/
    │   ├── main.py
    │   ├── config.py
    │   ├── database.py
    │   ├── api/
    │   │   ├── ai/
    │   │   │   └── recommendations.py
    │   │   └── routes/
    │   │       ├── running.py
    │   │       ├── shoes.py
    │   │       ├── users.py
    │   │       └── weather.py
    │   ├── models/
    │   └── schemas/
    └── tests/
        └── __init__.py

```

---

## 🚀 빠른 시작

### 사전 요구사항
- **Backend**: Python 3.9+, PostgreSQL, Redis
- **Frontend**: Android Studio, JDK 17+
- **API Keys**: Google Gemini API (무료), Google Maps API, OpenWeather API

### Backend 설치

```bash
cd backend
python -m venv venv
source venv/bin/activate  # Windows PowerShell: .\venv\Scripts\Activate.ps1
pip install -r requirements.txt

# .env 파일 설정
cp .env.example .env  # Windows PowerShell: Copy-Item .env.example .env
# .env 파일 수정 (API 키 등)

# 현재 기본 스키마 생성
python -c "from app.database import Base, engine; Base.metadata.create_all(bind=engine)"

# 서버 실행
uvicorn app.main:app --reload
```

**API 문서**: http://localhost:8000/docs

### Frontend 설정

```bash
cd android
# Android Studio에서 열기 또는
./gradlew build
./gradlew installDebug  # 에뮬레이터/디바이스에 설치
```

---

## 📚 목표 기능

### 1️⃣ 신발 관리 & 킬로 적립
- 여러 신발 등록 및 관리
- 신발별 누적 러닝 킬로 자동 추적
- 신발 선택 후 러닝 기록

### 2️⃣ AI 기반 맞춤형 신발 추천
- 체중, 발크기, 발볼 유형, 아치 분석
- 주요 브랜드 신발 데이터베이스
- AI가 생성한 개인화된 추천

### 3️⃣ GPS & 지도 연동
- 실시간 GPS 추적 (거리, 속도, 고도)
- 구글맵 기반 루트 기록 및 재실행
- 즐겨찾기 루트 저장
- 지역별 인기 러닝 코스 추천

### 4️⃣ 기상 데이터 기반 체감온도 & 조언
- 실시간 기온, 습도, 풍량, 자외선 데이터
- 체감 온도 계산
- 러닝 추천 점수 (일정 여유 시 선택 구현)
- 최적 조건: 15-20°C, 습도 50-70%, 약한 바람

### 5️⃣ 개인 데이터 분석 & 성장 추적
- 주간/월간/연간 러닝 통계
- 페이스 분석 (최고/최저/평균)
- 계절별 러닝 패턴 분석

### 6️⃣ 건강 & 안전
- 긴급 연락처 위치 공유
- 야간 러닝 안전 알림
- 심박수 범위 모니터링 (선택)

---

## 🧪 테스트

### Backend
```bash
cd backend
pytest
pytest --cov=app tests/  # 커버리지 포함
```

### Frontend
```bash
cd android
./gradlew test
./gradlew connectedAndroidTest  # Instrumented 테스트
```

---

## 📖 API 엔드포인트 초안

### 사용자 관련
- `POST /api/users/register` - 회원가입
- `POST /api/users/login` - 로그인
- `GET /api/users/me` - 내 정보 조회
- `PUT /api/users/me` - 내 정보 수정

### 신발 관련
- `POST /api/shoes` - 신발 등록
- `GET /api/shoes` - 신발 목록 조회
- `GET /api/shoes/{id}` - 신발 상세 조회
- `PUT /api/shoes/{id}` - 신발 정보 수정
- `DELETE /api/shoes/{id}` - 신발 삭제
- `GET /api/shoes/{id}/stats` - 신발 통계 조회

### 신발 추천
- `POST /api/recommendations/shoes` - AI 기반 신발 추천 (라우트 추가 예정)

### 러닝 기록
- `POST /api/running/start` - 러닝 세션 시작
- `POST /api/running/{id}/end` - 러닝 세션 종료 및 저장
- `GET /api/running` - 러닝 기록 조회
- `GET /api/running/{id}` - 러닝 기록 상세 조회
- `GET /api/running/statistics/summary` - 통계 요약
- `GET /api/running/statistics/weekly` - 주간 통계
- `GET /api/running/statistics/monthly` - 월간 통계

### 기상 조언
- `GET /api/weather/advice` - 현재 위치 기반 러닝 조언
- `GET /api/weather/history` - 최근 기상 이력 조회

---

## 🔧 개발 가이드

상세 개발 가이드는 [SETUP.md](./SETUP.md)를 참조하세요.

---

## 📝 라이선스

MIT License

---

## 📞 지원

문제 또는 제안사항은 GitHub Issues를 통해 등록해주세요.

---

**Happy Running! 🏃‍♀️🏃‍♂️**
