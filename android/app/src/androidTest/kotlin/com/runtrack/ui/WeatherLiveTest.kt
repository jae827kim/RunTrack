package com.runtrack.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runtrack.data.weather.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherLiveTest {
    @Test fun readsARealPublicCityForecastOnAndroid() = runBlocking {
        val data = WeatherRepository().get(weatherCities.first())
        assertEquals("seoul", data.city.id)
        assertNotNull(data.temperatureC)
        assertNotNull(data.validAt)
    }
}
