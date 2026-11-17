package com.runtrack.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runtrack.data.weather.*
import com.runtrack.ui.theme.RunTrackColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HomeWeather(model: WeatherViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(model) { model.refresh() }
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    WeatherCard(state, model::select) { model.refresh(force = true) }
}

@Composable
internal fun WeatherCard(state: WeatherState, select: (WeatherCity) -> Unit, refresh: () -> Unit) {
    var choosingCity by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    RunTrackCard(Modifier.testTag("weatherCard")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("날씨", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { choosingCity = true }, modifier = Modifier.testTag("weatherCity")) {
                Icon(Icons.Outlined.LocationOn, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp)); Text(state.city.name)
                Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
            }
            IconButton(onClick = refresh, enabled = !state.loading, modifier = Modifier.testTag("weatherRefresh")) {
                if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, "날씨 새로고침", Modifier.size(20.dp))
            }
        }
        val data = state.data?.takeIf { it.city == state.city }
        if (data == null) {
            Text(if (state.error != null) "날씨를 불러오지 못했어요." else "${state.city.name} 날씨를 불러오고 있어요.",
                style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(weatherIcon(data), null, Modifier.size(42.dp), tint = RunTrackColors.Accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(weatherNumber(data.temperatureC, "°C"), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("weatherTemperature"))
                    Text(weatherDescription(data.weatherCode), style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("선택한 도시 기준", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
                    Text("체감 ${weatherNumber(data.feelsLikeC, "°C")}", style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCaption("습도", data.humidityPercent?.let { "$it%" } ?: "—", Modifier.weight(1f))
                MetricCaption("바람", weatherNumber(data.windMetresPerSecond, " m/s"), Modifier.weight(1f))
                val minutes = data.intervalSeconds?.takeIf { it % 60 == 0 }?.div(60)
                MetricCaption(if (minutes != null) "강수 (${minutes}분)" else "강수", weatherNumber(data.precipitationMm, " mm"), Modifier.weight(1f))
            }
            val time = DateTimeFormatter.ofPattern("M.d HH:mm").withZone(ZoneId.of("Asia/Seoul")).format(data.validAt)
            Text("$time 기준 · 한국 시간", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
            if (state.stale || data.isOld()) Badge("이전 정보 · 새로고침 필요", warning = true)
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
        Column {
            Text("예보 모델 기반 · 상태명 한국어 표기", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { uri.openUri("https://open-meteo.com/") }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Text("Open-Meteo", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { uri.openUri("https://creativecommons.org/licenses/by/4.0/") }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("CC BY 4.0", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
    if (choosingCity) AlertDialog(onDismissRequest = { choosingCity = false }, title = { Text("날씨 도시 선택") },
        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text("선택한 도시의 중심 지역 기준으로 표시해요.", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
            weatherCities.forEach { city ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.RadioButton) {
                    choosingCity = false; select(city)
                }.testTag("weatherCity_${city.id}"), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = city == state.city, onClick = null)
                    Text(city.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } }, confirmButton = { TextButton(onClick = { choosingCity = false }) { Text("닫기") } })
}

private fun weatherNumber(value: Double?, suffix: String): String = value?.takeIf { it.isFinite() }
    ?.let { String.format(Locale.KOREAN, "%.1f", it) + suffix } ?: "—"

private fun weatherIcon(data: CurrentWeather): ImageVector = when (data.weatherCode) {
    0, 1 -> if (data.isDay == false) Icons.Outlined.NightsStay else Icons.Outlined.WbSunny
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Outlined.Umbrella
    71, 73, 75, 77, 85, 86 -> Icons.Outlined.AcUnit
    95, 96, 99 -> Icons.Outlined.FlashOn
    else -> Icons.Outlined.Cloud
}
