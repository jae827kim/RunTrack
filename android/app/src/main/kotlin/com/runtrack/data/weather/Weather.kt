package com.runtrack.data.weather

import java.time.Instant

data class WeatherCity(val id: String, val name: String, val latitude: Double, val longitude: Double)

/** Public city-centre coordinates, never a device location. */
val weatherCities = listOf(
    WeatherCity("seoul", "서울", 37.5665, 126.9780),
    WeatherCity("busan", "부산", 35.1796, 129.0756),
    WeatherCity("daegu", "대구", 35.8714, 128.6014),
    WeatherCity("incheon", "인천", 37.4563, 126.7052),
    WeatherCity("gwangju", "광주", 35.1595, 126.8526),
    WeatherCity("daejeon", "대전", 36.3504, 127.3845),
    WeatherCity("ulsan", "울산", 35.5384, 129.3114),
    WeatherCity("sejong", "세종", 36.4800, 127.2890),
    WeatherCity("suwon", "수원", 37.2636, 127.0286),
    WeatherCity("chuncheon", "춘천", 37.8813, 127.7300),
    WeatherCity("gangneung", "강릉", 37.7519, 128.8761),
    WeatherCity("cheongju", "청주", 36.6424, 127.4890),
    WeatherCity("jeonju", "전주", 35.8242, 127.1480),
    WeatherCity("mokpo", "목포", 34.8118, 126.3922),
    WeatherCity("andong", "안동", 36.5684, 128.7294),
    WeatherCity("changwon", "창원", 35.2280, 128.6811),
    WeatherCity("jeju", "제주", 33.4996, 126.5312)
)

data class CurrentWeather(
    val city: WeatherCity,
    val validAt: Instant,
    val temperatureC: Double?,
    val feelsLikeC: Double?,
    val humidityPercent: Int?,
    val windMetresPerSecond: Double?,
    val precipitationMm: Double?,
    val weatherCode: Int?,
    val isDay: Boolean?,
    val intervalSeconds: Int?,
    val fetchedAtMillis: Long
) {
    fun isOld(nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis - validAt.toEpochMilli() > 2 * 60 * 60 * 1000L
}

fun weatherDescription(code: Int?): String = when (code) {
    0 -> "맑음"; 1 -> "대체로 맑음"; 2 -> "구름 조금"; 3 -> "흐림"
    45, 48 -> "안개"; 51, 53, 55 -> "이슬비"; 56, 57 -> "어는 이슬비"
    61, 63, 65 -> "비"; 66, 67 -> "어는 비"; 71, 73, 75 -> "눈"; 77 -> "눈 알갱이"
    80, 81, 82 -> "소나기"; 85, 86 -> "눈 소나기"; 95 -> "뇌우"; 96, 99 -> "우박 동반 뇌우"
    else -> "날씨 상태 정보 없음"
}
