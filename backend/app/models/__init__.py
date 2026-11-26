"""Models __init__"""
from app.models.user import User
from app.models.shoe import Shoe
from app.models.running_record import RunningRecord
from app.models.gps_point import GPSPoint

__all__ = ["User", "Shoe", "RunningRecord", "GPSPoint"]
