# 👥 RunTrack 팀 역할 정의

**프로젝트**: AI 기반 맞춤형 러닝 조언 앱  
**팀 구성**: 5명 (PM, Backend 1, Backend 2, Frontend, DevOps & QA)  
**개발 기간**: 7주  
**저장소**: https://github.com/jae827kim/RunTrack

---

## 🔴 PM (프로젝트 매니저)

**담당 역할:**
- 전체 프로젝트 일정 관리 및 팀 회의 진행
- 데이터베이스 설계 (ERD)
- ORM 모델 작성 (SQLAlchemy)
- Pydantic 검증 스키마 작성
- 통합 테스트 작성 및 검증

**구현 파일:**
```
backend/app/models/
  ├── user.py              (User ORM 모델)
  ├── shoe.py              (Shoe ORM 모델)
  └── running_record.py    (RunningRecord ORM 모델)

backend/app/schemas/
  ├── user.py              (UserCreate, UserResponse, UserUpdate)
  ├── shoe.py              (ShoeCreate, ShoeResponse, ShoeUpdate)
  └── running_record.py    (RunningRecordCreate, RunningRecordResponse)

backend/tests/
  └── test_integration.py  (통합 테스트 20개)
```

**DB 설계 항목:**
- User: id, email, password, name, weight_kg, height_cm, created_at, updated_at
- Shoe: id, user_id(FK), brand, model, size, color, purchase_date, cumulative_km, created_at, updated_at
- RunningRecord: id, user_id(FK), shoe_id(FK), distance_km, duration_minutes, date, temperature, humidity, wind_speed, feeling, created_at, updated_at

**일정:**
| 주차 | 작업 |
|------|------|
| 1주 | DB 설계 (ERD), 테이블 구조 정의 |
| 2주 | ORM 모델 & Pydantic 스키마 작성 |
| 3-4주 | 통합 테스트 작성 및 검증 |
| 5주 | 전체 시스템 통합 점검 |
| 6주 | 발표 자료 준비 |
| 7주 | 최종 발표 |

---

## 🔵 Backend 1 (사용자 & 신발 관리)

**담당 역할:**
- 사용자 인증 시스템 (회원가입, 로그인)
- 신발 관리 CRUD API
- JWT 토큰 발급 및 검증
- 비밀번호 해싱 (bcrypt)

**구현 파일:**
```
backend/app/api/routes/
  ├── users.py   (회원가입, 로그인, 프로필 조회/수정)
  └── shoes.py   (신발 추가, 조회, 수정, 삭제)

backend/tests/
  ├── test_users.py   (6개 테스트)
  └── test_shoes.py   (6개 테스트)
```

**구현할 API (9개):**
```
POST   /api/users/signup         - 회원가입
POST   /api/users/login          - 로그인
GET    /api/users/me             - 프로필 조회 (인증필요)
PUT    /api/users/update         - 프로필 수정 (인증필요)
POST   /api/shoes                - 신발 추가
GET    /api/shoes                - 신발 목록
GET    /api/shoes/{id}           - 신발 상세
PUT    /api/shoes/{id}           - 신발 수정
DELETE /api/shoes/{id}           - 신발 삭제
```

**체크리스트:**
- [ ] PM의 User & Shoe 모델 import
- [ ] 비밀번호 해싱 (bcrypt)
- [ ] JWT 토큰 생성 및 검증
- [ ] 이메일 중복 검증
- [ ] 신발 소유권 검증
- [ ] 에러 처리 (400, 401, 403, 404)
- [ ] 테스트 12개 작성

**일정:**
| 주차 | 작업 |
|------|------|
| 1주 | API 설계, 요구사항 정리 |
| 2주 | 회원가입/로그인 구현 |
| 3주 | 신발 API 구현 |
| 4주 | 테스트 & 최적화 |
| 5주 | 버그 수정 & 보안 점검 |
| 6주 | 최종 최적화 |
| 7주 | 최종 발표 |

---

## 🟣 Backend 2 (러닝기록 & 기상정보 & AI)

**담당 역할:**
- 러닝 기록 CRUD API
- 기상 정보 API 통합 (OpenWeather)
- Google Gemini AI 신발 추천 엔진
- 러닝 통계 계산 및 분석

