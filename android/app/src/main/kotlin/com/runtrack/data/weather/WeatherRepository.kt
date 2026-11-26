package com.runtrack.data.weather

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

interface ForecastService {
    @GET("v1/forecast")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") fields: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,is_day",
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("wind_speed_unit") windUnit: String = "ms",
        @Query("precipitation_unit") rainUnit: String = "mm",
        @Query("timezone") timezone: String = "Asia/Seoul",
        @Query("forecast_days") days: Int = 1
    ): Response<ForecastResponse>
}

data class ForecastResponse(
    val current: ForecastCurrent? = null,
    @SerializedName("current_units") val units: ForecastUnits? = null,
    val timezone: String? = null
)

data class ForecastUnits(
    @SerializedName("temperature_2m") val temperature: String? = null,
    @SerializedName("apparent_temperature") val feelsLike: String? = null,
    @SerializedName("relative_humidity_2m") val humidity: String? = null,
    @SerializedName("wind_speed_10m") val wind: String? = null,
    val precipitation: String? = null
)

data class ForecastCurrent(
    val time: String? = null,
    val interval: Int? = null,
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("apparent_temperature") val feelsLike: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("wind_speed_10m") val wind: Double? = null,
    val precipitation: Double? = null,
    @SerializedName("weather_code") val code: Int? = null,
    @SerializedName("is_day") val isDay: Int? = null
)

/** Separate public client: RunTrack bearer tokens must never be sent to the weather provider. */
object WeatherClient {
    fun create(baseUrl: String = "https://api.open-meteo.com/"): ForecastService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(OkHttpClient.Builder().connectTimeout(7, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(ForecastService::class.java)
}

class WeatherFailure(message: String) : IOException(message)

class WeatherRepository(
    private val service: ForecastService = WeatherClient.create(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val cache = mutableMapOf<String, CurrentWeather>()
    private val mutex = Mutex()

    suspend fun get(city: WeatherCity, force: Boolean = false): CurrentWeather = mutex.withLock {
        val cached = cache[city.id]
        // Ten-minute cache; manual refresh is limited to once per 30 seconds per city.
        val age = cached?.let { now() - it.fetchedAtMillis }
        if (cached != null && age != null && age >= 0 && age < if (force) 30_000 else 600_000) return@withLock cached
        val response = service.current(city.latitude, city.longitude)
        if (!response.isSuccessful) throw WeatherFailure(if (response.code() == 429)
            "요청이 많아요. 잠시 후 다시 확인해 주세요." else "날씨 서비스에 연결하지 못했어요.")
        val body = response.body() ?: throw WeatherFailure("날씨 응답이 비어 있어요.")
        val current = body.current ?: throw WeatherFailure("날씨 정보가 아직 제공되지 않았어요.")
        if (body.timezone != "Asia/Seoul") throw WeatherFailure("날씨 시각 정보를 확인하지 못했어요.")
        val validAt = runCatching { LocalDateTime.parse(current.time).atZone(ZoneId.of("Asia/Seoul")).toInstant() }
            .getOrElse { throw WeatherFailure("날씨 시각 정보를 확인하지 못했어요.") }
        val units = body.units
        val result = CurrentWeather(city, validAt,
            current.temperature?.finite()?.takeIf { units?.temperature == "°C" },
            current.feelsLike?.finite()?.takeIf { units?.feelsLike == "°C" },
            current.humidity?.takeIf { units?.humidity == "%" && it in 0..100 },
            current.wind?.finite()?.takeIf { units?.wind == "m/s" && it >= 0 },
            current.precipitation?.finite()?.takeIf { units?.precipitation == "mm" && it >= 0 },
            current.code, when (current.isDay) { 0 -> false; 1 -> true; else -> null },
            current.interval?.takeIf { it > 0 }, now())
        if (result.temperatureC == null && result.humidityPercent == null && result.weatherCode == null)
            throw WeatherFailure("표시할 날씨 정보가 없어요.")
        cache[city.id] = result
        result
    }
}

private fun Double.finite() = takeIf { isFinite() }
