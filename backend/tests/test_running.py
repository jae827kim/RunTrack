"""Completed-run persistence, ownership, totals and calendar statistics."""
import pytest
from sqlalchemy import select, func
from sqlalchemy.exc import SQLAlchemyError

from app.core.security import create_access_token
from app.models import User, Shoe, RunningRecord
from app.schemas.running_record import RunningRecordCreate, RunningRecordUpdate
from app.services.running import RunningService


RUN = {
    "distance_km": 5, "duration_minutes": 30,
    "start_time": "2026-10-08T09:00:00+09:00",
    "end_time": "2026-10-08T09:30:00+09:00", "notes": "morning run",
}


@pytest.fixture
def runs(auth_integration):
    client, factory = auth_integration
    with factory() as db:
        users = [User(username=name, email=name + "@example.com", hashed_password="unused")
                 for name in ("runner-owner", "runner-other")]
        db.add_all(users)
        db.flush()
        shoes = [Shoe(user_id=users[index].id, brand="Test", model=str(index))
                 for index in (0, 0, 1)]
        db.add_all(shoes)
        db.commit()
        owner, other = [u.id for u in users]
        ids = [s.id for s in shoes]
    headers = {"Authorization": "Bearer " + create_access_token(owner)[0]}
    other_headers = {"Authorization": "Bearer " + create_access_token(other)[0]}
    return client, factory, headers, other_headers, ids, owner


def totals(factory, shoe_id):
    with factory() as db:
        shoe = db.get(Shoe, shoe_id)
        return shoe.cumulative_km, shoe.run_count


def test_complete_record_lifecycle_and_shoe_transfer(runs):
    client, factory, headers, _, shoes, _ = runs
    created = client.post("/api/running", json={**RUN, "shoe_id": shoes[0],
                          "avg_pace_min_per_km": 99, "avg_speed_kmh": 99}, headers=headers)
    assert created.status_code == 201
    record = created.json()
    rid = record["id"]
    assert record["avg_pace_min_per_km"] == 6
    assert record["avg_speed_kmh"] == 10
    assert record["start_time"] == "2026-10-08T00:00:00"
    assert record["notes"] == RUN["notes"]
    assert totals(factory, shoes[0]) == (5, 1)
    assert client.get(f"/api/running/{rid}", headers=headers).json() == record
    assert client.get("/api/running", headers=headers).json() == [record]
    changed = client.put(f"/api/running/{rid}", headers=headers,
                         json={"distance_km": 10, "duration_minutes": 60})
    assert changed.status_code == 200
    assert changed.json()["notes"] == RUN["notes"]
    assert totals(factory, shoes[0]) == (10, 1)
    changed = client.put(f"/api/running/{rid}", headers=headers,
                         json={"shoe_id": shoes[1], "notes": None})
    assert changed.status_code == 200 and changed.json()["notes"] is None
    assert totals(factory, shoes[0]) == (0, 0)
    assert totals(factory, shoes[1]) == (10, 1)
    assert client.put(f"/api/running/{rid}", headers=headers, json={}).status_code == 200
    assert totals(factory, shoes[1]) == (10, 1)
    response = client.delete(f"/api/running/{rid}", headers=headers)
    assert response.status_code == 204 and response.content == b""
    assert totals(factory, shoes[1]) == (0, 0)
    assert client.get(f"/api/running/{rid}", headers=headers).status_code == 404
    assert client.delete(f"/api/running/{rid}", headers=headers).status_code == 404
    with factory() as db:
        assert db.scalar(select(func.count()).select_from(RunningRecord)) == 0


def test_unassigned_run_and_detach_shoe(runs):
    client, factory, headers, _, shoes, _ = runs
    record = client.post("/api/running", json=RUN, headers=headers).json()
    path = f"/api/running/{record['id']}"
    assert record["shoe_id"] is None
    assert client.put(path, json={"shoe_id": shoes[0]}, headers=headers).status_code == 200
    assert totals(factory, shoes[0]) == (5, 1)
    assert client.put(path, json={"shoe_id": None}, headers=headers).status_code == 200
    assert totals(factory, shoes[0]) == (0, 0)
    assert client.delete(path, headers=headers).status_code == 204


def test_owner_isolation_and_rejected_shoe_changes(runs):
    client, factory, headers, other, shoes, _ = runs
    record = client.post("/api/running", json={**RUN, "shoe_id": shoes[0]}, headers=headers).json()
    path = f"/api/running/{record['id']}"
    for method, data in (("GET", None), ("PUT", {"notes": "stolen"}), ("DELETE", None)):
        assert client.request(method, path, json=data, headers=other).status_code == 404
    assert client.get("/api/running", headers=other).json() == []
    assert client.get("/api/running/statistics/summary", headers=other).json()["total_runs"] == 0
    for shoe_id in (shoes[2], 2147483647):
        assert client.post("/api/running", json={**RUN, "shoe_id": shoe_id}, headers=headers).status_code == 404
        assert client.put(path, json={"shoe_id": shoe_id, "distance_km": 20}, headers=headers).status_code == 404
    assert totals(factory, shoes[0]) == (5, 1)
    assert totals(factory, shoes[2]) == (0, 0)
    assert client.get(path, headers=headers).json()["distance_km"] == 5


