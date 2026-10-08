# 제 안 서

---

## 제안명

**RunTrack 백엔드 QA Phase 1: Backend 1 전수 테스트 및 CI/CD 파이프라인 검증**

---

## 개요

현재 RunTrack 프로젝트는 Backend 1(인증 및 신발 관리)의 모든 엔드포인트 구현을 완료한 상태입니다. 
이 단계에서 QA 팀은 구현된 API의 정확성, 안정성, 보안성을 검증하고, 
CI/CD 파이프라인의 정상 작동을 확인하는 **전수 검사(Regression Testing)**를 수행해야 합니다.

### 현재 상태
- **Backend 1**: 9개 엔드포인트 완성 (회원가입, 로그인, 프로필, 신발 CRUD)
- **Backend 2**: 라우팅/스키마 완성 (서비스 로직 구현 진행 중)
- **테스트 인프라**: 기본 pytest 프레임워크 및 GitHub Actions 파이프라인 구성 완료
- **Database**: PostgreSQL/Redis 헬스체크 필요

### 목표
- Backend 1의 모든 엔드포인트를 정상 작동 기준으로 검증
- 보안 및 권한 관리 로직 검증
- CI/CD 파이프라인의 자동화 확인
- Production 배포 전 품질 보증 (Quality Gate 통과)

---

## 목적 및 구현 계획

### **Phase 1-1: 환경 구성 및 기초 검증** (3일, 10/8-10/10)

#### 1. Docker 및 Database 환경 구성
- Docker Compose로 PostgreSQL(v15), Redis(v7) 정상 구동 확인
- 데이터베이스 마이그레이션/테이블 생성 확인
- 헬스 체크 엔드포인트 검증
```bash
docker-compose up -d
pytest backend/tests/ -v  # DB 초기화
curl http://localhost:8000/health  # 200 OK 확인
```

#### 2. Backend 서버 실행 및 Swagger API 문서 로드
- Uvicorn 서버 정상 실행
- Swagger UI (localhost:8000/docs) 접근 가능 여부 확인
- API 엔드포인트 목록 자동 생성 확인

#### 3. 기존 테스트 유틸 검증
- test_auth_integration.py, test_shoes.py, test_running.py 실행
- 현재 통과/실패 상태 파악
- 실패 시 원인 분석 및 버그 이슈 등록

### **Phase 1-2: Backend 1 전수 테스트** (4일, 10/11-10/14)

#### 4. 인증 API 테스트
| 엔드포인트 | 테스트 항목 | 기대값 |
|-----------|----------|--------|
| POST /auth/register | 정상 회원가입, 중복 이메일 거절, 필수필드 검증 | 201, 409, 422 |
| POST /auth/login | 정상 로그인, 잘못된 암호, 유효성 검사 | 200+JWT, 401, 422 |

#### 5. 사용자 프로필 API 테스트
| 엔드포인트 | 테스트 항목 | 기대값 |
|-----------|----------|--------|
| GET /users/me | 인증된 사용자 프로필, 미인증 요청 | 200, 401 |
| PUT /users/me | 부분 수정, 전체 수정, 잘못된 데이터 | 200, 422 |

#### 6. 신발 관리 API 테스트
| 엔드포인트 | 테스트 항목 | 기대값 |
|-----------|----------|--------|
| POST /shoes | 신발 생성, 유효성 검사 | 201, 422 |
| GET /shoes | 사용자 신발 목록 조회 | 200 (배열) |
| GET /shoes/{id} | 신발 상세, 다른 사용자 신발 접근 | 200, 404 |
| PUT /shoes/{id} | 부분 수정, 전체 수정 | 200, 422 |
| DELETE /shoes/{id} | 정상 삭제, 재조회 실패 | 204, 404 |
| GET /shoes/{id}/stats | 신발 통계 (누적km, 러닝 횟수) | 200 |

#### 7. 권한 및 보안 테스트
- JWT 토큰 없이 보호된 엔드포인트 접근 시도 → 401
- 만료된 토큰 사용 → 401 또는 토큰 갱신
- 다른 사용자의 신발 접근 시도 → 404 또는 403
- SQL Injection 테스트 (특수문자 입력)

### **Phase 1-3: CI/CD 파이프라인 검증** (2일, 10/13-10/14)

#### 8. GitHub Actions 워크플로우 검증
- **Lint 체크**: flake8으로 코드 스타일 위반 확인
  - 라인 길이 초과 (100자 제한) 
  - Import 순서, 네이밍 컨벤션
  
- **테스트 자동화**: pytest 전수 실행 및 커버리지
  - 성공 테스트 수 / 전체 테스트 수
  - 커버리지 목표: 80% 이상
  - 실패하는 테스트 분석 및 이슈 등록
  
