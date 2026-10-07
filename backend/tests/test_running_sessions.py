"""README start/end lifecycle and atomic, repeatable finalization."""
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import create_engine, func, inspect, select
from sqlalchemy.exc import IntegrityError, SQLAlchemyError

from app.models import User, RunningSession, RunningRecord
from app.database import Base
from app.migrate_running_sessions import create_session_table
from app.schemas.running_record import RunningSessionStart, RunningSessionEnd
from app.services.running import RunningService
from tests.test_running import runs, totals  # shared isolated DB/owner/shoes fixture


START = {"start_time": "2026-10-08T09:00:00+09:00"}
END = {"end_time": "2026-10-08T09:30:00+09:00", "distance_km": 5, "duration_minutes": 30}


def test_readme_seven_endpoints_and_retries(runs):
    client, factory, headers, _, shoes, _ = runs
    request = {**START, "shoe_id": shoes[0]}
    start = client.post("/api/running/start", json=request, headers=headers)
    assert start.status_code == 200
    session = start.json()
    assert session["status"] == "active"
    assert session["start_time"] == "2026-10-08T00:00:00"
    assert client.post("/api/running/start", json=request, headers=headers).json() == session
    assert client.get("/api/running", headers=headers).json() == []
    assert client.get("/api/running/statistics/summary", headers=headers).json()["total_runs"] == 0
    assert totals(factory, shoes[0]) == (0, 0)
    path = f"/api/running/{session['session_id']}/end"
    ended = client.post(path, json=END, headers=headers)
    assert ended.status_code == 200
    record = ended.json()
    assert record["shoe_id"] == shoes[0]
    assert record["avg_pace_min_per_km"] == 6
    assert record["avg_speed_kmh"] == 10
    assert client.post(path, json=END, headers=headers).json() == record
    assert client.post(path, json={**END, "end_time": "2026-10-08T00:30:00Z"}, headers=headers).json() == record
    assert client.post(path, json={**END, "distance_km": 6}, headers=headers).status_code == 409
    assert totals(factory, shoes[0]) == (5, 1)
    assert client.get("/api/running", headers=headers).json() == [record]
    assert client.get(f"/api/running/{record['id']}", headers=headers).json() == record
    assert client.get("/api/running/statistics/summary", headers=headers).json()["total_runs"] == 1
    assert client.get("/api/running/statistics/weekly", headers=headers).json()["2026-10-05"]["total_runs"] == 1
    assert client.get("/api/running/statistics/monthly", headers=headers).json()["2026-10"]["total_runs"] == 1
    with factory() as db:
        saved = db.get(RunningSession, session["session_id"])
        assert saved.completed_at is not None and saved.record_id == record["id"]
        assert db.scalar(select(func.count()).select_from(RunningRecord)) == 1
    next_run = client.post("/api/running/start", headers=headers)
    assert next_run.status_code == 200
    assert next_run.json()["session_id"] != session["session_id"]


def test_default_start_is_persistent_and_recovers_without_body(runs):
    client, factory, headers, _, _, _ = runs
    before = datetime.now(timezone.utc).replace(tzinfo=None)
    response = client.post("/api/running/start", headers=headers)
    assert response.status_code == 200
    session = response.json()
    with factory() as db:
        stored = db.get(RunningSession, session["session_id"])
        assert stored.start_time >= before and stored.shoe_id is None
    assert client.post("/api/running/start", headers=headers).json() == session
    end_time = (datetime.fromisoformat(session["start_time"]) + timedelta(minutes=30)).isoformat()
    ended = client.post(f"/api/running/{session['session_id']}/end", json={**END, "end_time": end_time}, headers=headers)
    assert ended.status_code == 200 and ended.json()["shoe_id"] is None


def test_active_session_conflict_and_owner_checks(runs):
    client, factory, headers, other, shoes, _ = runs
    for shoe_id in (shoes[2], 2147483647):
        assert client.post("/api/running/start", json={**START, "shoe_id": shoe_id}, headers=headers).status_code == 404
    session = client.post("/api/running/start", json={**START, "shoe_id": shoes[0]}, headers=headers).json()
    assert client.post("/api/running/start", headers=headers).json() == session
    assert client.post("/api/running/start", json={**START, "shoe_id": shoes[1]}, headers=headers).status_code == 409
    path = f"/api/running/{session['session_id']}/end"
    assert client.post(path, json=END, headers=other).status_code == 404
    assert client.post("/api/running/00000000-0000-0000-0000-000000000001/end", json=END, headers=headers).status_code == 404
    assert totals(factory, shoes[0]) == (0, 0)
    assert client.post(path, json=END, headers=headers).status_code == 200


