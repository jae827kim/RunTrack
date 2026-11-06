@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.runtrack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runtrack.ui.theme.RunTrackColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import com.runtrack.R
import com.runtrack.data.local.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrackApp(vm: TrackViewModel = viewModel(), onLogout: (() -> Unit)? = null) {
    val state by vm.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var removeShoe by rememberSaveable { mutableStateOf<String?>(null) }
    var removeRun by rememberSaveable { mutableStateOf<String?>(null) }
    var settings by remember { mutableStateOf(false) }
    fun navigate(destination: String, id: String? = null) { editId = id; screen = destination }
    BackHandler(enabled = screen != "home" || state.saving) {
        if (!state.saving) screen = when (screen) { "shoeEditor" -> "shoes"; "runEditor" -> "runs"; else -> "home" }
    }
    Surface(Modifier.fillMaxSize(), color = RunTrackColors.Background) {
        when {
            state.loading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                Text("러닝 기록을 불러오고 있어요", style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
            }
            state.loadFailed -> Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                EmptyStateCard("기록을 불러오지 못했어요.", state.error ?: "잠시 후 다시 시도해 주세요.",
                    "다시 불러오기", Icons.Outlined.Refresh, action = vm::reload)
            }
            screen == "shoeEditor" -> ShoeEditor(state.data.shoes.find { it.id == editId }, state.saving,
                close = { navigate("shoes") }, save = { vm.saveShoe(it, editId) { navigate("shoes") } })
            screen == "runEditor" -> RunEditor(state.data.runs.find { it.id == editId }, state.data.shoes, state.saving,
                close = { navigate("runs") }, save = { vm.saveRun(it, editId) { navigate("runs") } })
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (screen) {
                        "runs" -> RunningScreen(state.data, { navigate("runEditor") }, { navigate("runEditor", it) }, { removeRun = it })
                        "shoes" -> ShoesScreen(state.data, { navigate("shoeEditor") }, { navigate("shoeEditor", it) }, { removeShoe = it })
                        "ai" -> AiScreen { navigate("home") }
                        else -> HomeScreen(state.data, { navigate("ai") }, { settings = true }, { navigate("runEditor") })
                    }
                }
                TrackNavigation(screen) { navigate(it) }
            }
        }
    }
    state.error?.takeUnless { state.loadFailed }?.let { message ->
        AlertDialog(onDismissRequest = vm::clearError, title = { Text("저장하지 못했어요") }, text = { Text(message) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("확인") } })
    }
    removeShoe?.let { id ->
        DeleteDialog("신발을 삭제할까요?", "러닝 기록은 유지되고, 과거 기록에는 당시 신발 이름이 남습니다.", { removeShoe = null }) {
            vm.deleteShoe(id); removeShoe = null
        }
    }
    removeRun?.let { id ->
        DeleteDialog("러닝 기록을 삭제할까요?", "주간 요약과 신발 누적 기록에도 반영됩니다.", { removeRun = null }) {
            vm.deleteRun(id); removeRun = null
        }
    }
    if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text("기록 저장 안내") },
        text = { Text("기록은 이 기기에 저장됩니다. 앱을 삭제하면 저장한 기록도 삭제됩니다.") },
        confirmButton = { TextButton(onClick = { settings = false }) { Text("확인") } },
        dismissButton = {
            if (onLogout != null) TextButton(onClick = { settings = false; onLogout() }, modifier = Modifier.testTag("logout")) { Text("로그아웃") }
        })
}

