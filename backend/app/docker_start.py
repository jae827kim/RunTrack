"""Check dependencies and initialize only a completely empty development database."""

from redis import Redis, RedisError
from sqlalchemy import inspect, text
from sqlalchemy.exc import SQLAlchemyError

import app.models  # noqa: F401 - registers every model with Base
from app.config import settings
from app.database import Base, engine


def main():
    try:
        with engine.connect() as connection:
            connection.execute(text("SELECT 1"))
    except SQLAlchemyError:
        raise SystemExit("PostgreSQL connection failed; check db logs and DATABASE_URL") from None

    try:
        with Redis.from_url(settings.REDIS_URL, socket_connect_timeout=3, socket_timeout=3) as client:
            client.ping()
    except (RedisError, ValueError):
        raise SystemExit("Redis connection failed; check redis logs and REDIS_URL") from None

    try:
        existing = set(inspect(engine).get_table_names())
        expected = set(Base.metadata.tables)
        if not existing:
            Base.metadata.create_all(engine)
            print("Created tables in empty development database", flush=True)
        elif missing := expected - existing:
            names = ", ".join(sorted(missing))
            raise SystemExit(
                f"Database has existing tables but is missing: {names}. "
                "Back up the database and apply the existing migration manually."
            )
    except SQLAlchemyError:
        raise SystemExit("Database schema check or initialization failed; check db logs") from None

    print("PostgreSQL and Redis connections verified", flush=True)


if __name__ == "__main__":
    main()