@pytest.mark.parametrize("payload", [
    {}, {**END, "distance_km": 0}, {**END, "duration_minutes": 0},
    {**END, "end_time": START["start_time"]}, {**END, "user_id": 2},
    {**END, "shoe_id": 1}, {**END, "distance_km": "NaN"},
])
def test_invalid_end_leaves_session_active(runs, payload):
    client, factory, headers, _, shoes, _ = runs
    session = client.post("/api/running/start", json={**START, "shoe_id": shoes[0]}, headers=headers).json()
    response = client.post(f"/api/running/{session['session_id']}/end", json=payload, headers=headers)
    assert response.status_code == 422
    with factory() as db:
        assert db.get(RunningSession, session["session_id"]).completed_at is None
        assert db.scalar(select(func.count()).select_from(RunningRecord)) == 0
    assert totals(factory, shoes[0]) == (0, 0)


@pytest.mark.parametrize("operation", ["start", "end"])
def test_session_failure_rolls_back_everything(runs, monkeypatch, operation):
    client, factory, headers, _, shoes, owner = runs
    session_id = None
    if operation == "end":
        session_id = client.post("/api/running/start", json={**START, "shoe_id": shoes[0]}, headers=headers).json()["session_id"]
    with factory() as db:
        service = RunningService(db, db.get(User, owner))

        def fail_commit():
            db.flush()
            raise SQLAlchemyError("injected failure")

        monkeypatch.setattr(db, "commit", fail_commit)
        with pytest.raises(SQLAlchemyError):
            if operation == "start":
                service.start(RunningSessionStart(**START, shoe_id=shoes[0]))
            else:
                service.end(session_id, RunningSessionEnd(**END))
    with factory() as db:
        assert db.scalar(select(func.count()).select_from(RunningRecord)) == 0
        if session_id:
            saved = db.get(RunningSession, session_id)
            assert saved.completed_at is None and saved.record_id is None
        else:
            assert db.scalar(select(func.count()).select_from(RunningSession)) == 0
    assert totals(factory, shoes[0]) == (0, 0)
    if session_id:
        assert client.post(f"/api/running/{session_id}/end", json=END, headers=headers).status_code == 200


def test_deleted_record_cannot_be_recreated_by_end_retry(runs):
    client, factory, headers, _, shoes, _ = runs
    session = client.post("/api/running/start", json={**START, "shoe_id": shoes[0]}, headers=headers).json()
    path = f"/api/running/{session['session_id']}/end"
    record = client.post(path, json=END, headers=headers).json()
    assert client.delete(f"/api/running/{record['id']}", headers=headers).status_code == 204
    assert client.post(path, json=END, headers=headers).status_code == 410
    assert totals(factory, shoes[0]) == (0, 0)


@pytest.mark.parametrize("payload", [
    {"shoe_id": 0}, {"start_time": "invalid"}, {"user_id": 1},
])
def test_start_validation(runs, payload):
    client, _, headers, _, _, _ = runs
    assert client.post("/api/running/start", json=payload, headers=headers).status_code == 422


def test_end_requires_uuid_and_positive_authenticated_payload(runs):
    client, _, headers, _, _, _ = runs
    assert client.post("/api/running/not-a-uuid/end", json=END, headers=headers).status_code == 422


def test_additive_migration_preserves_records_and_enforces_active_uniqueness():
    engine = create_engine("sqlite://")
    try:
        # Simulate an existing schema without the newly introduced session table.
        tables = [table for table in Base.metadata.sorted_tables if table.name != "running_sessions"]
        Base.metadata.create_all(engine, tables=tables)
        with engine.begin() as conn:
            conn.exec_driver_sql("PRAGMA foreign_keys=ON")
            conn.execute(User.__table__.insert().values(
                id=1, username="existing", email="existing@example.com", hashed_password="unused",
            ))
            conn.execute(RunningRecord.__table__.insert().values(
                id=1, user_id=1, distance_km=5, duration_minutes=30,
                start_time=datetime(2026, 10, 8, 0), end_time=datetime(2026, 10, 8, 0, 30),
            ))
        before = inspect(engine).get_columns("running_records")
        create_session_table(engine)
        create_session_table(engine)
        assert [c["name"] for c in inspect(engine).get_columns("running_records")] == [c["name"] for c in before]
        with engine.begin() as conn:
            assert conn.scalar(select(RunningRecord.distance_km).where(RunningRecord.id == 1)) == 5
            conn.execute(RunningSession.__table__.insert().values(
                id="existing-session", user_id=1, start_time=datetime(2026, 10, 8),
            ))
        with pytest.raises(IntegrityError), engine.begin() as conn:
            conn.execute(RunningSession.__table__.insert().values(
                id="second-active", user_id=1, start_time=datetime(2026, 10, 8),
            ))
    finally:
        engine.dispose()