@Composable
fun HomeScreen(data: TrackSnapshot, ai: () -> Unit, settings: () -> Unit, add: () -> Unit) {
    val summary = data.weekSummary(LocalDate.now())
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Column(Modifier.fillMaxSize().background(RunTrackColors.Background)) {
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(RunTrackColors.SoftAccent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DirectionsRun, null, Modifier.size(24.dp), tint = RunTrackColors.Accent)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("RunTrack", style = MaterialTheme.typography.titleLarge)
                Text("오늘도 나만의 페이스로", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
            }
            IconButton(onClick = settings, modifier = Modifier.testTag("settings")) {
                Icon(Icons.Outlined.Settings, "설정", Modifier.size(23.dp), tint = RunTrackColors.Text)
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(Modifier.fillMaxWidth().testTag("weekDistance"), color = RunTrackColors.SoftAccent, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("이번 주 요약", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(monday.format(DateTimeFormatter.ofPattern("M.d")) + " – " + monday.plusDays(6).format(DateTimeFormatter.ofPattern("M.d")),
                            style = MaterialTheme.typography.labelMedium, color = RunTrackColors.Accent)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("총 거리", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Accent)
                            Text("${number(summary.distanceKm)} km", style = MaterialTheme.typography.displaySmall)
                            Text("달린 만큼 쌓이는 나의 기록", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
                        }
                        WeeklySparkline(data, Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeeklyMetric("총 러닝 시간", "${number(summary.durationMinutes)} 분", Icons.Outlined.Schedule,
                    Modifier.weight(1f).testTag("weekDuration"))
                WeeklyMetric("평균 페이스", "${paceText(summary.pace)} /km", Icons.Outlined.Timer,
                    Modifier.weight(1f).testTag("weekPace"))
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("최근 러닝 기록 3개", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = ai, modifier = Modifier.testTag("openAi"), contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI 추천", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (data.runs.isEmpty()) EmptyStateCard("아직 러닝 기록이 없어요.", "첫 러닝을 기록해 보세요.", "러닝 기록 추가", action = add)
            else data.recentRuns().forEach { run ->
                Surface(shape = RoundedCornerShape(18.dp), color = RunTrackColors.Surface, border = BorderStroke(1.dp, RunTrackColors.Outline)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(LocalDateTime.parse(run.startTime).format(DateTimeFormatter.ofPattern("M월 d일 · HH:mm")),
                            style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${distance(run.distanceKm)} km", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${number(run.durationMinutes)}분", style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
                            Text("·", color = RunTrackColors.Muted)
                            Text("${paceText(run.effectivePace)} /km", style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySparkline(data: TrackSnapshot, modifier: Modifier) {
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val totals = (0..6).map { day -> data.runs.filter { LocalDateTime.parse(it.startTime).toLocalDate() == monday.plusDays(day.toLong()) }.sumOf { it.distanceKm } }
    val maximum = (totals.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Row(modifier.height(88.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
        totals.forEachIndexed { index, total ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.height(60.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.width(10.dp).height((4 + total / maximum * 52).dp)
                        .background(if (total > 0) RunTrackColors.Accent else RunTrackColors.Outline, RoundedCornerShape(4.dp))
                        .semantics { contentDescription = "${listOf("월", "화", "수", "목", "금", "토", "일")[index]}요일 ${number(total)} km" })
                }
                Text(listOf("월", "화", "수", "목", "금", "토", "일")[index], style = MaterialTheme.typography.labelSmall, color = RunTrackColors.Muted)
            }
        }
    }
}

@Composable
private fun WeeklyMetric(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = RunTrackColors.Surface, border = BorderStroke(1.dp, RunTrackColors.Outline)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp), tint = RunTrackColors.Accent)
                Text(label, style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
            }
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
internal fun RunningScreen(data: TrackSnapshot, add: () -> Unit, edit: (String) -> Unit, delete: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val records = data.runs.sortedByDescending { it.startTime }.filter {
        query.isBlank() || listOf(it.startTime, it.shoeName.orEmpty(), it.notes.orEmpty(), it.rating.orEmpty()).any { value -> value.contains(query, true) }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("러닝 기록", "추가 +", action = add)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("searchRuns"), singleLine = true,
                shape = RoundedCornerShape(14.dp), leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp)) },
                placeholder = { Text("날짜·신발·메모 검색", style = MaterialTheme.typography.bodyMedium) })
            Text("${records.size}개의 기록", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
            if (records.isEmpty()) EmptyStateCard(if (query.isBlank()) "아직 러닝 기록이 없어요." else "검색 결과가 없어요.",
                "날짜와 거리부터 간단히 기록해 보세요.", "러닝 기록 추가", action = add)
            records.forEach { run ->
                RunTrackCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(LocalDateTime.parse(run.startTime).format(DateTimeFormatter.ofPattern("M월 d일 (E) · HH:mm", Locale.KOREAN)),
                            Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
                        run.rating?.let { Badge(it) }
                    }
                    Text("${distance(run.distanceKm)} km", style = MaterialTheme.typography.headlineLarge)
                    Row(Modifier.fillMaxWidth()) {
                        MetricCaption("러닝 시간", "${number(run.durationMinutes)}분", Modifier.weight(1f), Icons.Outlined.Schedule)
                        MetricCaption("페이스", "${paceText(run.effectivePace)} /km", Modifier.weight(1f), Icons.Outlined.Timer)
                    }
                    run.shoeName?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ShoeIcon(18.dp)
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
                        }
                    }
                    val details = listOfNotNull(run.heartRate?.let { "심박수 $it bpm" }, run.calories?.let { "칼로리 ${number(it)} kcal" },
                        run.averageSpeedKmh?.let { "평균 속도 ${number(it)} km/h" }, run.temperatureC?.let { "기온 ${number(it)} °C" })
                    if (details.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        details.forEach { detail -> Text(detail, style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted,
                            modifier = Modifier.background(RunTrackColors.Field, RoundedCornerShape(8.dp)).padding(8.dp)) }
                    }
                    run.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted) }
                    EditActions({ edit(run.id) }, { delete(run.id) })
                }
            }
        }
    }
}

@Composable
internal fun ShoesScreen(data: TrackSnapshot, add: () -> Unit, edit: (String) -> Unit, delete: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("신발 관리", "추가 +", action = add)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("함께 달리는 신발 ${data.shoes.size}켤레", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
            if (data.shoes.isEmpty()) EmptyStateCard("등록된 신발이 없어요.", "함께 달리는 신발을 추가해 보세요.", "신발 추가", Icons.Outlined.Inventory2, add)
            data.shoes.forEach { shoe ->
                RunTrackCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).background(RunTrackColors.SoftAccent, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { ShoeIcon(38.dp) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(shoe.brand, style = MaterialTheme.typography.labelMedium, color = RunTrackColors.Accent)
                            Text(shoe.model, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            shoe.condition?.let { Badge(it, warning = it == "교체 권장" || it == "폐기") }
                        }
                    }
                    Divider(color = RunTrackColors.Outline)
                    Row(Modifier.fillMaxWidth()) {
                        MetricCaption("누적 주행", "${distance(data.shoeDistance(shoe))} km", Modifier.weight(1f))
                        MetricCaption("러닝 횟수", "${data.shoeRunCount(shoe)}회", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth()) {
                        MetricCaption("사이즈", shoe.size ?: "—", Modifier.weight(1f))
                        MetricCaption("색상", shoe.color ?: "—", Modifier.weight(1f))
                    }
                    Text("구매일  ${shoe.purchaseDate}", style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
                    shoe.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted) }
                    EditActions({ edit(shoe.id) }, { delete(shoe.id) })
                }
            }
        }
    }
}

