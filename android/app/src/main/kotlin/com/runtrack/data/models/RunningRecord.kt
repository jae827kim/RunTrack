package com.runtrack.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RunningRecord(
    // TODO: PM을 참고하여 러닝 기록 모델 단연 정의
)
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
