package com.runtrack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.runtrack.data.local.TrackSnapshot
import com.runtrack.data.local.TrackedRun
import com.runtrack.data.local.TrackedShoe
import com.runtrack.ui.theme.RunTrackColors
import com.runtrack.ui.theme.RunTrackTheme
import com.runtrack.ui.auth.LoginScreen
import com.runtrack.ui.auth.SignupScreen
import com.runtrack.data.weather.CurrentWeather
import com.runtrack.data.weather.weatherCities
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Preview(name = "홈 화면", showBackground = true, locale = "ko", widthDp = 393, heightDp = 780)
@Composable
private fun HomePreview() {
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val example = TrackSnapshot(runs = listOf(
        TrackedRun("preview-1", monday.atTime(7, 0).toString(), 31.0, 5.2),
        TrackedRun("preview-2", monday.plusDays(2).atTime(19, 0).toString(), 35.0, 6.0),
        TrackedRun("preview-3", monday.plusDays(4).atTime(7, 30).toString(), 32.0, 5.2)
    ))
    RunTrackTheme {
        Surface {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { HomeScreen(example, {}, {}, {}, weatherContent = { WeatherCard(previewWeather(), {}, {}) }) }
                TrackNavigation("home") {}
            }
        }
    }
}

private fun previewWeather() = WeatherState(data = CurrentWeather(weatherCities.first(), Instant.now(),
    18.0, 17.0, 60, 1.5, 0.0, 2, true, 900, System.currentTimeMillis()))

@Preview(name = "날씨 카드 · 예시", showBackground = true, locale = "ko", widthDp = 393)
@Composable
private fun WeatherPreview() {
    RunTrackTheme { Surface { WeatherCard(previewWeather(), {}, {}) } }
}

@Preview(name = "신발 추가", showBackground = true, locale = "ko", widthDp = 393, heightDp = 780)
@Composable
private fun ShoeEditorPreview() {
    RunTrackTheme { Surface { ShoeEditor(null, false, {}, {}, serverMode = true) } }
}

@Preview(name = "러닝 기록 추가", showBackground = true, locale = "ko", widthDp = 393, heightDp = 780)
@Composable
private fun RunEditorPreview() {
    RunTrackTheme { Surface { RunEditor(null, emptyList(), false, {}, {}, serverMode = true) } }
}

@Preview(name = "로그인", showBackground = true, locale = "ko", widthDp = 393, heightDp = 780)
@Composable
private fun LoginPreview() {
    RunTrackTheme { Surface { LoginScreen(onContinue = { _, _ -> }, onSignup = {}) } }
}

@Preview(name = "회원가입", showBackground = true, locale = "ko", widthDp = 393, heightDp = 780)
@Composable
private fun SignupPreview() {
    RunTrackTheme { Surface { SignupScreen(onBack = {}, onComplete = {}) } }
}

private fun designExample(): TrackSnapshot {
    val shoe = TrackedShoe("preview-shoe", "Nike", "Air Zoom Pegasus 41", "2026-09-01",
        "260mm", "화이트", "사용 중", 45.0, 8, "가볍게 달리는 날 함께하는 신발")
    val run = TrackedRun("preview-run", LocalDate.now().atTime(7, 0).toString(), 32.0, 5.2,
        heartRate = 148, calories = 320.0, shoeId = shoe.id, shoeName = shoe.displayName,
        averageSpeedKmh = 9.75, rating = "great", temperatureC = 22.0,
        notes = "천천히 시작해서 일정한 페이스로 마무리했어요.")
    return TrackSnapshot(shoes = listOf(shoe), runs = listOf(run))
}

@Preview(name = "러닝 기록 목록", showBackground = true, locale = "ko", widthDp = 393, heightDp = 780)
@Composable
private fun RunsPreview() {
    RunTrackTheme { Surface(color = RunTrackColors.Background) { Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { RunningScreen(designExample(), {}, {}, {}) }
        TrackNavigation("runs") {}
    } } }
}

@Preview(name = "신발 목록", showBackground = true, locale = "ko", widthDp = 393, heightDp = 780)
@Composable
private fun ShoesPreview() {
    RunTrackTheme { Surface(color = RunTrackColors.Background) { Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { ShoesScreen(designExample(), {}, {}, {}) }
        TrackNavigation("shoes") {}
    } } }
}

@Preview(name = "AI 추천", showBackground = true, locale = "ko", widthDp = 393, heightDp = 780)
@Composable
private fun AiPreview() {
    RunTrackTheme { Surface(color = RunTrackColors.Background) { Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { AiScreen {} }
        TrackNavigation("ai") {}
    } } }
}