@Composable
internal fun AiScreen(back: () -> Unit) {
    var message by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("AI 신발 추천", "새로고침", action = { message = true })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            InformationNote("디자인 예시 · 실제 AI 추천이 아닙니다")
            SectionHeading("나의 러닝에 맞는 한 켤레", "나에게 맞는 쿠셔닝과 착화감을 살펴보세요.")
            RunTrackCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Badge("추천 01")
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(20.dp), tint = RunTrackColors.Accent)
                }
                Box(Modifier.fillMaxWidth().height(100.dp).background(RunTrackColors.Field, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { ShoeIcon(68.dp) }
                Text("Nike Air Zoom Pegasus 41", style = MaterialTheme.typography.titleLarge)
                Text("가벼우면서 충분한 쿠셔닝을 제공하는 러닝화 예시입니다. 실제 추천 결과는 추천 서비스 연동 후 표시됩니다.",
                    style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
                MetricCaption("예시 가격대", "$100–120")
                Divider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCaption("장점", "가벼움 · 통풍성\n내구성", Modifier.weight(1f), Icons.Outlined.CheckCircle)
                    MetricCaption("고려할 점", "비용", Modifier.weight(1f), Icons.Outlined.Info)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { message = true }, Modifier.weight(1f)) { Text("더보기") }
                    OutlinedButton(onClick = { message = true }, Modifier.weight(1f)) { Text("구매 정보") }
                }
            }
            TextButton(onClick = back, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("홈으로 돌아가기") }
        }
    }
    if (message) AlertDialog(onDismissRequest = { message = false }, title = { Text("추천 서비스 준비 중") },
        text = { Text("현재 화면은 디자인 예시입니다. 추천과 구매 정보는 서비스 연결 후 사용할 수 있어요.") },
        confirmButton = { TextButton(onClick = { message = false }) { Text("확인") } })
}

@Composable
internal fun TrackNavigation(screen: String, navigate: (String) -> Unit) {
    Surface(shadowElevation = 6.dp, color = RunTrackColors.Surface) {
        Column {
            Divider(color = RunTrackColors.Outline)
            Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(Triple("home", R.drawable.home_btn, "홈"), Triple("runs", R.drawable.clipboard_btn, "러닝"),
                    Triple("shoes", R.drawable.shoes_btn, "신발"), Triple("runEditor", R.drawable.plus_btn, "추가")).forEach { (route, image, label) ->
                    val active = screen == route
                    Column(Modifier.weight(1f).fillMaxHeight().semantics { selected = active }
                        .clickable(role = Role.Button) { navigate(route) }.testTag("nav_$route"),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(Modifier.size(width = 54.dp, height = 32.dp).background(if (active) RunTrackColors.Selection else Color.Transparent,
                            RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Image(painterResource(image), when (route) { "runs" -> "러닝 기록"; "shoes" -> "신발 관리"; "runEditor" -> "러닝 추가"; else -> "홈" },
                                Modifier.size(if (route == "runEditor") 22.dp else 24.dp), colorFilter = ColorFilter.tint(if (active) RunTrackColors.Accent else RunTrackColors.Muted))
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) RunTrackColors.Accent else RunTrackColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoeIcon(size: Dp) {
    Image(painterResource(R.drawable.shoes_btn), null, Modifier.size(size), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(RunTrackColors.Accent))
}

@Composable
private fun EditActions(edit: () -> Unit, delete: () -> Unit) {
    Column {
        Divider(color = RunTrackColors.Outline)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = edit) { Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("편집") }
            TextButton(onClick = delete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("삭제")
            }
        }
    }
}

@Composable
private fun DeleteDialog(title: String, detail: String, dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(detail) },
        confirmButton = { TextButton(onClick = confirm) { Text("삭제", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = dismiss) { Text("취소") } })
}

private fun distance(value: Double) = "%.2f".format(Locale.US, value)
