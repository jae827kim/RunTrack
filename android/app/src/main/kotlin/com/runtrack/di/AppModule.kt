package com.runtrack.di

import com.runtrack.data.api.RetrofitClient
import com.runtrack.data.api.UserService
import com.runtrack.data.api.ShoeService
import com.runtrack.data.api.RunningService
import com.runtrack.data.api.WeatherService
import com.runtrack.data.repository.ShoeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideUserService(): UserService = RetrofitClient.userService
    
    @Provides
    @Singleton
    fun provideShoeService(): ShoeService = RetrofitClient.shoeService
    
    @Provides
    @Singleton
    fun provideRunningService(): RunningService = RetrofitClient.runningService
    
    @Provides
    @Singleton
    fun provideWeatherService(): WeatherService = RetrofitClient.weatherService
    
    @Provides
    @Singleton
    fun provideShoeRepository(): ShoeRepository = ShoeRepository()
}
