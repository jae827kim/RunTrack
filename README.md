# 🏃‍♂️ RunTrack: AI 기반 맞춤형 러닝 조언 앱

> **학습데이터 특화 AI를 활용한 맞춤형 러닝 성과 관리 애플리케이션**

## 📌 프로젝트 개요

**RunTrack**는 러너의 개인 러닝 데이터를 분석하고, AI를 통해 다음을 제공합니다:
- 🥾 **맞춤형 신발 추천** (체중, 발크기, 발볼 유형, 아치 분석)
- 🗺️ **GPS & 지도 연동** (구글맵 기반 루트 추적)
- 🌡️ **기상 조건 분석** (체감온도, 러닝 추천 점수)
- 📊 **성장 추적** (주간/월간/연간 통계)
- 🛡️ **건강 & 안전** (야간 러닝 알림, 긴급 연락처 공유)

---

## � 팀 구성 (5명)

| 역할 | 담당자 | 주요 업무 |
|------|--------|----------|
| **PM** | TBD | DB 설계, ORM/Pydantic 스키마, 통합 테스트, 팀 조율 |
| **Backend 1** | TBD | 사용자/신발 관리 API (users.py, shoes.py) |
| **Backend 2** | TBD | 러닝/날씨/AI API (running.py, weather.py, recommendations.py) |
| **Frontend** | TBD | Kotlin 모바일 앱 (6개 화면, 500줄) |
| **DevOps & QA** | TBD | Docker, CI/CD, 테스트 자동화, 배포 |

**GitHub Repository**: https://github.com/jae827kim/RunTrack

---

## �🛠️ 기술 스택

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
├── backend/                          # Python FastAPI 백엔드
│   ├── app/
│   │   ├── main.py                  # FastAPI 진입점
│   │   ├── api/
│   │   │   ├── routes/
│   │   │   │   ├── shoes.py         # 신발 관련 API
│   │   │   │   ├── running.py       # 러닝 기록 API
│   │   │   │   ├── weather.py       # 기상 조언 API
│   │   │   │   └── users.py         # 사용자 API
│   │   │   └── ai/
│   │   │       └── recommendations.py # AI 신발 추천 서비스
│   │   ├── models/
│   │   │   ├── user.py
│   │   │   ├── shoe.py
│   │   │   └── running_record.py
│   │   ├── schemas/
│   │   │   ├── user.py
│   │   │   ├── shoe.py
│   │   │   └── running_record.py
│   │   ├── database.py              # DB 연결
│   │   ├── config.py                # 환경설정
│   │   └── dependencies.py          # 의존성 주입
│   ├── requirements.txt              # Python 패키지
│   ├── .env.example                  # 환경변수 예시
│   ├── pytest.ini                    # 테스트 설정
│   └── tests/
│       ├── test_shoes.py
│       ├── test_running.py
│       └── test_ai_recommendations.py
│
├── android/                          # Kotlin Android 앱
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── kotlin/com/runtrack/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   │   ├── shoes/
│   │   │   │   │   │   ├── ShoeListScreen.kt
│   │   │   │   │   │   ├── ShoeDetailScreen.kt
│   │   │   │   │   │   └── ShoeViewModel.kt
│   │   │   │   │   ├── running/
│   │   │   │   │   │   ├── RunningScreen.kt
│   │   │   │   │   │   └── RunningViewModel.kt
│   │   │   │   │   ├── recommendations/
│   │   │   │   │   │   ├── RecommendationScreen.kt
│   │   │   │   │   │   └── RecommendationViewModel.kt
│   │   │   │   │   └── common/
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/
│   │   │   │   │   │   ├── RetrofitClient.kt
│   │   │   │   │   │   ├── ShoeService.kt
│   │   │   │   │   │   ├── RunningService.kt
│   │   │   │   │   │   └── RecommendationService.kt
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── User.kt
│   │   │   │   │   │   ├── Shoe.kt
│   │   │   │   │   │   └── RunningRecord.kt
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── ShoeRepository.kt
│   │   │   │   │   │   ├── RunningRepository.kt
│   │   │   │   │   │   └── RecommendationRepository.kt
│   │   │   │   │   └── local/
│   │   │   │   │       └── AppDatabase.kt
│   │   │   │   ├── di/
│   │   │   │   │   └── AppModule.kt
│   │   │   │   └── utils/
│   │   │   │       ├── Constants.kt
│   │   │   │       └── Extensions.kt
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       ├── drawable/
│   │   │       ├── values/
│   │   │       │   └── strings.xml
│   │   │       └── values-night/
│   │   ├── build.gradle.kts
│   │   └── AndroidManifest.xml
│   └── settings.gradle.kts
│
└── docs/
    ├── API.md                        # API 문서
    ├── ARCHITECTURE.md               # 아키텍처
    ├── SETUP.md                      # 설치 가이드
    └── CONTRIBUTING.md               # 기여 가이드

```

---

## 🚀 빠른 시작

### 사전 요구사항
- **Backend**: Python 3.9+, PostgreSQL, Redis
- **Frontend**: Android Studio, JDK 17+
- **API Keys**: Google Gemini API (무료), Google Maps API

### Backend 설치

```bash
cd backend
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt

# .env 파일 설정
cp .env.example .env
# .env 파일 수정 (API 키 등)

# 데이터베이스 마이그레이션
alembic upgrade head

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

## 📚 주요 기능

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
- 러닝 추천 점수 (0-100점)
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

## 📖 API 엔드포인트

### 신발 관련
- `POST /api/shoes` - 신발 등록
- `GET /api/shoes` - 신발 목록 조회
- `GET /api/shoes/{id}` - 신발 상세 조회
- `PUT /api/shoes/{id}` - 신발 정보 수정
- `DELETE /api/shoes/{id}` - 신발 삭제

### 신발 추천
- `POST /api/recommendations/shoes` - AI 기반 신발 추천

### 러닝 기록
- `POST /api/running` - 러닝 기록 저장
- `GET /api/running` - 러닝 기록 조회
- `GET /api/running/statistics` - 통계 조회

### 기상 조언
- `GET /api/weather/advice` - 현재 위치 기반 러닝 조언

---

## 🔧 개발 가이드

상세 개발 가이드는 [SETUP.md](docs/SETUP.md)를 참조하세요.

---

## 📝 라이선스

MIT License

---

## 📞 지원

문제 또는 제안사항은 GitHub Issues를 통해 등록해주세요.

---

**Happy Running! 🏃‍♀️🏃‍♂️**
