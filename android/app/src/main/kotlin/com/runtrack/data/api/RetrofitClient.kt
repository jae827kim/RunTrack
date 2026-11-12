package com.runtrack.data.api

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.runtrack.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.ConnectionPool
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    fun create(baseUrl: String): Retrofit {
        val baseClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .addInterceptor { chain ->
                    val token = ApiSession.token()
                    val original = chain.request()
                    val request = if (token != null && original.header("Authorization") == null)
                        original.newBuilder().header("Authorization", "Bearer $token").build() else original
                    val response = chain.proceed(request)
                    if (response.code == 401) {
                        request.header("Authorization")?.removePrefix("Bearer ")?.let(ApiSession::invalidate)
                    }
                    response
                }.build()
        val reads = baseClient.newBuilder().retryOnConnectionFailure(true).build()
        // The API has no idempotency keys for manual POSTs. Never replay them and do not
        // reuse a pooled idle socket which Uvicorn may have already closed during editing.
        val writes = baseClient.newBuilder().connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS)).build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .callFactory { request ->
                (if (request.method == "GET") reads else writes).newCall(request)
            }
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .serializeNulls()
                        .create()
                )
            )
            .build()
    }
    private val retrofit by lazy { create(BuildConfig.API_BASE_URL) }

    val userService: UserService by lazy { retrofit.create(UserService::class.java) }
    val shoeService: ShoeService by lazy { retrofit.create(ShoeService::class.java) }
    val runningService: RunningService by lazy { retrofit.create(RunningService::class.java) }
    val weatherService: WeatherService by lazy { retrofit.create(WeatherService::class.java) }
}
