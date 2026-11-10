# Backend 2 구현 범위 및 검증

> 러닝 기록 CRUD, 세션 시작·종료 및 통계가 구현되어 있다. 상세 계약은
> [RUNNING_API_CONTRACT.md](RUNNING_API_CONTRACT.md)를 따른다. 기상 연동은 현재 범위에서 제외한다.

러닝 API는 인증된 사용자의 기록만 처리하며 신발 거리·횟수 갱신을 기록 변경과 함께 처리한다.

## 경로

| 메서드 | 실제 경로 | 목적 |
| --- | --- | --- |
| POST | /api/running/start | 세션 시작 |
| POST | /api/running/{session_id}/end | 종료 및 기록 저장 |
| GET | /api/running | 기록 목록 |
| GET | /api/running/{record_id} | 기록 상세 |
| PUT | /api/running/{record_id} | 기록 수정 |
| DELETE | /api/running/{record_id} | 기록 삭제 |
| GET | /api/running/statistics/summary | 전체 통계 |
| GET | /api/running/statistics/weekly | 주간 통계 |
| GET | /api/running/statistics/monthly | 월간 통계 |
| GET | /api/weather/advice | 날씨와 러닝 조언 |
| GET | /api/weather/history | 기상 이력 |

러닝 API는 인증이 필요하다. 타인 소유 기록과 신발은 조회·수정할 수 없다.
기상 경로는 현재 미구현 상태이며 이번 핵심 범위에 포함하지 않는다.

## 파일과 역할

```text
app/
  api/routes/running.py           요청 검증·인증·러닝 서비스 연결
  api/routes/weather.py           날씨 요청 및 서비스 연결
  services/running.py             DB 변경·소유권·누적 거리·통계 구현 자리
  services/weather.py             OpenWeather 호출·단위 변환·조언 구현 자리
  schemas/running_record.py       기존 PM 스키마 재사용
  schemas/weather.py              날씨 응답 초안
```

기상 연동을 다시 범위에 추가할 때에는 서비스 공급자, 데이터 출처, 오류 및 시간 제한 정책을 먼저 확정한다.

## 요청·응답 초안

- 러닝: 시작 응답 및 종료 요청 본문은 세션 저장 방식을 정한 뒤 추가한다.
  기존 DB 모델에 맞춰 분 단위 필드를 유지하며 초 단위로 임의 변경하지 않았다.
- 수정: title, description, shoe_id, feeling, notes만 허용한다.
  기록·신발 소유권 검사는 서비스 실제 구현 단계에서 추가해야 한다.
- 기상 API는 현재 외부 공급자와 연결하지 않는다.

## 실제 구현 시 남은 일

1. Android 앱에서 종료 요청 필드와 시간대가 계약대로 전송되는지 통합 검증한다.
2. PostgreSQL에서 사용자별 동시 기록 변경과 행 잠금 동작을 별도로 검증한다.
3. 기상 기능은 범위 변경 승인이 있는 경우에만 공급자·데이터·오류 정책을 재설계한다.

## 검증 범위

backend에서 `.\.venv\Scripts\python.exe -m pytest -q`로 실행한다.
인증, 러닝 세션·기록·통계 및 입력 검증을 검사한다.
이는 PostgreSQL 동시성, Android 통합 또는 외부 API 연동 검증을 포함하지 않는다.