- **배포 워크플로우**: Docker 이미지 빌드/푸시
  - Docker 이미지 빌드 성공 여부
  - Registry 푸시 성공 여부

#### 9. 데이터베이스 스키마 검증
```sql
-- 테이블 구조 확인
\d users, shoes, running_records

-- 제약조건 확인
- users.email UNIQUE
- shoes.user_id FK → users.id
- running_records.user_id FK → users.id
- running_records.shoe_id FK → shoes.id
- running_records.updated_at 컬럼 존재 확인
- running_records.shoe relationship 정의 확인

-- 샘플 데이터 무결성
SELECT * FROM users LIMIT 1;  -- 정상 조회
```

#### 10. 최종 Quality Gate 리포트
- ✅ 통과한 엔드포인트 목록
- ❌ 실패한 엔드포인트 (스크린샷, 에러 로그)
- 🔒 보안 취약점 (있으면 우선순위 정의)
- 📊 테스트 커버리지 (%)
- 📌 발견된 버그 (이슈 번호, 심각도)

---

## 기대효과 및 활용 시례

### **즉시 효과 (Week 3-4)**
1. **버그 조기 발견**: Backend 1의 결함을 배포 전 식별
   - 예시: JWT 토큰 검증 실패, 권한 체크 누락 등
   
2. **회귀 테스트 기반 구축**: 향후 기능 추가 시 기존 기능 파괴 방지
   - 자동화된 테스트 스위트로 매 배포마다 재검증
   
3. **CI/CD 신뢰도 향상**: 자동화 파이프라인의 정상 작동 확인
   - 코드 품질 게이트 통과 → 배포 승인 프로세스 확립

### **중기 효과 (Week 5-6)**
4. **Frontend 통합 테스트 준비**: API 명세서 정확성 보증
   - Frontend 팀이 신뢰할 수 있는 API로 개발 가속화
   
5. **Performance 기반선(Baseline) 확립**: 
   - API 응답시간, DB 쿼리 성능 측정
   - 향후 최적화의 비교 지표

### **장기 효과 (Production)**
6. **안정성 있는 배포**: Production에서의 장애 사전 방지
7. **고객 신뢰도**: 서비스 안정성 확보

### **활용 시례**

**시례 1: 신발 수정 API 검증**
```
요청: PUT /shoes/1
본문: {"condition": "worn", "notes": "왼쪽이 조금 손상됨"}

기대 응답:
{
  "id": 1,
  "user_id": 123,
  "brand": "Nike",
  "condition": "worn",  ← 수정됨
  "notes": "왼쪽이 조금 손상됨",  ← 수정됨
  "updated_at": "2026-10-08T15:30:00"  ← 갱신됨
}

검증: 200 OK + 응답 필드 일치도 100%
```

**시례 2: 다른 사용자 신발 접근 차단**
```
사용자 A 토큰으로: DELETE /shoes/5 (사용자 B의 신발)
기대: 404 Not Found

검증: 권한 체크 정상 작동 ✓
```

**시례 3: CI/CD 자동 검증**
```
PR 생성 → GitHub Actions 자동 실행
  ✓ flake8 lint pass
  ✓ 32/35 테스트 통과 (커버리지 82%)
  ⚠️ 3개 테스트 실패 → 수정 요청
```

---

## 참고 자료

### 필수 기술 스택
- **API 테스트**: Postman, curl, Python pytest
- **데이터베이스**: PostgreSQL 15, psql CLI
- **자동화**: GitHub Actions, Docker
- **모니터링**: pytest-cov (커버리지), GitHub Actions 로그

### 관련 문서
- `backend/tests/test_auth_integration.py`: 인증 테스트 케이스
- `backend/tests/test_shoes.py`: 신발 API 테스트
- `.github/workflows/test.yml`: CI/CD 테스트 워크플로우
- `DATABASE_DESIGN.md`: 데이터베이스 스키마 정의

### 일정
- **시작일**: 2026-10-08 (지금)
- **완료일**: 2026-10-14 (Week 3 마감)
- **산출물**: QA Phase 1 최종 리포트 (통과/실패 엔드포인트, 버그 이슈, 커버리지 %)

### 성공 기준 (Quality Gate)
- ✅ Backend 1 모든 엔드포인트 정상 작동 (201/201 통과)
- ✅ 권한/보안 검사 정상 작동
- ✅ 테스트 커버리지 80% 이상
- ✅ GitHub Actions 파이프라인 자동화 확인
- ✅ 발견된 중요 버그 0개 (또는 해결됨)

---

**작성일**: 2026-10-08  
**제안자**: Backend PM  
**승인자**: CTO (예정)  

