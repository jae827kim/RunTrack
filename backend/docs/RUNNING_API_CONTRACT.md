# 러닝 시작·종료 및 기록 API

2026-10-08: README의 시작/종료·목록/상세·전체/주간/월간 통계 7개 API가 기본 흐름이다.
모든 요청에 `Authorization: Bearer <token>`이 필요하다.

| 기능 | 경로 | 성공 |
| --- | --- | --- |
| 시작/진행 중 세션 복구 | POST /api/running/start | 200, RunningSessionResponse |
| 종료 및 저장 | POST /api/running/{session_id}/end | 200, RunningRecordResponse |
| 저장 | POST /api/running | 201, RunningRecordResponse |
| 목록 | GET /api/running?skip=0&limit=10 | 200, 기록 배열 |
| 상세 | GET /api/running/{record_id} | 200, RunningRecordResponse |
| 수정 | PUT /api/running/{record_id} | 200, RunningRecordResponse |
| 삭제 | DELETE /api/running/{record_id} | 204, 빈 응답 |
| 전체 통계 | GET /api/running/statistics/summary | 200, RunningStatistics |
| 주간 통계 | GET /api/running/statistics/weekly | 200, 주 시작일 → 통계 |
| 월간 통계 | GET /api/running/statistics/monthly | 200, YYYY-MM → 통계 |

초기 create/list/update/delete/stats 주소를 별도로 중복 생성하지 않는다.
기본 저장은 /start → /{session_id}/end 방식이다. POST /api/running은 앞서 구현한
완료 기록 직접 입력용 보조 API로 유지하며 start/end를 대체하지 않는다.
날씨·Gemini 추천은 이번 구현 범위에 포함하지 않는다.

## 기본 흐름: 시작 → 종료

POST /api/running/start 요청 (본문 생략 가능):

```json
{"shoe_id": 1, "start_time": "2026-10-08T09:00:00+09:00"}
```

start_time을 생략하면 서버의 현재 UTC 시각을 사용한다. 신발은 생략/null 가능하다.
200 응답 예시:

```json
{"session_id": "871b4aa7-9b4e-421d-9f29-185251517e31", "start_time": "2026-10-08T00:00:00", "shoe_id": 1, "status": "active"}
```

사용자당 진행 중 세션은 하나다. 같은 시작 요청이나 본문 없는 재요청은 기존 세션을
반환한다. 다른 신발/시작 시각을 명시하면 409다. 앱이 종료돼도 DB에 세션이 남는다.
진행 중 세션은 완료 기록 목록·통계·신발 누적값에 포함하지 않는다.

POST /api/running/871b4aa7-9b4e-421d-9f29-185251517e31/end 요청:

```json
{"distance_km": 5, "duration_minutes": 30, "end_time": "2026-10-08T09:30:00+09:00", "notes": "아침 러닝"}
```

거리·운동 시간·종료 시각은 필수다. 시작 시각/신발/사용자 ID는 세션에서 가져오므로
종료 본문으로 변경할 수 없다. 200 응답은 저장된 기록이며 그 id(int)를 상세 조회에 사용한다.
session_id(UUID 문자열)와 완료 기록 id(int)는 별개다.

세션 완료·기록 생성·신발 거리/횟수 갱신을 한 트랜잭션으로 처리한다.
같은 세션에 같은 종료 내용을 재전송하면 기존 기록을 반환하며 다시 적립하지 않는다.
다른 종료 내용은 409, 타인/없는 세션은 404, 이미 완료 후 삭제된 기록은 410이다.
종료 요청의 정규화된 본문 해시를 저장하며 선택 필드 생략/null은 동일하게 취급한다.
완료 후 기록을 PUT으로 수정했다면 동일 종료 재시도는 현재 수정된 기록을 반환한다.
실패한 종료는 세션을 진행 중 상태로 유지한다.

## 보조 API: 완료 기록 직접 입력

POST /api/running 요청:

```json
{
  "distance_km": 5.0,
  "duration_minutes": 30,
  "start_time": "2026-10-08T09:00:00+09:00",
  "end_time": "2026-10-08T09:30:00+09:00",
  "shoe_id": 1,
  "notes": "아침 러닝"
}
```

거리·운동 시간·시작/종료 시각이 필수다. 신발은 생략/null 가능하다.
거리와 운동 시간은 양수이며 시간은 기존 DB에 맞춰 정수 분 단위다.
날짜는 ISO 8601, 시간대 없는 입력은 UTC로 해석한다. 시간대가 있는 입력은
UTC로 변환해 기존 timezone-naive DB 컬럼에 저장한다. 응답의 시간대 없는
날짜 문자열도 UTC다. 앱은 한국 시각에 반드시 +09:00을 붙여 전송해야 한다.
end_time은 start_time보다 뒤여야 한다. duration_minutes는 실제 운동 시간이며
일시정지 등을 고려해 시작·종료 시각 차이로 덮어쓰거나 동일함을 강제하지 않는다.

