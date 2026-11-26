"""
러닝 기록 API 라우트
- 러닝 세션 시작/종료, 기록 저장
- 러닝 기록 조회, 통계 분석
- GPS 위치점 저장 및 조회
"""
from uuid import UUID
from math import radians, sin, cos, sqrt, atan2

from fastapi import APIRouter, Depends, HTTPException, Path, Query, Response
from sqlalchemy.orm import Session
from app.api.dependencies import get_current_user
from app.database import get_db
from app.models import User
from app.models.running_record import RunningRecord
from app.models.gps_point import GPSPoint
from app.schemas.running_record import (
    RunningRecordCreate, RunningRecordResponse, RunningStatistics, RunningRecordUpdate,
    RunningSessionStart, RunningSessionResponse, RunningSessionEnd,
)
from app.schemas.gps_point import GPSPointCreate, GPSPointResponse, GPSRouteResponse
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


def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """두 위도/경도 사이의 거리 계산 (킬로미터)"""
    R = 6371
    
    lat1_rad = radians(lat1)
    lon1_rad = radians(lon1)
    lat2_rad = radians(lat2)
    lon2_rad = radians(lon2)
    
    dlat = lat2_rad - lat1_rad
    dlon = lon2_rad - lon1_rad
    
    a = sin(dlat / 2) ** 2 + cos(lat1_rad) * cos(lat2_rad) * sin(dlon / 2) ** 2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return R * c

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

# GPS 위치점 추가 (실시간)
@router.post("/{record_id}/gps-points", response_model=GPSPointResponse)
async def add_gps_point(
    record_id: int, 
    gps_data: GPSPointCreate, 
    db: Session = Depends(get_db)
):
    """러닝 세션에 GPS 위치점 추가"""
    running_record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not running_record:
        raise HTTPException(status_code=404, detail="러닝 기록을 찾을 수 없습니다.")
    
    gps_point = GPSPoint(
        running_record_id=record_id,
        latitude=gps_data.latitude,
        longitude=gps_data.longitude,
        altitude_m=gps_data.altitude_m,
        speed_kmh=gps_data.speed_kmh,
        accuracy_m=gps_data.accuracy_m,
        bearing_degrees=gps_data.bearing_degrees,
        timestamp=gps_data.timestamp
    )
    
    db.add(gps_point)
    db.commit()
    db.refresh(gps_point)
    
    return GPSPointResponse.model_validate(gps_point)

# GPS 위치점 배치 추가
@router.post("/{record_id}/gps-points/batch")
async def add_gps_points_batch(
    record_id: int, 
    gps_data_list: list[GPSPointCreate], 
    db: Session = Depends(get_db)
):
    """러닝 세션에 여러 GPS 위치점 추가"""
    running_record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not running_record:
        raise HTTPException(status_code=404, detail="러닝 기록을 찾을 수 없습니다.")
    
    gps_points = [
        GPSPoint(
            running_record_id=record_id,
            latitude=gps_data.latitude,
            longitude=gps_data.longitude,
            altitude_m=gps_data.altitude_m,
            speed_kmh=gps_data.speed_kmh,
            accuracy_m=gps_data.accuracy_m,
            bearing_degrees=gps_data.bearing_degrees,
            timestamp=gps_data.timestamp
        )
        for gps_data in gps_data_list
    ]
    
    db.add_all(gps_points)
    db.commit()
    
    return {"message": f"{len(gps_points)}개의 GPS 위치점이 추가되었습니다.", "count": len(gps_points)}

# GPS 경로 조회
@router.get("/{record_id}/gps-route", response_model=GPSRouteResponse)
async def get_gps_route(record_id: int, db: Session = Depends(get_db)):
    """러닝 기록의 전체 GPS 경로 조회"""
    running_record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not running_record:
        raise HTTPException(status_code=404, detail="러닝 기록을 찾을 수 없습니다.")
    
    gps_points = db.query(GPSPoint).filter(
        GPSPoint.running_record_id == record_id
    ).order_by(GPSPoint.timestamp).all()
    
    if len(gps_points) < 2:
        raise HTTPException(status_code=400, detail="GPS 데이터가 충분하지 않습니다.")
    
    total_distance_km = 0.0
    for i in range(len(gps_points) - 1):
        point1 = gps_points[i]
        point2 = gps_points[i + 1]
        distance = haversine_distance(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude
        )
        total_distance_km += distance
    
    return GPSRouteResponse(
        running_record_id=record_id,
        points=[GPSPointResponse.model_validate(p) for p in gps_points],
        total_distance_km=total_distance_km,
        start_point=GPSPointResponse.model_validate(gps_points[0]),
        end_point=GPSPointResponse.model_validate(gps_points[-1])
    )

# GPS 포인트 조회 (페이징)
@router.get("/{record_id}/gps-points", response_model=list[GPSPointResponse])
async def get_gps_points(
    record_id: int,
    skip: int = Query(0, ge=0),
    limit: int = Query(100, ge=1, le=1000),
    db: Session = Depends(get_db)
):
    """러닝 기록의 GPS 포인트 조회"""
    running_record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not running_record:
        raise HTTPException(status_code=404, detail="러닝 기록을 찾을 수 없습니다.")
    
    gps_points = db.query(GPSPoint).filter(
        GPSPoint.running_record_id == record_id
    ).order_by(GPSPoint.timestamp).offset(skip).limit(limit).all()
    
    return [GPSPointResponse.model_validate(p) for p in gps_points]

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
