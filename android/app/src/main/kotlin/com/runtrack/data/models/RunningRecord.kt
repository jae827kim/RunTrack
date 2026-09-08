package com.runtrack.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RunningRecord(
    val id: Int,
    val userId: Int,
    val shoeId: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val distanceKm: Float,
    val durationMinutes: Int,
    val startTime: String,
    val endTime: String,
    val avgPaceMinPerKm: Float? = null,
    val maxSpeedKmh: Float? = null,
    val avgSpeedKmh: Float? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val caloriesBurned: Float? = null,
    val elevationGainM: Float? = null,
    val elevationLossM: Float? = null,
    val maxAltitudeM: Float? = null,
    val gpsRoute: String? = null,  // GeoJSON 또는 Polyline
    val temperatureC: Float? = null,
    val humidityPercent: Int? = null,
    val windSpeedKmh: Float? = null,
    val routeType: String? = null,
    val surfaceType: String? = null,
    val difficulty: String? = null,
    val feeling: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class RunningRecordCreateRequest(
    val title: String? = null,
    val description: String? = null,
    val distanceKm: Float,
    val durationMinutes: Int,
    val startTime: String,
    val endTime: String,
    val shoeId: Int? = null,
    val avgPaceMinPerKm: Float? = null,
    val maxSpeedKmh: Float? = null,
    val avgSpeedKmh: Float? = null,
    val avgHeartRate: Int? = null,
    val caloriesBurned: Float? = null,
    val elevationGainM: Float? = null,
    val temperatureC: Float? = null,
    val feeling: String? = null,
    val notes: String? = null,
)

@Serializable
data class RunningStatistics(
    val totalDistanceKm: Float,
    val totalRuns: Int,
    val avgDistanceKm: Float,
    val avgPaceMinPerKm: Float,
    val totalDurationMinutes: Int,
    val avgDurationMinutes: Int,
    val totalCalories: Float,
    val avgHeartRate: Float? = null,
    val avgElevationGainM: Float? = null,
)
