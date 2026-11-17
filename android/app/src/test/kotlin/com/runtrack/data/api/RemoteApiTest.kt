package com.runtrack.data.api

import com.google.gson.JsonParser
import com.runtrack.data.local.*
import com.runtrack.data.models.*
import com.runtrack.data.repository.*
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.*
import org.junit.Assert.*
import java.time.ZoneId
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class RemoteApiTest {
    private lateinit var server: MockWebServer
    private lateinit var users: UserService
    private lateinit var repository: RemoteTrackRepository
    private val profile = """{"id":1,"username":"runner","email":"runner@example.com","created_at":"2026-11-01T00:00:00","updated_at":"2026-11-01T00:00:00"}"""
    private val user get() = com.google.gson.Gson().fromJson(profile, User::class.java)

    @Before fun setup() {
        ApiSession.clear()
        server = MockWebServer(); server.start()
        val retrofit = RetrofitClient.create(server.url("/").toString())
        users = retrofit.create(UserService::class.java)
        repository = RemoteTrackRepository(retrofit.create(ShoeService::class.java), retrofit.create(RunningService::class.java), ZoneId.of("Asia/Seoul"))
    }
    @After fun close() { ApiSession.clear(); server.shutdown() }
    private fun reply(json: String, code: Int = 200) { server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(json)) }
    private fun request() = server.takeRequest(3, TimeUnit.SECONDS) ?: error("Missing request")
    private fun login() { ApiSession.start(LoginResponse("valid-token", "bearer", 1800), user) }

    @Test fun loginChecksProfileWithTheNewTokenBeforeEnteringTheApp() = runBlocking {
        reply("""{"access_token":"valid-token","token_type":"bearer","expires_in":1800}""")
        reply(profile)
        AuthRepository(users).login("runner", "secret-password")
        assertEquals("/api/users/login", request().path)
        val me = request()
        assertEquals("/api/users/me", me.path)
        assertEquals("Bearer valid-token", me.getHeader("Authorization"))
        assertEquals(1, ApiSession.state.value!!.user.id)
    }

    @Test fun badCredentialsDoNotCreateASession() = runBlocking {
        reply("""{"detail":"Invalid username or password"}""", 401)
        try { AuthRepository(users).login("runner", "wrong"); fail("Expected rejection") }
        catch (error: ApiFailure) { assertTrue(error.message!!.contains("올바르지")) }
        assertNull(ApiSession.state.value)
    }

    @Test fun authenticationExpiryReturnsToLoginButOldTokensCannotLogOutANewSession() {
        ApiSession.start(LoginResponse("old", "bearer", 1), user, nowMillis = 1000)
        assertNull(ApiSession.token(nowMillis = 2000))
        login()
        ApiSession.invalidate("old")
        assertEquals("valid-token", ApiSession.token())
    }

    @Test fun protectedRequest401ClearsSession() = runBlocking {
        login(); reply("""{"detail":"Could not validate credentials"}""", 401)
        try { repository.load(); fail("Expected 401") } catch (_: ApiFailure) { }
        assertNull(ApiSession.state.value)
        assertEquals("Bearer valid-token", request().getHeader("Authorization"))
    }

    @Test fun allPagesAreLoadedAndUtcDatesAndServerShoeTotalsArePreserved() = runBlocking {
        login()
        reply("""[{"id":7,"user_id":1,"brand":"Nike","model":"Pegasus","purchase_date":"2026-11-01T00:00:00","cumulative_km":500.5,"run_count":120,"condition":"good"}]""")
        fun record(id: Int) = """{"id":$id,"user_id":1,"distance_km":1.0,"duration_minutes":6,"start_time":"2026-11-08T16:30:00","end_time":"2026-11-08T16:36:00","shoe_id":7,"avg_pace_min_per_km":6.0}"""
        reply((1..100).joinToString(",", "[", "]") { record(it) }); reply("[${record(101)}]")
        val data = repository.load()
        assertEquals(101, data.runs.size)
        assertEquals("2026-11-09T01:30", data.runs.first().startTime)
        assertEquals(101.0, data.weekSummary(LocalDate.of(2026,11,9)).distanceKm, 0.0)
        assertEquals(500.5, data.shoeDistance(data.shoes.single()), 0.0)
        assertEquals(120, data.shoeRunCount(data.shoes.single()))
        assertEquals("/api/shoes", request().path)
        assertEquals("/api/running?skip=0&limit=100", request().path)
        assertEquals("/api/running?skip=100&limit=100", request().path)
    }

    @Test fun editingCanDetachAShoeAndClearNotesWithoutSendingUnsupportedFields() = runBlocking {
        login(); reply("{}")
        repository.saveRun(RunDraft(startTime = "2026-11-10T07:00", duration = "32", distance = "5.2"), "3")
        val update = request()
        assertEquals("PUT", update.method)
        assertEquals("/api/running/3", update.path)
        val json = JsonParser.parseString(update.body.readUtf8()).asJsonObject
        assertTrue(json.get("shoe_id").isJsonNull)
        assertTrue(json.get("notes").isJsonNull)
        assertEquals("2026-11-10T07:32+09:00", json.get("end_time").asString)
        assertFalse(json.has("calories_burned")); assertFalse(json.has("avg_heart_rate"))
    }

    @Test fun createUsesOffsetTimeAndServerSupportedFieldsOnly() = runBlocking {
        login(); reply("{}", 201)
        repository.saveRun(RunDraft(startTime = "2026-11-10T07:00", duration = "32", distance = "5.2", heartRate = "148", rating = "great"), null)
        val json = JsonParser.parseString(request().body.readUtf8()).asJsonObject
        assertEquals("2026-11-10T07:00+09:00", json.get("start_time").asString)
        assertEquals(148, json.get("avg_heart_rate").asInt)
        assertFalse(json.has("calories_burned"))
    }

    @Test fun fractionalMinutesAreRejectedBeforeAnyHttpRequest() = runBlocking {
        try { repository.saveRun(RunDraft(duration = "3.5", distance = "1"), null); fail("Expected validation") }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, server.requestCount)
    }

    @Test fun newShoeUsesTheCreateEndpointAndDefaultCondition() = runBlocking {
        login(); reply("{}", 201)
        repository.saveShoe(ShoeDraft(brand = "API 테스트 브랜드", model = "API 테스트 모델", size = "260 mm"), null)
        val create = request()
        assertEquals("/api/shoes", create.path)
        val json = JsonParser.parseString(create.body.readUtf8()).asJsonObject
        assertEquals("good", json.get("condition").asString)
    }

    @Test fun uncertainPostIsNotAutomaticallySentAgain() = runBlocking {
        login()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        try { repository.saveShoe(ShoeDraft(brand = "Brand", model = "Model"), null); fail("Expected lost response") }
        catch (_: java.io.IOException) { }
        assertEquals(1, server.requestCount)
    }

    @Test fun readOnlyMetricsDoNotPreventUpdatingSupportedFields() = runBlocking {
        login(); reply("{}")
        repository.saveRun(RunDraft(duration = "32", distance = "5.2", pace = "0:00", calories = "-1", heartRate = "0"), "3")
        val json = JsonParser.parseString(request().body.readUtf8()).asJsonObject
        assertEquals(32, json.get("duration_minutes").asInt)
        assertFalse(json.has("calories_burned")); assertFalse(json.has("avg_heart_rate"))
    }
}
