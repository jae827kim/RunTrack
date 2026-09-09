package com.runtrack.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // TODO: Retrofit 서비스, Repository 주입 정의
}
    
    @Provides
    @Singleton
    fun provideWeatherService(): WeatherService = RetrofitClient.weatherService
    
    @Provides
    @Singleton
    fun provideShoeRepository(): ShoeRepository = ShoeRepository()
}
