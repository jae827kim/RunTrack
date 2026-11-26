"""Persist in-progress runs without relaxing completed-record constraints."""
from sqlalchemy import Column, DateTime, ForeignKey, Index, Integer, String, text

from app.database import Base


class RunningSession(Base):
    __tablename__ = "running_sessions"

    id = Column(String(36), primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    shoe_id = Column(Integer, ForeignKey("shoes.id", ondelete="SET NULL"))
    start_time = Column(DateTime, nullable=False)
    record_id = Column(Integer, ForeignKey("running_records.id", ondelete="SET NULL"), unique=True)
    completed_at = Column(DateTime)
    completion_hash = Column(String(64))

    __table_args__ = (
        Index("uq_running_sessions_active_user", "user_id", unique=True,
              postgresql_where=text("completed_at IS NULL"),
              sqlite_where=text("completed_at IS NULL")),
    )
