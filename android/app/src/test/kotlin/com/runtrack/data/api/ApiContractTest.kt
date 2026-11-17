package com.runtrack.data.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.runtrack.data.models.LoginResponse
import com.runtrack.data.models.ShoeCreateRequest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiContractTest {
    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    @Test
    fun backendTokenFieldsAreDecoded() {
        val token = gson.fromJson(
            """{"access_token":"test-token","token_type":"bearer","expires_in":3600}""",
            LoginResponse::class.java
        )
        assertEquals("test-token", token.accessToken)
        assertEquals("bearer", token.tokenType)
        assertEquals(3600, token.expiresIn)
    }

    @Test
    fun requestUsesBackendFieldNames() {
        val json = gson.toJsonTree(ShoeCreateRequest("Brand", "Model", purchaseDate = "2026-11-10T00:00:00")).asJsonObject
        assertEquals("2026-11-10T00:00:00", json.get("purchase_date").asString)
        assertFalse(json.has("purchase_price_won"))
    }

    @Test
    fun serviceDeclarationsAreValidRetrofitInterfaces() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .validateEagerly(true)
            .build()
        retrofit.create(UserService::class.java)
        retrofit.create(ShoeService::class.java)
        retrofit.create(RunningService::class.java)
        retrofit.create(WeatherService::class.java)
    }
}
