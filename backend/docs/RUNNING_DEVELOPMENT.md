# 러닝 기록 API 개발 기반

완료된 러닝 기록의 저장·조회·수정·삭제 및 통계가 구현되어 있다.
실제 요청/응답과 테스트 범위는 [러닝 API 계약](RUNNING_API_CONTRACT.md)을 참고한다.
README의 start/end를 기본 저장 흐름으로 구현했다. 시작은 running_sessions에,
종료는 기존 running_records에 저장한다. 보조 POST /api/running도 유지한다.

## 구조

- app/api/routes/running.py: 공통 인증, 요청 검증, 기존 응답 스키마 연결.
- app/services/running.py: 소유권 검사, 기록 저장 및 신발 통계의 원자적 변경, 통계 집계.
- app/schemas/running_record.py: 기존 필드를 유지하고 입력 검증·수정 가능 필드를 보완.
- app/models/running_record.py: PM의 기존 모델 유지.
- app/models/running_session.py: 진행 중 세션, 완료 기록 연결, 종료 요청 해시.
- app/migrate_running_sessions.py: 기존 DB에 세션 테이블만 추가하는 명령.
- tests/test_running.py: CRUD·소유권·통계·rollback 검증.
- tests/test_running_sessions.py: 시작/종료, 재전송, 소유권, 원자성 검증.
- tests/test_running_scaffold.py: 인증 및 조회 입력 검증.

동기 SQLAlchemy 세션에 맞춰 라우트는 def로 작성한다.
get_current_user로 인증한 사용자 ID를 서비스에 전달한다.
조회/수정/삭제에 사용자 조건을 적용하고 선택한 신발의 소유권도 검사한다.

## 후속 확장

1. 세션 취소·만료 정책.
2. 분 단위 정수에서 초 단위로 확장 여부, 일시정지 시간 처리.
3. 보조 POST의 중복 방지 식별자. start/end는 세션 ID로 중복 종료를 방지한다.

목록은 skip >= 0, 1 <= limit <= 100이며 상세 ID는 양수다.
조회와 통계 응답은 기존 Python 스키마 및 Android 초안 형태를 따른다.
start 응답의 UUID session_id와 end 응답의 정수 record id를 구분한다.
PUT/DELETE /api/running/{record_id}는 초안과 ROLES.md의 수정/삭제 요구를 보완한다.
PUT은 기존 필드와 거리·시간·시작/종료 시각을 받으며 생략은 유지한다.
null은 메모·신발 등 선택 필드만 비울 수 있다. 알 수 없는 필드와 잘못된 수치는 거부한다.

## 후속 구현

기록과 신발 통계는 한 트랜잭션에서 변경하며 PostgreSQL의 사용자/신발 행 잠금을 사용한다.
실제 PostgreSQL 동시성 검증과 모바일 연결은 후속 작업이다.
날씨와 AI 추천의 전체 골격은 BACKEND2_STRUCTURE.md에 정리한다.

## 로컬 검증

backend 폴더에서 실행:

```powershell
.\.venv\Scripts\python.exe -m pytest -q
```

테스트는 팀원의 격리된 SQLite fixture와 테스트 JWT를 사용한다.
PostgreSQL, 실제 서버, Android 연동 검증은 별도로 필요하다.
