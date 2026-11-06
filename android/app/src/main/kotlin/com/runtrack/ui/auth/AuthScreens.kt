package com.runtrack.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runtrack.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.runtrack.ui.*
import com.runtrack.ui.theme.RunTrackColors

/** In-memory navigation for UI trials; it never authenticates or stores credentials. */
class AuthDemoViewModel : ViewModel() {
    var enteredDemo by mutableStateOf(false)
        private set
    var signingUp by mutableStateOf(false)
        private set
    var suggestedUsername by mutableStateOf("")
        private set
    var registrationReviewed by mutableStateOf(false)
        private set

    fun showSignup() { signingUp = true; registrationReviewed = false }
    fun showLogin() { signingUp = false }
    fun registrationFinished(username: String) {
        suggestedUsername = username.trim()
        registrationReviewed = true
        signingUp = false
    }
    fun enterDemo() { enteredDemo = true; registrationReviewed = false }
    fun signOut() {
        enteredDemo = false
        signingUp = false
        suggestedUsername = ""
        registrationReviewed = false
    }
}

@Composable
fun RunTrackEntry(model: AuthDemoViewModel = viewModel()) {
    when {
        model.enteredDemo -> TrackApp(onLogout = model::signOut)
        model.signingUp -> {
            BackHandler(onBack = model::showLogin)
            SignupScreen(onBack = model::showLogin, onComplete = model::registrationFinished)
        }
        else -> LoginScreen(
            initialUsername = model.suggestedUsername,
            registrationReviewed = model.registrationReviewed,
            onContinue = model::enterDemo,
            onSignup = model::showSignup
        )
    }
}

@Composable
fun LoginScreen(initialUsername: String = "", registrationReviewed: Boolean = false,
                onContinue: () -> Unit, onSignup: () -> Unit) {
    var username by rememberSaveable(initialUsername) { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var recovery by remember { mutableStateOf(false) }
    val errors = if (submitted) AuthValidation.login(username, password) else emptyMap()
    val focus = LocalFocusManager.current
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val submit: () -> Unit = {
        submitted = true
        val result = AuthValidation.login(username, password)
        when {
            "username" in result -> usernameFocus.requestFocus()
            "password" in result -> passwordFocus.requestFocus()
            else -> { focus.clearFocus(); password = ""; onContinue() }
        }
    }
    Column(Modifier.fillMaxSize().background(RunTrackColors.Background).imePadding().testTag("loginScreen")) {
        AuthBrand(R.drawable.auth_logo_login)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthIntro("다시 만나 반가워요", "오늘도 나만의 페이스로 달려볼까요?")
            AuthTabs(false, {}, { password = ""; onSignup() })
            InformationNote(if (registrationReviewed) "회원가입 입력을 확인했어요. 체험용 로그인을 진행해 주세요." else "임의의 아이디와 비밀번호로 화면을 체험할 수 있어요.",
                if (registrationReviewed) Modifier.testTag("registrationReviewed") else Modifier)
            RunTrackCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("아이디")
                    AuthInput(username, { username = it }, "아이디를 입력하세요", "loginUsername", errors["username"], usernameFocus, description = "아이디")
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("비밀번호")
                    AuthInput(password, { password = it }, "비밀번호를 입력하세요", "loginPassword", errors["password"], passwordFocus,
                        password = true, ime = ImeAction.Done, done = submit, description = "비밀번호")
                }
                PrimaryActionButton("로그인", submit, Modifier.testTag("loginSubmit"))
            }
            TextButton(onClick = { recovery = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("비밀번호 찾기", color = RunTrackColors.Muted)
            }
        }
    }
    if (recovery) AlertDialog(onDismissRequest = { recovery = false }, title = { Text("비밀번호 찾기") },
        text = { Text("지금은 화면 체험 단계예요. 실제 계정과 비밀번호 복구는 아직 연결되지 않았어요.") },
        confirmButton = { TextButton(onClick = { recovery = false }) { Text("확인") } })
}

