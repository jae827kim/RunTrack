"""
러닝 기록 API 라우트
- 러닝 세션 시작/종료, 기록 저장
- 러닝 기록 조회, 통계 분석
"""
from uuid import UUID

from fastapi import APIRouter, Depends, Path, Query, Response
from sqlalchemy.orm import Session
from app.api.dependencies import get_current_user
from app.database import get_db
from app.models import User
from app.schemas.running_record import (
    RunningRecordCreate, RunningRecordResponse, RunningStatistics, RunningRecordUpdate,
    RunningSessionStart, RunningSessionResponse, RunningSessionEnd,
)
from app.services.running import RunningService

router = APIRouter(responses={
    401: {"description": "Authentication required"},
    404: {"description": "Owned record or shoe not found"},
    409: {"description": "Session or shoe totals conflict"},
})


def get_running_service(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return RunningService(db, current_user)

# 러닝 세션 시작
@router.post("/start", response_model=RunningSessionResponse)
def start_running(
    request: RunningSessionStart | None = None,
    service: RunningService = Depends(get_running_service),
):
    """Start a persisted session or recover the owner's matching active session."""
    return service.start(request or RunningSessionStart())

# 러닝 세션 종료
@router.post("/{session_id}/end", response_model=RunningRecordResponse,
             responses={410: {"description": "Completed record was deleted"}})
def end_running(
    session_id: UUID,
    request: RunningSessionEnd,
    service: RunningService = Depends(get_running_service),
):
    """Finalize a run once; identical retries return the existing record."""
    return service.end(str(session_id), request)


@router.post("", response_model=RunningRecordResponse, status_code=201)
def create_running_record(
    request: RunningRecordCreate,
    service: RunningService = Depends(get_running_service),
):
    """Save a completed run and update the selected shoe's totals atomically."""
    return service.create_record(request)

# 러닝 기록 조회
@router.get("", response_model=list[RunningRecordResponse])
def get_running_records(
    skip: int = Query(0, ge=0),
    limit: int = Query(10, ge=1, le=100),
    service: RunningService = Depends(get_running_service),
):
    return service.list_records(skip, limit)

# 러닝 기록 상세
@router.get("/{record_id}", response_model=RunningRecordResponse)
def get_running_record(
    record_id: int = Path(..., gt=0),
    service: RunningService = Depends(get_running_service),
):
    return service.get_record(record_id)

# 전체 통계 요약
@router.get("/statistics/summary", response_model=RunningStatistics)
def get_summary_statistics(service: RunningService = Depends(get_running_service)):
    return service.summary()

# 주간 통계
@router.get("/statistics/weekly", response_model=dict[str, RunningStatistics])
def get_weekly_statistics(service: RunningService = Depends(get_running_service)):
    return service.weekly()

# 월간 통계
@router.get("/statistics/monthly", response_model=dict[str, RunningStatistics])
def get_monthly_statistics(service: RunningService = Depends(get_running_service)):
    return service.monthly()


@router.put("/{record_id}", response_model=RunningRecordResponse)
def update_running_record(
    request: RunningRecordUpdate,
    record_id: int = Path(..., gt=0),
    service: RunningService = Depends(get_running_service),
):
    """Draft: omitted fields stay unchanged, explicit null clears optional fields."""
    return service.update_record(record_id, request)


@router.delete("/{record_id}", status_code=204)
def delete_running_record(
    record_id: int = Path(..., gt=0),
    service: RunningService = Depends(get_running_service),
):
    service.delete_record(record_id)
    return Response(status_code=204)