평균 페이스와 속도는 서버가 각각 시간/거리와 거리*60/시간으로 계산한다.
보조 POST만 avg_pace_min_per_km 및 avg_speed_kmh 입력을 호환용으로 받되 덮어쓴다.
user_id, calories_burned 등 정의되지 않은 요청 필드는 422로 거부한다.
칼로리 추정은 하지 않으며 입력하지 않은 측정값은 null로 유지한다.

## 조회·수정·삭제

목록은 본인 기록만 시작 시각 내림차순, 같은 시각은 ID 내림차순이다.
skip >= 0, limit은 1~100이다. 마지막 페이지를 넘으면 빈 배열이다.
상세/수정/삭제는 본인 소유이면서 존재하는 ID만 처리하며 나머지는 404다.
선택한 신발이 없거나 타인 소유여도 404를 반환한다.

PUT은 부분 수정이다. title, description, shoe_id, feeling, notes와
distance_km, duration_minutes, start_time, end_time을 지원한다.
생략한 값은 유지한다. 명시적 null은 선택 필드만 비우고 필수 거리/시간/시각은
null을 거부한다. 빈 객체는 기존 값을 유지한다. 변경 후에도 시각 순서를 검사한다.

## 신발 누적값과 원자성

저장 시 거리/횟수를 더하고, 수정 시 이전 신발에서 이전 값을 차감한 뒤
새 신발에 새 값을 더한다. 삭제 시 차감한다. 기록과 누적값은 같은 트랜잭션에
저장하고 실패하면 rollback한다. 기존 통계와 기록이 모순되면 409로 거부한다.
PostgreSQL에서 사용자 행 잠금으로 동일 사용자 쓰기를 직렬화하고 신발 행도
ID 순서로 잠근다. 신발 API PR #4는 아직 미병합이므로 그 코드는 복사하지 않았다.
테스트에서 신발을 직접 준비하여 공통 ORM과 통계 컬럼 연결을 검증한다.

## 통계

모든 통계는 로그인 사용자의 전체 기록 대상이다. 기간 선택 필터는 아직 없다.
평균 페이스는 총 시간/총 거리, 평균 운동 시간은 소수점을 보존한다(float).
이는 기존 RunningStatistics.avg_duration_minutes의 int에서 변경된 계약이므로
Android 응답 모델도 실수형으로 맞춰야 한다.
심박수/상승 고도 평균은 null 값을 제외한 기록의 산술 평균이다.
기록이 없으면 횟수/거리/시간/페이스/칼로리는 0, 선택 평균은 null이다.
칼로리는 저장된 값의 합만 제공하며 null은 합산에서 제외한다.
주간은 한국 시간(UTC+09:00) 시작일 기준 월요일, 월간도 한국 시작일 기준이다.
주간 키 예: 2026-10-05, 월간 키 예: 2026-10. 기록이 없는 주/월은 생략한다.

## 한계와 검증

- 종료 재시도 중복 방지는 구현돼 있다. 보조 POST /api/running에는 중복 방지가 없다.
- 세션 취소·자동 만료는 아직 없다. 본문 없는 start로 기존 세션을 복구해 종료할 수 있다.
- 시작 후 신발이 DB에서 삭제되면 FK의 ON DELETE SET NULL로 세션 신발 연결을 해제한다.
- 시간은 정수 분 단위이며 초 단위 정밀 측정은 후속 모델 변경이 필요하다.
- 집계는 사용자 기록을 메모리에 읽어 처리한다. 대규모 데이터는 DB 집계 최적화가 필요하다.
- pytest는 격리된 SQLite로 실제 HTTP 요청 경로, 영속화, 소유권,
  누적 거리 이동/해제, 잘못된 입력, 주/월 경계와 실패 rollback을 검증한다.
- PostgreSQL 행 잠금/동시성, PR #4와의 실제 통합 및 Android 연동은 미검증이다.

실행: backend에서 `.\.venv\Scripts\python.exe -m pytest -q`.

## 기존 DB 적용

RunningRecord 테이블은 변경하지 않고 running_sessions 테이블과 활성 세션 유일 인덱스를
추가한다. users, shoes, running_records가 이미 생성된 DB에서 설정을 확인한 뒤 실행한다:

```powershell
.\.venv\Scripts\python.exe -m app.migrate_running_sessions
```

이 명령은 신규 테이블만 생성하며 재실행 시 기존 테이블을 덮어쓰지 않는다.
빈 개발 DB는 `import app.models` 후 Base.metadata.create_all(bind=engine)로 전체 생성한다.
이번 작업에서 실제 PostgreSQL에 명령을 실행하지 않았으며 SQLite 이행 테스트로 검증한다.
