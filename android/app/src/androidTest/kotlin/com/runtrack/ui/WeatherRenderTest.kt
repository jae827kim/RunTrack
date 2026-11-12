package com.runtrack.ui

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runtrack.data.weather.*
import com.runtrack.ui.theme.RunTrackTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WeatherRenderTest {
    @get:Rule val rule = createComposeRule()
    private fun sample() = CurrentWeather(weatherCities.first(), Instant.now(), 8.9, 6.6, 63, 1.2, 0.0, 0, false, 900, System.currentTimeMillis())

    @Test fun showsWeatherAndChangingCityNeverKeepsThePreviousCityValues() {
        rule.setContent {
            var state by remember { mutableStateOf(WeatherState(data = sample())) }
            RunTrackTheme { Surface { WeatherCard(state, { state = WeatherState(city = it, loading = true) }, {}) } }
        }
        rule.onNodeWithTag("weatherTemperature").assertTextEquals("8.9°C")
        rule.onNodeWithText("Open-Meteo").assertIsDisplayed()
        capture("weather-card")
        rule.onNodeWithTag("weatherCity").performClick()
        rule.onNodeWithTag("weatherCity_busan").performScrollTo().performClick()
        rule.onNodeWithText("부산 날씨를 불러오고 있어요.").assertIsDisplayed()
        rule.onNodeWithTag("weatherTemperature").assertDoesNotExist()
    }

    @Test fun failureAllowsRetryAndDoesNotDisplayInventedTemperature() {
        var refreshes = 0
        rule.setContent { RunTrackTheme { Surface { WeatherCard(WeatherState(error = "네트워크를 확인해 주세요."), {}, { refreshes++ }) } } }
        rule.onNodeWithTag("weatherTemperature").assertDoesNotExist()
        rule.onNodeWithText("날씨를 불러오지 못했어요.").assertIsDisplayed()
        rule.onNodeWithTag("weatherRefresh").performClick()
        rule.runOnIdle { assertEquals(1, refreshes) }
        capture("weather-error")
    }

    @Test fun cachedWeatherIsMarkedAsPreviousInformation() {
        rule.setContent { RunTrackTheme { Surface {
            WeatherCard(WeatherState(data = sample().copy(temperatureC = null), stale = true, error = "새 정보를 불러오지 못했어요."), {}, {})
        } } }
        rule.onNodeWithTag("weatherTemperature").assertTextEquals("—")
        rule.onNodeWithText("이전 정보 · 새로고침 필요").assertIsDisplayed()
        capture("weather-stale")
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "weather-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