**구현 파일:**
```
backend/app/api/routes/
  ├── running.py    (러닝 기록 저장/조회/통계)
  └── weather.py    (기상 정보, 체감온도, 러닝 추천 점수)

backend/app/api/ai/
  └── recommendations.py  (Google Gemini 신발 추천)

backend/tests/
  ├── test_running.py              (7개 테스트)
  ├── test_weather.py              (3개 테스트)
  └── test_ai_recommendations.py   (5개 테스트)
```

**구현할 API (11개):**
```
POST   /api/running                      - 러닝 기록 저장
GET    /api/running                      - 러닝 기록 조회
GET    /api/running/{id}                 - 러닝 기록 상세
PUT    /api/running/{id}                 - 러닝 기록 수정
DELETE /api/running/{id}                 - 러닝 기록 삭제
GET    /api/running/statistics/summary   - 전체 통계
GET    /api/running/statistics/weekly    - 주간 통계
GET    /api/running/statistics/monthly   - 월간 통계
GET    /api/weather/advice               - 기상 조언 (위도, 경도)
GET    /api/weather/history              - 기상 이력
POST   /api/recommendations/shoes        - AI 신발 추천
```

**체크리스트:**
- [ ] PM의 RunningRecord 모델 import
- [ ] 러닝 기록 저장 (거리, 시간, 신발ID, 기상정보)
- [ ] 신발 누적 거리 자동 업데이트
- [ ] 평균 페이스 계산
- [ ] OpenWeather API 연동
- [ ] 러닝 추천 점수 계산 (0-100)
- [ ] Google Gemini API 연동
- [ ] 사용자 러닝 패턴 분석
- [ ] 통계 계산 (주간, 월간, 연간)
- [ ] 사용자 데이터 필터링
- [ ] 테스트 15개 작성

**필요한 API 키:**
- GEMINI_API_KEY
- OPENWEATHER_API_KEY

**일정:**
| 주차 | 작업 |
|------|------|
| 1주 | API 설계 & 외부 API 문서 학습 |
| 2주 | 러닝 기록 API 구현 |
| 3주 | 기상 정보 & AI 추천 엔진 |
| 4주 | 통계 로직 & AI 프롬프트 최적화 |
| 5주 | 버그 수정 & 성능 개선 |
| 6주 | 최종 테스트 |
| 7주 | 최종 발표 |

---

## 🟢 Frontend (Kotlin/Android)

**담당 역할:**
- 6개 화면 설계 및 구현
- Backend API 연동 (Retrofit)
- MVVM 패턴 + Hilt DI
- 사용자 인터페이스 개선

**구현할 화면 (6개):**

| 화면 | 기능 |
|------|------|
| 로그인/회원가입 | 이메일/비밀번호/이름 입력, 토큰 저장, 로그인 상태 유지 |
| 홈 화면 | 프로필 표시, 최근 기록 3개, 주간 거리/시간, 버튼 (기록/신발/추천) |
| 러닝 기록 리스트 | 모든 기록 조회, 날짜/거리/시간/신발 표시, 필터링 |
| 러닝 기록 상세/추가 | 거리/시간/신발 입력, 기상정보 자동 조회, 저장/수정/삭제 |
| 신발 관리 | 신발 목록/추가/수정/삭제, 누적거리 표시 |
| AI 추천 결과 | 로딩 표시, 추천 신발 목록 (3-5개), 추천 이유 표시 |

**구현 파일 구조:**
```
ui/
  ├── auth/           (로그인/회원가입)
  ├── home/           (홈 화면)
  ├── running/        (기록 리스트, 상세, 추가)
  ├── shoes/          (신발 관리)
  ├── recommendations/ (AI 추천)
  ├── navigation/     (화면 네비게이션)
  └── common/         (공통 컴포넌트)

data/
  ├── api/            (Retrofit, API Services)
  ├── models/         (User, Shoe, RunningRecord)
  └── repository/     (Auth, Running, Shoe repos)

di/
  └── AppModule.kt    (Hilt DI 설정)
```

**체크리스트:**
- [ ] Retrofit 클라이언트 설정
- [ ] 6개 화면 Jetpack Compose 구현
- [ ] MVVM 패턴 (ViewModel, LiveData/StateFlow)
- [ ] Repository 패턴 적용
- [ ] JWT 토큰 저장/관리
- [ ] 토큰 만료 시 자동 재로그인
- [ ] 인터넷 연결 상태 체크
- [ ] 입력값 유효성 검증
- [ ] 에러 처리 및 사용자 알림
- [ ] 로딩 상태 표시
- [ ] 빈 상태 처리

