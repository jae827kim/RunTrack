package com.runtrack.data.weather

import com.runtrack.data.api.ApiSession
import com.runtrack.data.models.LoginResponse
import com.runtrack.data.models.User
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import java.time.Instant

class WeatherRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: WeatherRepository
    private var now = Instant.parse("2026-11-10T08:30:00Z").toEpochMilli()
    private val sample = """{"timezone":"Asia/Seoul","current_units":{"temperature_2m":"°C","apparent_temperature":"°C","relative_humidity_2m":"%","wind_speed_10m":"m/s","precipitation":"mm"},"current":{"time":"2026-11-10T17:30","interval":900,"temperature_2m":8.9,"apparent_temperature":6.6,"relative_humidity_2m":63,"wind_speed_10m":1.22,"precipitation":0.0,"weather_code":0,"is_day":0}}"""

    @Before fun setup() {
        ApiSession.clear(); server = MockWebServer(); server.start()
        repository = WeatherRepository(WeatherClient.create(server.url("/").toString())) { now }
    }
    @After fun close() { server.shutdown(); ApiSession.clear() }
    private fun reply(body: String = sample, code: Int = 200) { server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)) }

    @Test fun publicWeatherRequestNeverContainsTheRunTrackTokenAndUsesExplicitUnits() = runBlocking {
        ApiSession.start(LoginResponse("private-runtrack-token", "bearer", 1800), User(1, "runner", "r@example.test", createdAt = "", updatedAt = ""))
        reply()
        val data = repository.get(weatherCities.first())
        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertEquals("ms", request.requestUrl!!.queryParameter("wind_speed_unit"))
        assertEquals("Asia/Seoul", request.requestUrl!!.queryParameter("timezone"))
        assertEquals("37.5665", request.requestUrl!!.queryParameter("latitude"))
        assertEquals(8.9, data.temperatureC!!, 0.0)
        assertEquals(Instant.parse("2026-11-10T08:30:00Z"), data.validAt)
        assertEquals(false, data.isDay)
    }

    @Test fun cachesPerCityAndLimitsManualRefresh() = runBlocking {
        reply(); reply(); reply()
        val city = weatherCities.first()
        repository.get(city); repository.get(city); repository.get(city, force = true)
        assertEquals(1, server.requestCount)
        now += 30_001; repository.get(city, force = true)
        assertEquals(2, server.requestCount)
        repository.get(weatherCities[1])
        assertEquals(3, server.requestCount)
    }

    @Test fun expiredCacheIsFetchedAgain() = runBlocking {
        reply(); reply()
        repository.get(weatherCities.first()); now += 600_000
        repository.get(weatherCities.first())
        assertEquals(2, server.requestCount)
    }

    @Test fun missingValuesStayMissingAndUnknownCodesNeverMeanClearSky() = runBlocking {
        reply(sample.replace("\"temperature_2m\":8.9", "\"temperature_2m\":null")
            .replace("\"wind_speed_10m\":1.22", "\"wind_speed_10m\":null").replace("\"weather_code\":0", "\"weather_code\":1234"))
        val result = repository.get(weatherCities.first())
        assertNull(result.temperatureC); assertNull(result.windMetresPerSecond)
        assertEquals("날씨 상태 정보 없음", weatherDescription(result.weatherCode))
    }

    @Test fun wrongUnitsAreNotDisplayedAsRequestedUnits() = runBlocking {
        reply(sample.replace("\"wind_speed_10m\":\"m/s\"", "\"wind_speed_10m\":\"km/h\""))
        assertNull(repository.get(weatherCities.first()).windMetresPerSecond)
    }

    @Test fun errorsDoNotCreateFakeWeatherOrLogOutTheUser() = runBlocking {
        ApiSession.start(LoginResponse("still-valid", "bearer", 1800), User(1, "runner", "r@example.test", createdAt = "", updatedAt = ""))
        reply("{}", 429)
        try { repository.get(weatherCities.first()); fail("Expected failure") }
        catch (failure: WeatherFailure) { assertTrue(failure.message!!.contains("잠시 후")) }
        assertNotNull(ApiSession.state.value)
        reply(); assertNotNull(repository.get(weatherCities.first()).temperatureC)
    }

    @Test fun invalidTimeAndEmptyResponsesAreRejected() = runBlocking {
        reply("{}"); reply(sample.replace("2026-11-10T17:30", "invalid"))
        repeat(2) {
            try { repository.get(weatherCities.first()); fail("Expected missing data failure") }
            catch (_: WeatherFailure) { }
        }
    }

    @Test fun oldModelDataAndWeatherCodesAreLabeledHonestly() = runBlocking {
        reply()
        val data = repository.get(weatherCities.first())
        assertFalse(data.isOld(now))
        assertTrue(data.isOld(now + 3 * 60 * 60 * 1000L))
        assertEquals("비", weatherDescription(61))
        assertEquals("눈", weatherDescription(75))
        assertEquals("뇌우", weatherDescription(95))
        assertEquals("날씨 상태 정보 없음", weatherDescription(null))
    }
}
