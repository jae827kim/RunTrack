package com.runtrack.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.runtrack.MainActivity
import com.runtrack.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

/**
 * GPS 위치 추적 Foreground Service
 * 백그라운드에서 실시간 GPS 위치를 수집하고 저장합니다.
 */
class LocationTrackingService : Service() {
    
    companion object {
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_RUNNING_RECORD_ID = "running_record_id"
    }
    
    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isTracking = false
    private var currentRunningRecordId: Int? = null
    
    // GPS 콜백 리스너
    private var locationListener: ((latitude: Double, longitude: Double, altitude: Double?, speed: Float?, accuracy: Float?) -> Unit)? = null
    
    // 수집된 위치 데이터 (배치 업로드용)
    private val locationBuffer = mutableListOf<LocationPoint>()
    private val BUFFER_SIZE = 20  // 20개씩 모아서 업로드
    
    data class LocationPoint(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val speed: Float?,
        val accuracy: Float?,
        val bearing: Float?,
        val timestamp: Long
    )
    
    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.d("LocationTrackingService onCreate")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("LocationTrackingService onStartCommand")
        
        when (intent?.action) {
            ACTION_START -> {
                currentRunningRecordId = intent.getIntExtra(EXTRA_RUNNING_RECORD_ID, -1)
                if (currentRunningRecordId != -1) {
                    startLocationTracking()
                }
            }
            ACTION_STOP -> {
                stopLocationTracking()
            }
        }
        
        // 포어그라운드 알림 표시
        startForeground(NOTIFICATION_ID, createNotification())
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                
                for (location in locationResult.locations) {
                    Timber.d(
                        "위치 수신: Lat=${location.latitude}, Lon=${location.longitude}, " +
                        "Alt=${location.altitude}, Speed=${location.speed}, Accuracy=${location.accuracy}"
                    )
                    
                    // 콜백 리스너에 전달
                    locationListener?.invoke(
                        location.latitude,
                        location.longitude,
                        if (location.hasAltitude()) location.altitude else null,
                        if (location.hasSpeed()) location.speed else null,
                        location.accuracy
                    )
                    
                    // 버퍼에 추가
                    addLocationToBuffer(location)
                }
            }
        }
    }
    
    private fun addLocationToBuffer(location: android.location.Location) {
        locationBuffer.add(
            LocationPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = if (location.hasAltitude()) location.altitude else null,
                speed = if (location.hasSpeed()) location.speed else null,
                accuracy = location.accuracy,
                bearing = if (location.hasBearing()) location.bearing else null,
                timestamp = location.time
            )
        )
        
        // 버퍼가 가득 차면 업로드
        if (locationBuffer.size >= BUFFER_SIZE) {
            uploadLocationBatch()
        }
    }
    
    private fun uploadLocationBatch() {
        if (locationBuffer.isEmpty() || currentRunningRecordId == null) return
        
        Timber.d("위치 데이터 배치 업로드: ${locationBuffer.size}개 포인트")
        
        // 코루틴에서 API 호출
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // TODO: Retrofit을 통해 API에 업로드
                // val response = apiService.addGpsPointsBatch(
                //     runningRecordId = currentRunningRecordId!!,
                //     gpsPoints = locationBuffer.map { ... }
                // )
                
                locationBuffer.clear()
                Timber.d("위치 데이터 업로드 완료")
            } catch (e: Exception) {
                Timber.e(e, "위치 데이터 업로드 실패")
            }
        }
    }
    
    private fun startLocationTracking() {
        if (isTracking) return
        
        Timber.d("GPS 추적 시작")
        isTracking = true
        
        // 위치 요청 설정
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(5f)  // 5미터 이동 또는
            .setMaxUpdateDelayMillis(2000L)  // 최대 2초마다 업데이트
            .build()
        
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                
                Timber.d("위치 업데이트 요청 성공")
            } else {
                Timber.e("위치 권한 없음")
            }
        } catch (e: Exception) {
            Timber.e(e, "위치 추적 시작 실패")
        }
    }
    
    private fun stopLocationTracking() {
        if (!isTracking) return
        
        Timber.d("GPS 추적 중지")
        isTracking = false
        
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        // 남은 데이터 업로드
        uploadLocationBatch()
        
        // 서비스 종료
        stopSelf()
    }
    
    fun setLocationListener(listener: (latitude: Double, longitude: Double, altitude: Double?, speed: Float?, accuracy: Float?) -> Unit) {
        locationListener = listener
    }
    
    fun getCurrentRunningRecordId(): Int? = currentRunningRecordId
    
    fun isTrackingActive(): Boolean = isTracking
    
    private fun createNotification(): NotificationCompat.Notification {
        // Notification Channel 생성 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS 위치 추적",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "러닝 중 GPS 위치를 추적합니다."
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RunTrack 위치 추적")
            .setContentText("러닝 중 GPS 위치를 추적합니다.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
