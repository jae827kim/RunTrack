package com.runtrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Float? = null,
    val accuracy: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * GPS 지도 화면 - 실시간 위치 추적 및 경로 표시
 */
@Composable
fun RunningMapScreen(
    modifier: Modifier = Modifier,
    onStartRunning: () -> Unit = {},
    onStopRunning: () -> Unit = {},
    onPauseRunning: () -> Unit = {}
) {
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LocationPoint?>(null) }
    val locationHistory = remember { mutableStateListOf<LocationPoint>() }
    
    var totalDistance by remember { mutableStateOf(0.0) }
    var elapsedTime by remember { mutableStateOf(0) }
    var avgPace by remember { mutableStateOf("0:00") }
    
    // 카메라 위치 상태
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(37.5665, 126.9780), // 서울 기본 위치
            15f
        )
    }
    
    // 위치 업데이트 시뮬레이션 (실제로는 LocationTrackingService에서 수신)
    LaunchedEffect(isRunning) {
        if (isRunning && !isPaused) {
            // TODO: LocationTrackingService로부터 위치 업데이트 수신
            // 현재는 시뮬레이션으로 표시
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true,
                compassEnabled = true
            )
        ) {
            // 경로 그리기 (Polyline)
            if (locationHistory.size >= 2) {
                val polylinePoints = locationHistory.map {
                    LatLng(it.latitude, it.longitude)
                }
                Polyline(
                    points = polylinePoints,
                    color = Color.Blue,
                    width = 5f
                )
            }
            
            // 현재 위치 마커
            currentLocation?.let {
                Marker(
                    state = MarkerState(
                        position = LatLng(it.latitude, it.longitude)
                    ),
                    title = "현재 위치",
                    snippet = "속도: ${it.speed?.let { s -> String.format("%.1f", s) } ?: "N/A"} km/h"
                )
            }
            
            // 시작 위치 마커
            locationHistory.firstOrNull()?.let {
                Marker(
                    state = MarkerState(
                        position = LatLng(it.latitude, it.longitude)
                    ),
                    title = "시작점",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                        .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN)
                )
            }
            
            // 종료 위치 마커
            if (!isRunning && locationHistory.isNotEmpty()) {
                locationHistory.lastOrNull()?.let {
                    Marker(
                        state = MarkerState(
                            position = LatLng(it.latitude, it.longitude)
                        ),
                        title = "종료점",
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                            .defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }
        }
        
        // 상단: 통계 정보
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .shadow(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 거리
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "거리",
                        value = String.format("%.2f", totalDistance),
                        unit = "km"
                    )
                    StatItem(
                        label = "시간",
                        value = formatTime(elapsedTime),
                        unit = ""
                    )
                    StatItem(
                        label = "페이스",
                        value = avgPace,
                        unit = "/km"
                    )
                }
                
                // 현재 속도 및 고도
                if (currentLocation != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "속도",
                            value = currentLocation!!.speed?.let { String.format("%.1f", it) } ?: "0",
                            unit = "km/h"
                        )
                        StatItem(
                            label = "고도",
                            value = currentLocation!!.altitude?.let { String.format("%.0f", it) } ?: "0",
                            unit = "m"
                        )
                        StatItem(
                            label = "정확도",
                            value = currentLocation!!.accuracy?.let { String.format("%.0f", it) } ?: "0",
                            unit = "m"
                        )
                    }
                }
            }
        }
        
        // 하단: 제어 버튼
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isRunning) {
                // 시작 버튼
                Button(
                    onClick = {
                        isRunning = true
                        isPaused = false
                        onStartRunning()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "시작",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("시작", color = Color.White)
                }
            } else {
                // 일시 중지 버튼
                Button(
                    onClick = {
                        isPaused = !isPaused
                        onPauseRunning()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107)
                    )
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "일시 중지",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(if (isPaused) "재개" else "일시 중지", color = Color.White)
                }
                
                // 종료 버튼
                Button(
                    onClick = {
                        isRunning = false
                        isPaused = false
                        onStopRunning()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "종료",
                        tint = Color.White,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("종료", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Light
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.End
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

fun calculateDistance(points: List<LocationPoint>): Double {
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
