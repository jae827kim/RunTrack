package com.runtrack.data.api

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.runtrack.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val httpClient: OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)

    init {
        // 개발 모드에서만 로깅 추가
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            httpClient.addInterceptor(logging)
        }

        // 토큰 인터셉터 추가
        httpClient.addInterceptor { chain ->
            val original = chain.request()
            
            val request = original.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()
            
            chain.proceed(request)
        }
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(httpClient.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val userService: UserService = retrofit.create(UserService::class.java)
    val shoeService: ShoeService = retrofit.create(ShoeService::class.java)
    val runningService: RunningService = retrofit.create(RunningService::class.java)
    val weatherService: WeatherService = retrofit.create(WeatherService::class.java)
}
