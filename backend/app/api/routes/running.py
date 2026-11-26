"""
러닝 기록 API 라우트
- 러닝 세션 시작/종료, 기록 저장
- 러닝 기록 조회, 통계 분석
- GPS 위치점 저장 및 조회
"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app.models.running_record import RunningRecord
from app.models.gps_point import GPSPoint
from app.schemas.gps_point import GPSPointCreate, GPSPointResponse, GPSRouteResponse
from datetime import datetime
from math import radians, sin, cos, sqrt, atan2

router = APIRouter()

# 헬퍼: 두 좌표 간의 거리 계산 (Haversine 공식)
def haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """
    두 위도/경도 사이의 거리 계산 (킬로미터)
    """
    R = 6371  # 지구 반경 (km)
    
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
@router.post("/start")
async def start_running(db: Session = Depends(get_db)):
    pass

# 러닝 세션 종료
@router.post("/{session_id}/end")
async def end_running(session_id: str, db: Session = Depends(get_db)):
    pass

# 러닝 기록 조회
@router.get("")
async def get_running_records(skip: int = 0, limit: int = 10, db: Session = Depends(get_db)):
    pass

# 러닝 기록 상세
@router.get("/{record_id}")
async def get_running_record(record_id: int, db: Session = Depends(get_db)):
    pass

# GPS 위치점 추가 (러닝 중 실시간)
@router.post("/{record_id}/gps-points")
async def add_gps_point(
    record_id: int, 
    gps_data: GPSPointCreate, 
    db: Session = Depends(get_db)
):
    """
    러닝 세션에 GPS 위치점 추가
    """
    # 러닝 기록 존재 확인
    running_record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not running_record:
        raise HTTPException(status_code=404, detail="러닝 기록을 찾을 수 없습니다.")
    
    # GPS 포인트 생성
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

# GPS 위치점 여러 개 추가 (배치)
@router.post("/{record_id}/gps-points/batch")
async def add_gps_points_batch(
    record_id: int, 
    gps_data_list: list[GPSPointCreate], 
    db: Session = Depends(get_db)
):
    """
    러닝 세션에 여러 GPS 위치점 추가 (배치)
    """
    # 러닝 기록 존재 확인
    running_record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not running_record:
        raise HTTPException(status_code=404, detail="러닝 기록을 찾을 수 없습니다.")
    
    gps_points = []
    for gps_data in gps_data_list:
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
        gps_points.append(gps_point)
    
    db.add_all(gps_points)
    db.commit()
    
    return {"message": f"{len(gps_points)}개의 GPS 위치점이 추가되었습니다.", "count": len(gps_points)}

# GPS 경로 조회
@router.get("/{record_id}/gps-route")
async def get_gps_route(record_id: int, db: Session = Depends(get_db)):
    """
    러닝 기록의 전체 GPS 경로 조회 (모든 위치점)
    """
    # 러닝 기록 존재 확인
    running_record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not running_record:
        raise HTTPException(status_code=404, detail="러닝 기록을 찾을 수 없습니다.")
    
    # GPS 포인트 조회 (시간순)
    gps_points = db.query(GPSPoint).filter(
        GPSPoint.running_record_id == record_id
    ).order_by(GPSPoint.timestamp).all()
    
    if len(gps_points) < 2:
        raise HTTPException(status_code=400, detail="GPS 데이터가 충분하지 않습니다.")
    
    # 총 거리 계산
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

# GPS 포인트 조회
@router.get("/{record_id}/gps-points")
async def get_gps_points(
    record_id: int,
    skip: int = 0,
    limit: int = 100,
    db: Session = Depends(get_db)
):
    """
    러닝 기록의 GPS 포인트 조회 (페이징)
    """
    # 러닝 기록 존재 확인
    running_record = db.query(RunningRecord).filter(RunningRecord.id == record_id).first()
    if not running_record:
        raise HTTPException(status_code=404, detail="러닝 기록을 찾을 수 없습니다.")
    
    # GPS 포인트 조회
    gps_points = db.query(GPSPoint).filter(
        GPSPoint.running_record_id == record_id
    ).order_by(GPSPoint.timestamp).offset(skip).limit(limit).all()
    
    return [GPSPointResponse.model_validate(p) for p in gps_points]

# 전체 통계 요약
@router.get("/statistics/summary")
async def get_summary_statistics(db: Session = Depends(get_db)):
    pass

# 주간 통계
@router.get("/statistics/weekly")
async def get_weekly_statistics(db: Session = Depends(get_db)):
    pass

# 월간 통계
@router.get("/statistics/monthly")
async def get_monthly_statistics(db: Session = Depends(get_db)):
    pass