**일정:**
| 주차 | 작업 |
|------|------|
| 1주 | 화면 와이어프레임 & 레이아웃 설계 |
| 2주 | 로그인/회원가입 & 홈 화면 |
| 3주 | 러닝 기록, 신발 관리 화면 |
| 4주 | AI 추천 화면 & 네비게이션 |
| 5주 | Backend API 연동 & 버그 수정 |
| 6주 | UI 개선 & 성능 최적화 |
| 7주 | 최종 발표 |

---

## 🟠 DevOps & QA (배포 & 테스트)

**담당 역할:**
- Docker 이미지 빌드
- CI/CD 파이프라인 구축
- 자동 테스트 실행
- 배포 자동화
- 모니터링 & 성능 테스트

**구현 파일:**
```
RunTrack/
├── Dockerfile              (Backend 컨테이너)
├── docker-compose.yml      (Backend, PostgreSQL, Redis)
├── .dockerignore

.github/workflows/
├── ci.yml                  (지속적 통합)
├── deploy.yml              (지속적 배포)
└── lint.yml                (코드 검사)
```

**체크리스트:**

**Dockerfile:**
- [ ] Base 이미지: python:3.11-slim
- [ ] 작업 디렉토리: /app
- [ ] requirements.txt 설치
- [ ] 포트 8000 노출
- [ ] 헬스 체크 추가
- [ ] 유니콘 실행

**docker-compose.yml:**
- [ ] Backend 서비스 (8000)
- [ ] PostgreSQL 서비스 (5432)
- [ ] Redis 서비스 (6379)
- [ ] 환경변수 설정
- [ ] 볼륨 설정 (데이터 영속성)
- [ ] 네트워크 구성

**GitHub Actions CI (ci.yml):**
- [ ] Python 3.11 설정
- [ ] 의존성 설치
- [ ] 코드 스타일 검사 (flake8, black)
- [ ] 타입 체크 (mypy)
- [ ] 단위 테스트 (pytest)
- [ ] 커버리지 리포트

**GitHub Actions CD (deploy.yml):**
- [ ] Docker 이미지 빌드
- [ ] Docker Hub/Registry에 푸시
- [ ] 서버 배포 (SSH)
- [ ] 헬스 체크
- [ ] Slack 알림

**GitHub Actions Lint (lint.yml):**
- [ ] Black (코드 포맷)
- [ ] isort (import 정렬)
- [ ] Flake8 (코드 스타일)
- [ ] MyPy (타입 체크)

**성능 & 모니터링:**
- [ ] 부하 테스트 (locust)
- [ ] 성능 테스트
- [ ] API 응답 시간 < 500ms
- [ ] 메모리 프로파일링
- [ ] 구조화된 로깅 (JSON)

**일정:**
| 주차 | 작업 |
|------|------|
| 1주 | Dockerfile & docker-compose.yml 작성 |
| 2주 | GitHub Actions CI/CD 파이프라인 |
| 3주 | 자동 테스트 실행 설정 |
| 4주 | 배포 파이프라인 테스트 |
| 5주 | 성능 테스트 & 모니터링 |
| 6주 | 프로덕션 배포 준비 |
| 7주 | 최종 배포 & 검증 |

---

## 📅 전체 개발 일정 (7주)

| 주차 | PM | Backend 1 | Backend 2 | Frontend | DevOps & QA |
|------|-----|------|------|------|------|
| **1주** | DB 설계 | API 설계 | API 설계 | 화면 설계 | 환경 구축 |
| **2주** | ORM/Pydantic | 회원가입/로그인 | 러닝 API | 로그인/홈 | Docker/CI/CD |
| **3주** | 통합 테스트 | 신발 API | 기상/AI API | 기록/신발 | 테스트 자동화 |
| **4주** | 통합 검증 | API 완성 | AI 완성 | 추천 화면 | 배포 파이프라인 |
| **5주** | 시스템 점검 | 버그 수정 | 버그 수정 | UI 수정 | 최종 테스트 |
| **6주** | 발표 준비 | 최적화 | 최적화 | 최적화 | 배포 |
| **7주** | 최종 발표 | 최종 발표 | 최종 발표 | 최종 발표 | 배포 검증 |

---

