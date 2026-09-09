package com.runtrack.data.api

import com.runtrack.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface UserService {
    // TODO: 사용자 로그인/회원가입 API 정의
}

interface ShoeService {
    // TODO: 신발 CRUD API 정의
}

interface RunningService {
    // TODO: 러닝 기록 CRUD 및 통계 API 정의
}

interface WeatherService {
    // TODO: 기상 API 정의
}

interface RecommendationService {
    // TODO: AI 추천 API 정의
}

    @PUT("api/shoes/{id}")
    suspend fun updateShoe(
        @Path("id") id: Int,
        @Body request: ShoeUpdateRequest
    ): Response<Shoe>

    @DELETE("api/shoes/{id}")
    suspend fun deleteShoe(@Path("id") id: Int): Response<Unit>

    @GET("api/shoes/{id}/stats")
    suspend fun getShoeStats(@Path("id") id: Int): Response<ShoeStats>
}

interface RunningService {
    @POST("api/running/start")
    suspend fun startRunning(): Response<Map<String, String>>

    @POST("api/running/{id}/end")
    suspend fun endRunning(@Path("id") sessionId: String): Response<RunningRecord>

    @GET("api/running")
    suspend fun getRunningRecords(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 10
    ): Response<List<RunningRecord>>

    @GET("api/running/{id}")
    suspend fun getRunningRecord(@Path("id") id: Int): Response<RunningRecord>

    @GET("api/running/statistics/summary")
    suspend fun getSummaryStatistics(): Response<RunningStatistics>

    @GET("api/running/statistics/weekly")
    suspend fun getWeeklyStatistics(): Response<Map<String, RunningStatistics>>

    @GET("api/running/statistics/monthly")
    suspend fun getMonthlyStatistics(): Response<Map<String, RunningStatistics>>
}

interface WeatherService {
    @GET("api/weather/advice")
    suspend fun getRunningAdvice(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Response<WeatherAdvice>

    @GET("api/weather/history")
    suspend fun getWeatherHistory(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("days") days: Int = 7
    ): Response<WeatherHistory>
}

// 날씨 관련 응답 모델
data class WeatherAdvice(
    val latitude: Double,
    val longitude: Double,
    val temperature: Float,
    val perceivedTemperature: Float,
    val humidity: Int,
    val windSpeed: Float,
    val uvIndex: Int,
    val runningScore: Int,
    val condition: String,
    val tips: List<String>
)

data class WeatherHistory(
    val location: Location,
    val days: Int,
    val history: List<WeatherData>
)

data class Location(
    val latitude: Double,
    val longitude: Double
)

data class WeatherData(
    val date: String,
    val temperature: Float,
    val condition: String,
    val humidity: Int,
    val windSpeed: Float
)
