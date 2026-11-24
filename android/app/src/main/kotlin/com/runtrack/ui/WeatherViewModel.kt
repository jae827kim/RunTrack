package com.runtrack.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runtrack.data.weather.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherState(
    val city: WeatherCity = weatherCities.first(), val data: CurrentWeather? = null,
    val loading: Boolean = false, val error: String? = null, val stale: Boolean = false
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("weather_city", Context.MODE_PRIVATE)
    private val repository = WeatherRepository()
    private val cities = mutableMapOf<String, CurrentWeather>()
    private val mutable = MutableStateFlow(WeatherState(city = weatherCities.find { it.id == preferences.getString("city", null) } ?: weatherCities.first()))
    val state = mutable.asStateFlow()
    private var request: Job? = null

    fun select(city: WeatherCity) {
        if (city == mutable.value.city) return
        request?.cancel()
        preferences.edit().putString("city", city.id).apply()
        mutable.value = WeatherState(city = city, data = cities[city.id], stale = cities[city.id]?.isOld() ?: false)
        refresh()
    }

    fun refresh(force: Boolean = false) {
        if (request?.isActive == true) return
        val city = mutable.value.city
        mutable.value = mutable.value.copy(loading = true, error = null)
        request = viewModelScope.launch {
            try {
                val data = repository.get(city, force)
                if (mutable.value.city == city) {
                    cities[city.id] = data
                    mutable.value = WeatherState(city, data, stale = data.isOld())
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (mutable.value.city == city) mutable.value = mutable.value.copy(loading = false, stale = true,
                    error = if (failure is WeatherFailure) failure.message else "날씨를 불러오지 못했어요. 네트워크를 확인해 주세요.")
            }
        }
    }
}
