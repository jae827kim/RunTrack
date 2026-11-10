"""Add only running_sessions to an existing database; never alter existing tables.

Run from backend: python -m app.migrate_running_sessions
"""
from app.database import engine
from app.models import RunningSession


def create_session_table(bind):
    RunningSession.__table__.create(bind=bind, checkfirst=True)


if __name__ == "__main__":
    create_session_table(engine)
    print("running_sessions table is ready")