@pytest.mark.parametrize("changes", [
    {"distance_km": 0}, {"distance_km": -1}, {"distance_km": "NaN"},
    {"duration_minutes": 0}, {"duration_minutes": 1.5}, {"duration_minutes": 2147483648},
    {"end_time": RUN["start_time"]}, {"start_time": "invalid"}, {"shoe_id": 0},
    {"user_id": 2}, {"temperature_c": "Infinity"}, {"avg_heart_rate": -1},
    {"distance_km": 1e-320}, {"distance_km": 1e308},
])
def test_invalid_create_cannot_persist(runs, changes):
    client, factory, headers, _, _, _ = runs
    assert client.post("/api/running", json={**RUN, **changes}, headers=headers).status_code == 422
    with factory() as db:
        assert db.scalar(select(func.count()).select_from(RunningRecord)) == 0


@pytest.mark.parametrize("changes", [
    {"distance_km": None}, {"duration_minutes": None}, {"end_time": None},
    {"distance_km": "Infinity"}, {"end_time": "2026-10-07T00:00:00Z"},
    {"user_id": 2}, {"avg_pace_min_per_km": 99},
])
def test_invalid_update_preserves_record_and_shoe(runs, changes):
    client, factory, headers, _, shoes, _ = runs
    record = client.post("/api/running", json={**RUN, "shoe_id": shoes[0]}, headers=headers).json()
    path = f"/api/running/{record['id']}"
    assert client.put(path, json=changes, headers=headers).status_code == 422
    assert client.get(path, headers=headers).json() == record
    assert totals(factory, shoes[0]) == (5, 1)


def test_statistics_ordering_and_korean_calendar_boundaries(runs):
    client, _, headers, _, _, _ = runs
    empty = client.get("/api/running/statistics/summary", headers=headers).json()
    assert empty["total_runs"] == 0 and empty["avg_pace_min_per_km"] == 0
    assert empty["avg_heart_rate"] is None
    assert client.get("/api/running/statistics/weekly", headers=headers).json() == {}
    samples = [
        {**RUN, "start_time": "2026-08-31T14:00:00Z", "end_time": "2026-08-31T14:30:00Z"},
        {**RUN, "start_time": "2026-08-31T15:00:00Z", "end_time": "2026-08-31T16:31:00Z",
         "distance_km": 10, "duration_minutes": 91, "avg_heart_rate": 140},
        {**RUN, "start_time": "2026-09-06T15:00:00Z", "end_time": "2026-09-06T15:30:00Z"},
    ]
    ids = []
    for sample in samples:
        response = client.post("/api/running", json=sample, headers=headers)
        assert response.status_code == 201
        ids.append(response.json()["id"])
    stats = client.get("/api/running/statistics/summary", headers=headers).json()
    assert stats["total_runs"] == 3 and stats["total_distance_km"] == 20
    assert stats["avg_pace_min_per_km"] == pytest.approx(151 / 20)
    assert stats["avg_duration_minutes"] == pytest.approx(151 / 3)
    assert stats["avg_heart_rate"] == 140
    weekly = client.get("/api/running/statistics/weekly", headers=headers).json()
    assert weekly["2026-08-31"]["total_runs"] == 2
    assert weekly["2026-09-07"]["total_runs"] == 1
    monthly = client.get("/api/running/statistics/monthly", headers=headers).json()
    assert monthly["2026-08"]["total_runs"] == 1
    assert monthly["2026-09"]["total_runs"] == 2
    page = client.get("/api/running?skip=1&limit=1", headers=headers).json()
    assert [r["id"] for r in page] == [ids[1]]


@pytest.mark.parametrize("operation", ["create", "update", "delete"])
def test_transaction_failure_rolls_back_both_record_and_totals(runs, monkeypatch, operation):
    client, factory, headers, _, shoes, owner = runs
    record = client.post("/api/running", json={**RUN, "shoe_id": shoes[0]}, headers=headers).json()
    with factory() as db:
        service = RunningService(db, db.get(User, owner))

        def fail_commit():
            db.flush()  # Even changes already sent to the DB must be rolled back.
            raise SQLAlchemyError("injected failure")

        monkeypatch.setattr(db, "commit", fail_commit)
        with pytest.raises(SQLAlchemyError):
            if operation == "create":
                service.create_record(RunningRecordCreate(**RUN, shoe_id=shoes[0]))
            elif operation == "update":
                service.update_record(record["id"], RunningRecordUpdate(distance_km=20, shoe_id=shoes[1]))
            else:
                service.delete_record(record["id"])
    assert totals(factory, shoes[0]) == (5, 1)
    assert totals(factory, shoes[1]) == (0, 0)
    assert client.get(f"/api/running/{record['id']}", headers=headers).json() == record
    assert len(client.get("/api/running", headers=headers).json()) == 1


@pytest.mark.parametrize("method,path,payload", [
    ("POST", "/api/running", RUN), ("GET", "/api/running", None),
    ("GET", "/api/running/1", None), ("PUT", "/api/running/1", {}),
    ("DELETE", "/api/running/1", None),
    ("GET", "/api/running/statistics/summary", None),
    ("GET", "/api/running/statistics/weekly", None),
    ("GET", "/api/running/statistics/monthly", None),
])
def test_record_apis_require_auth(auth_integration, method, path, payload):
    client, _ = auth_integration
    assert client.request(method, path, json=payload).status_code == 401
