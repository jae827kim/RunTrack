package com.runtrack.data.api

import com.runtrack.data.models.*
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface UserService {
    @POST("api/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<User>

    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/users/me")
    suspend fun getProfile(@Header("Authorization") authorization: String? = null): Response<User>
}

interface ShoeService {
    @POST("api/shoes")
    suspend fun createShoe(@Body request: ShoeCreateRequest): Response<Shoe>

    @GET("api/shoes")
    suspend fun getShoes(): Response<List<Shoe>>

    @GET("api/shoes/{id}")
    suspend fun getShoe(@Path("id") id: Int): Response<Shoe>

    @PUT("api/shoes/{id}")
    suspend fun updateShoe(@Path("id") id: Int, @Body request: ShoeUpdateRequest): Response<Shoe>

    @DELETE("api/shoes/{id}")
    suspend fun deleteShoe(@Path("id") id: Int): Response<Unit>

    @GET("api/shoes/{id}/stats")
    suspend fun getShoeStats(@Path("id") id: Int): Response<ShoeStats>
}

data class RunningSessionEndRequest(
    @SerializedName("distance_km") val distanceKm: Float,
    @SerializedName("duration_minutes") val durationMinutes: Int,
    @SerializedName("end_time") val endTime: String? = null,
    val notes: String? = null,
)

interface RunningService {
    @POST("api/running")
    suspend fun createRecord(@Body request: RunningRecordCreateRequest): Response<RunningRecord>

    @PUT("api/running/{id}")
    suspend fun updateRecord(@Path("id") id: Int, @Body request: RunningRecordUpdateRequest): Response<RunningRecord>

    @DELETE("api/running/{id}")
    suspend fun deleteRecord(@Path("id") id: Int): Response<Unit>

    @POST("api/running/start")
    suspend fun startRunning(@Body request: RunningSessionStartRequest): Response<RunningSessionResponse>

    @POST("api/running/{id}/end")
    suspend fun endRunning(
        @Path("id") sessionId: String,
        @Body request: RunningSessionEndRequest,
    ): Response<RunningRecord>

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
    
    // GPS 관련 API
    @POST("api/running/{record_id}/gps-points")
    suspend fun addGpsPoint(
        @Path("record_id") recordId: Int,
        @Body gpsData: GPSPointCreate
    ): Response<GPSPointResponse>
    
    @POST("api/running/{record_id}/gps-points/batch")
    suspend fun addGpsPointsBatch(
        @Path("record_id") recordId: Int,
        @Body gpsDataList: List<GPSPointCreate>
    ): Response<Map<String, Any>>
    
    @GET("api/running/{record_id}/gps-route")
    suspend fun getGpsRoute(@Path("record_id") recordId: Int): Response<GPSRouteResponse>
    
    @GET("api/running/{record_id}/gps-points")
    suspend fun getGpsPoints(
        @Path("record_id") recordId: Int,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<GPSPointResponse>>
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

// GPS 관련 데이터 모델
data class GPSPointCreate(
    val latitude: Double,
    val longitude: Double,
    val altitude_m: Double? = null,
    val speed_kmh: Float? = null,
    val accuracy_m: Float? = null,
    val bearing_degrees: Float? = null,
    val timestamp: String
)

data class GPSPointResponse(
    val id: Int,
    val running_record_id: Int,
    val latitude: Double,
    val longitude: Double,
    val altitude_m: Double? = null,
    val speed_kmh: Float? = null,
    val accuracy_m: Float? = null,
    val bearing_degrees: Float? = null,
    val timestamp: String
)

data class GPSRouteResponse(
    val running_record_id: Int,
    val points: List<GPSPointResponse>,
    val total_distance_km: Double,
    val start_point: GPSPointResponse,
    val end_point: GPSPointResponse
)