@Composable
fun SignupScreen(onBack: () -> Unit, onComplete: (String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var checkedId by remember { mutableStateOf<String?>(null) }
    var idCheckRequested by remember { mutableStateOf(false) }
    val errors = if (submitted) AuthValidation.signup(SignupInput(username, password, confirmation, name, weight)) else emptyMap()
    val focus = LocalFocusManager.current
    val requests = remember { listOf("username", "password", "confirmation", "name", "weight").associateWith { FocusRequester() } }
    val submit: () -> Unit = {
        submitted = true
        val result = AuthValidation.signup(SignupInput(username, password, confirmation, name, weight))
        if (result.isEmpty()) {
            focus.clearFocus(); password = ""; confirmation = ""; onComplete(username)
        } else requests.getValue(result.keys.first()).requestFocus()
    }
    Column(Modifier.fillMaxSize().background(RunTrackColors.Background).imePadding().testTag("signupScreen")) {
        AuthBrand(R.drawable.auth_logo_signup)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthIntro("함께 달릴 준비가 됐나요?", "기본 정보를 입력하고 RunTrack을 둘러보세요.")
            AuthTabs(true, { password = ""; confirmation = ""; onBack() }, {})
            InformationNote("화면 체험용 · 실제 계정은 생성되지 않아요.")
            RunTrackCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("아이디")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        AuthInput(username, { username = it; checkedId = null }, "아이디 입력", "signupUsername",
                            errors["username"] ?: if (idCheckRequested) AuthValidation.usernameError(username) else null,
                            requests.getValue("username"), modifier = Modifier.weight(1f), description = "아이디")
                        OutlinedButton(onClick = {
                            idCheckRequested = true
                            if (AuthValidation.usernameError(username) == null) checkedId = username.trim()
                            else requests.getValue("username").requestFocus()
                        }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.width(82.dp).height(56.dp).testTag("checkUsername")) {
                            Text("중복확인", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (checkedId == username.trim()) Text("아이디 입력을 확인했어요. 실제 중복 확인은 계정 서비스 연결 후 가능해요.",
                        style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Muted)
                }
                SignupField("비밀번호", password, { password = it }, "8자 이상 입력", "signupPassword", errors["password"], requests.getValue("password"), password = true)
                SignupField("비밀번호 확인", confirmation, { confirmation = it }, "한 번 더 입력하세요", "signupConfirmation", errors["confirmation"], requests.getValue("confirmation"), password = true)
                SignupField("이름 (한글/영문, 최대 50자)", name, { name = it }, "이름 입력", "signupName", errors["name"], requests.getValue("name"))
                SignupField("체중 (kg, 숫자만)", weight, { weight = it }, "예: 70.5", "signupWeight", errors["weight"], requests.getValue("weight"), keyboard = KeyboardType.Decimal, ime = ImeAction.Done, done = submit)
            }
            TextButton(onClick = { password = ""; confirmation = ""; onBack() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("로그인으로 돌아가기") }
        }
        Surface(shadowElevation = 6.dp, color = RunTrackColors.Surface) {
            Box(Modifier.padding(20.dp)) { PrimaryActionButton("가입완료", submit, Modifier.testTag("signupSubmit")) }
        }
    }
}

@Composable
private fun AuthBrand(logo: Int) {
    Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(RunTrackColors.SoftAccent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Image(painterResource(logo), null, Modifier.size(30.dp), contentScale = ContentScale.Fit)
        }
        Spacer(Modifier.width(10.dp))
        Text("RunTrack", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        Badge("체험 모드")
    }
}

@Composable
private fun AuthIntro(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Muted)
    }
}

@Composable
private fun AuthTabs(signup: Boolean, login: () -> Unit, join: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(RunTrackColors.Selection, RoundedCornerShape(16.dp)).padding(4.dp)) {
        listOf("로그인", "회원가입").forEachIndexed { index, label ->
            val active = (index == 1) == signup
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                .background(if (active) RunTrackColors.Surface else Color.Transparent)
                .semantics { selected = active }.clickable(role = Role.Tab, onClick = if (index == 0) login else join)
                .testTag(if (index == 1 && !signup) "openSignup" else "authTab$index"), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) RunTrackColors.Accent else RunTrackColors.Muted)
            }
        }
    }
}

@Composable
private fun SignupField(label: String, value: String, change: (String) -> Unit, hint: String, tag: String,
                        error: String?, requester: FocusRequester, password: Boolean = false,
                        keyboard: KeyboardType = KeyboardType.Text, ime: ImeAction = ImeAction.Next, done: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        AuthInput(value, change, hint, tag, error, requester, password = password, keyboard = keyboard, ime = ime, done = done, description = label)
    }
}

@Composable
private fun AuthInput(value: String, change: (String) -> Unit, hint: String, tag: String, error: String?,
                      requester: FocusRequester, modifier: Modifier = Modifier, password: Boolean = false,
                      keyboard: KeyboardType = KeyboardType.Text, ime: ImeAction = ImeAction.Next, done: (() -> Unit)? = null,
                      description: String = hint) {
    val focus = LocalFocusManager.current
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(value, change, modifier.fillMaxWidth().focusRequester(requester).testTag(tag).semantics { contentDescription = description },
        singleLine = true, placeholder = if (hint.isNotEmpty()) {{ Text(hint, style = MaterialTheme.typography.bodyMedium) }} else null,
        leadingIcon = when (tag) {
            "loginUsername" -> {{ Icon(Icons.Outlined.PersonOutline, null, Modifier.size(20.dp), tint = RunTrackColors.Muted) }}
            "loginPassword" -> {{ Icon(Icons.Outlined.Lock, null, Modifier.size(20.dp), tint = RunTrackColors.Muted) }}
            else -> null
        },
        trailingIcon = if (password) {{ IconButton(onClick = { revealed = !revealed }) {
            Icon(if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                if (revealed) "비밀번호 숨기기" else "비밀번호 표시", Modifier.size(20.dp), tint = RunTrackColors.Muted)
        } }} else null,
        shape = RoundedCornerShape(12.dp), isError = error != null,
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = RunTrackColors.Field,
            unfocusedContainerColor = RunTrackColors.Field, errorContainerColor = RunTrackColors.Field),
        supportingText = if (error != null) {{ Text(error) }} else null,
        visualTransformation = if (password && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard, imeAction = ime),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }, onDone = { done?.invoke() ?: focus.clearFocus() }))
}
