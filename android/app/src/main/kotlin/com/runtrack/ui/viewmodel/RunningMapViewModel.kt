package com.runtrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import com.runtrack.ui.screens.LocationPoint
import java.time.LocalDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class RunningUIState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentLocation: LocationPoint? = null,
    val locationHistory: List<LocationPoint> = emptyList(),
    val totalDistance: Double = 0.0,
    val elapsedTime: Int = 0, // seconds
    val avgPace: String = "0:00",
    val currentSpeed: Float = 0f,
    val currentAltitude: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 러닝 지도 화면 ViewModel
 * GPS 위치 추적, 거리 계산, 통계 관리
 */
class RunningMapViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(RunningUIState())
    val uiState = _uiState.asStateFlow()
    
    private var runningRecordId: Int? = null
    private var startTime: Long? = null
    private var pausedTime: Long? = null
    
    // 시간 추적용
    private var totalPausedDuration: Long = 0
    
    fun startRunning(userId: Int, shoeId: Int? = null) {
        Timber.d("러닝 시작")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // TODO: API를 통해 러닝 세션 생성
                // val response = apiService.startRunning(userId, shoeId)
                // runningRecordId = response.id
                
                _uiState.value = _uiState.value.copy(
                    isRunning = true,
                    isPaused = false,
                    isLoading = false,
                    locationHistory = emptyList(),
                    totalDistance = 0.0,
                    elapsedTime = 0
                )
                
                startTime = System.currentTimeMillis()
                totalPausedDuration = 0
            } catch (e: Exception) {
                Timber.e(e, "러닝 시작 실패")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "러닝 시작 실패"
                )
            }
        }
    }
    
    fun stopRunning() {
        Timber.d("러닝 종료")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                if (runningRecordId != null) {
                    // TODO: API를 통해 러닝 세션 종료
                    // val response = apiService.endRunning(runningRecordId!!)
                }
                
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    isPaused = false,
                    isLoading = false
                )
                
                startTime = null
                pausedTime = null
                runningRecordId = null
            } catch (e: Exception) {
                Timber.e(e, "러닝 종료 실패")
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "러닝 종료 실패"
                )
            }
        }
    }
    
    fun pauseRunning() {
        Timber.d("러닝 일시 중지")
        val isPaused = !_uiState.value.isPaused
        
        if (isPaused) {
            pausedTime = System.currentTimeMillis()
        } else if (pausedTime != null) {
            totalPausedDuration += System.currentTimeMillis() - pausedTime!!
            pausedTime = null
        }
        
        _uiState.value = _uiState.value.copy(isPaused = isPaused)
    }
    
    fun updateLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        speed: Float? = null,
        accuracy: Float? = null,
        bearing: Float? = null
    ) {
        if (!_uiState.value.isRunning || _uiState.value.isPaused) return
        
        val newLocation = LocationPoint(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            speed = speed,
            accuracy = accuracy,
            bearing = bearing,
            timestamp = System.currentTimeMillis()
        )
        
        val currentState = _uiState.value
        val updatedHistory = currentState.locationHistory.toMutableList().apply {
            add(newLocation)
        }
        
        // 거리 계산
        val totalDistance = calculateTotalDistance(updatedHistory)
        
        // 평균 페이스 계산
        val avgPace = calculateAveragePace(totalDistance, currentState.elapsedTime)
        
        _uiState.value = currentState.copy(
            currentLocation = newLocation,
            locationHistory = updatedHistory,
            totalDistance = totalDistance,
            currentSpeed = speed ?: 0f,
            currentAltitude = altitude ?: 0.0,
            avgPace = avgPace
        )
        
        // 백엔드에 위치 전송
        if (runningRecordId != null) {
            sendLocationToBackend(newLocation)
        }
    }
    
    private fun sendLocationToBackend(location: LocationPoint) {
        viewModelScope.launch {
            try {
                // TODO: GPS 포인트 API에 업로드
                // val gpsData = GPSPointCreate(
                //     latitude = location.latitude,
                //     longitude = location.longitude,
                //     altitude_m = location.altitude,
                //     speed_kmh = location.speed,
                //     accuracy_m = location.accuracy,
                //     bearing_degrees = location.bearing,
                //     timestamp = Instant.ofEpochMilli(location.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
                // )
                // apiService.addGpsPoint(runningRecordId!!, gpsData)
            } catch (e: Exception) {
                Timber.e(e, "위치 전송 실패")
            }
        }
    }
    
    fun updateElapsedTime(seconds: Int) {
        _uiState.value = _uiState.value.copy(
            elapsedTime = seconds
        )
        
        val avgPace = calculateAveragePace(_uiState.value.totalDistance, seconds)
        _uiState.value = _uiState.value.copy(avgPace = avgPace)
    }
    
    private fun calculateTotalDistance(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        
        var distance = 0.0
        for (i in 0 until points.size - 1) {
            val lat1 = Math.toRadians(points[i].latitude)
            val lon1 = Math.toRadians(points[i].longitude)
            val lat2 = Math.toRadians(points[i + 1].latitude)
            val lon2 = Math.toRadians(points[i + 1].longitude)
            
            val dLat = lat2 - lat1
            val dLon = lon2 - lon1
            
            val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            
            distance += 6371 * c // 지구 반지름 (km)
        }
        
        return distance
    }
    
    private fun calculateAveragePace(distanceKm: Double, secondsElapsed: Int): String {
        if (distanceKm == 0.0 || secondsElapsed == 0) return "0:00"
        
        val minutesPerKm = (secondsElapsed / 60.0) / distanceKm
        val minutes = minutesPerKm.toInt()
        val seconds = ((minutesPerKm - minutes) * 60).toInt()
        
        return String.format("%d:%02d", minutes, seconds)
    }
    
    fun getRunningRecord() {
        if (runningRecordId == null) return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // TODO: API를 통해 러닝 기록 조회
                // val record = apiService.getRunningRecord(runningRecordId!!)
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Timber.e(e, "러닝 기록 조회 실패")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "러닝 기록 조회 실패"
                )
            }
        }
    }
}
