"""Persisted sessions, user-owned records and atomic shoe totals."""
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from hashlib import sha256
from math import isfinite
from uuid import uuid4

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import User, Shoe, RunningRecord, RunningSession
from app.schemas.running_record import (
    RunningRecordCreate, RunningRecordUpdate, RunningStatistics,
    RunningSessionStart, RunningSessionResponse, RunningSessionEnd,
)


class RunningService:
    def __init__(self, db: Session, current_user: User):
        self.db = db
        self.user_id = current_user.id

    def start(self, request: RunningSessionStart):
        try:
            self._lock_user()
            active = self.db.scalar(select(RunningSession).where(
                RunningSession.user_id == self.user_id,
                RunningSession.completed_at.is_(None),
            ))
            if active is not None:
                if (("shoe_id" in request.model_fields_set and request.shoe_id != active.shoe_id)
                        or (request.start_time is not None and request.start_time != active.start_time)):
                    raise HTTPException(409, "A different running session is already active")
                result = RunningSessionResponse(
                    session_id=active.id, start_time=active.start_time, shoe_id=active.shoe_id,
                )
                self.db.commit()
                return result
            self._shoes(request.shoe_id)
            session = RunningSession(
                id=str(uuid4()), user_id=self.user_id, shoe_id=request.shoe_id,
                start_time=request.start_time or datetime.now(timezone.utc).replace(tzinfo=None),
            )
            self.db.add(session)
            self.db.commit()
            self.db.refresh(session)
            return RunningSessionResponse(
                session_id=session.id, start_time=session.start_time, shoe_id=session.shoe_id,
            )
        except Exception:
            self.db.rollback()
            raise

    def end(self, session_id: str, request: RunningSessionEnd):
        try:
            self._lock_user()
            session = self.db.scalar(select(RunningSession).where(
                RunningSession.id == session_id, RunningSession.user_id == self.user_id,
            ).with_for_update())
            if session is None:
                raise HTTPException(404, "Running session not found")
            fingerprint = sha256(request.model_dump_json().encode("utf-8")).hexdigest()
            if session.completed_at is not None:
                if session.record_id is None:
                    raise HTTPException(410, "Completed running record was deleted")
                if fingerprint != session.completion_hash:
                    raise HTTPException(409, "Session already ended with different data")
                record = self.get_record(session.record_id)
                self.db.commit()
                return record
            if request.end_time <= session.start_time:
                raise HTTPException(422, "end_time must be after start_time")
            shoes = self._shoes(session.shoe_id)
            record = RunningRecord(
                **request.model_dump(), user_id=self.user_id,
                shoe_id=session.shoe_id, start_time=session.start_time,
            )
            self._metrics(record)
            self.db.add(record)
            self.db.flush()
            if session.shoe_id is not None:
                self._adjust(shoes[session.shoe_id], record.distance_km, 1)
            session.record_id = record.id
            session.completed_at = datetime.now(timezone.utc).replace(tzinfo=None)
            session.completion_hash = fingerprint
            self.db.commit()
            self.db.refresh(record)
            return record
        except Exception:
            self.db.rollback()
            raise

    def list_records(self, skip: int, limit: int):
        return list(self.db.scalars(
            select(RunningRecord).where(RunningRecord.user_id == self.user_id)
            .order_by(RunningRecord.start_time.desc(), RunningRecord.id.desc())
            .offset(skip).limit(limit)
        ))

    def get_record(self, record_id: int):
        record = self.db.scalar(select(RunningRecord).where(
            RunningRecord.id == record_id, RunningRecord.user_id == self.user_id,
        ))
        if record is None:
            raise HTTPException(404, "Running record not found")
        return record

    def _lock_user(self):
        # Serialize writes for this owner on PostgreSQL, including different records.
        self.db.execute(select(User.id).where(User.id == self.user_id).with_for_update())

    def _shoes(self, *ids):
        shoes = {}
        for shoe_id in sorted({value for value in ids if value is not None}):
            shoe = self.db.scalar(select(Shoe).where(
                Shoe.id == shoe_id, Shoe.user_id == self.user_id,
            ).with_for_update().execution_options(populate_existing=True))
            if shoe is None:
                raise HTTPException(404, "Shoe not found")
            shoes[shoe_id] = shoe
        return shoes

    @staticmethod
    def _adjust(shoe, distance, count):
        total = (shoe.cumulative_km or 0.0) + distance
        runs = (shoe.run_count or 0) + count
        if not isfinite(total) or total < -1e-8 or runs < 0:
            raise HTTPException(409, "Shoe totals are inconsistent")
        shoe.cumulative_km = max(0.0, total)
        shoe.run_count = runs

    @staticmethod
    def _metrics(record):
        record.avg_pace_min_per_km = record.duration_minutes / record.distance_km
        record.avg_speed_kmh = record.distance_km * 60 / record.duration_minutes
        if not isfinite(record.avg_pace_min_per_km) or not isfinite(record.avg_speed_kmh):
            raise HTTPException(422, "Distance and duration exceed the supported numeric range")

    def create_record(self, request: RunningRecordCreate):
        try:
            self._lock_user()
            shoes = self._shoes(request.shoe_id)
            record = RunningRecord(**request.model_dump(), user_id=self.user_id)
            self._metrics(record)
            self.db.add(record)
            if record.shoe_id is not None:
                self._adjust(shoes[record.shoe_id], record.distance_km, 1)
            self.db.commit()
            self.db.refresh(record)
            return record
        except Exception:
            self.db.rollback()
            raise

    def update_record(self, record_id: int, request: RunningRecordUpdate):
        try:
            self._lock_user()
            record = self.get_record(record_id)
            values = request.model_dump(exclude_unset=True)
            start = values.get("start_time", record.start_time)
            end = values.get("end_time", record.end_time)
            if end <= start:
                raise HTTPException(422, "end_time must be after start_time")
            old_shoe_id, old_distance = record.shoe_id, record.distance_km
            new_shoe_id = values.get("shoe_id", old_shoe_id)
            shoes = self._shoes(old_shoe_id, new_shoe_id)
            for name, value in values.items():
                setattr(record, name, value)
            self._metrics(record)
            if old_shoe_id is not None:
                self._adjust(shoes[old_shoe_id], -old_distance, -1)
            if new_shoe_id is not None:
                self._adjust(shoes[new_shoe_id], record.distance_km, 1)
            self.db.commit()
            self.db.refresh(record)
            return record
        except Exception:
            self.db.rollback()
            raise

    def delete_record(self, record_id: int):
        try:
            self._lock_user()
            record = self.get_record(record_id)
            shoes = self._shoes(record.shoe_id)
            if record.shoe_id is not None:
                self._adjust(shoes[record.shoe_id], -record.distance_km, -1)
            # Retain the completion tombstone even on SQLite without FK enforcement.
            for session in self.db.scalars(select(RunningSession).where(
                RunningSession.record_id == record.id,
                RunningSession.user_id == self.user_id,
            )):
                session.record_id = None
            self.db.flush()
            self.db.delete(record)
            self.db.commit()
        except Exception:
            self.db.rollback()
            raise

    def _records(self):
        return list(self.db.scalars(select(RunningRecord).where(
            RunningRecord.user_id == self.user_id,
        ).order_by(RunningRecord.start_time, RunningRecord.id)))

    @staticmethod
    def _statistics(records):
        distance = sum(record.distance_km for record in records)
        duration = sum(record.duration_minutes for record in records)
        count = len(records)
        heart_rates = [r.avg_heart_rate for r in records if r.avg_heart_rate is not None]
        elevations = [r.elevation_gain_m for r in records if r.elevation_gain_m is not None]
        return RunningStatistics(
            total_distance_km=distance, total_runs=count,
            avg_distance_km=distance / count if count else 0,
            avg_pace_min_per_km=duration / distance if distance else 0,
            total_duration_minutes=duration,
            avg_duration_minutes=duration / count if count else 0,
            total_calories=sum(r.calories_burned or 0 for r in records),
            avg_heart_rate=sum(heart_rates) / len(heart_rates) if heart_rates else None,
            avg_elevation_gain_m=sum(elevations) / len(elevations) if elevations else None,
        )

    def summary(self):
        return self._statistics(self._records())

    def weekly(self):
        groups = defaultdict(list)
        for record in self._records():
            local_date = (record.start_time + timedelta(hours=9)).date()
            monday = local_date - timedelta(days=local_date.weekday())
            groups[monday.isoformat()].append(record)
        return {key: self._statistics(records) for key, records in groups.items()}

    def monthly(self):
        groups = defaultdict(list)
        for record in self._records():
            key = (record.start_time + timedelta(hours=9)).strftime("%Y-%m")
            groups[key].append(record)
        return {key: self._statistics(records) for key, records in groups.items()}
