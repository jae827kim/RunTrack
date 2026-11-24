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
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.runtrack.data.local.LocalTrackStore
import com.runtrack.data.repository.LocalAdminTrackRepository
import com.runtrack.data.repository.RemoteTrackRepository

@Composable
fun RunTrackEntry(model: AuthViewModel = viewModel()) {
    val session by model.session.collectAsStateWithLifecycle()
    when {
        session != null -> {
            val signedIn = session!!
            val context = LocalContext.current.applicationContext
            val owner = remember(signedIn.instanceId) { object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            } }
            val factory = remember(signedIn.instanceId) {
                val repository = if (signedIn.isOfflineAdmin)
                    LocalAdminTrackRepository(LocalTrackStore(context, LocalAdminTrackRepository.PREFERENCES_NAME))
                else RemoteTrackRepository()
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        require(modelClass == TrackViewModel::class.java)
                        return TrackViewModel(repository) as T
                    }
                }
            }
            DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
            key(signedIn.instanceId) {
                TrackApp(vm = viewModel(viewModelStoreOwner = owner, factory = factory), onLogout = model::signOut,
                    isOfflineAdmin = signedIn.isOfflineAdmin)
            }
        }
        model.signingUp -> {
            BackHandler(enabled = !model.busy, onBack = model::showLogin)
            SignupScreen(busy = model.busy, serverError = model.error, onBack = model::showLogin, onComplete = model::register)
        }
        else -> LoginScreen(
            initialUsername = model.suggestedUsername,
            registrationReviewed = model.registrationReviewed,
            busy = model.busy, serverError = model.error,
            onContinue = model::login,
            onSignup = model::showSignup
        )
    }
}

@Composable
fun LoginScreen(initialUsername: String = "", registrationReviewed: Boolean = false,
                busy: Boolean = false, serverError: String? = null,
                onContinue: (String, String) -> Unit, onSignup: () -> Unit) {
    var username by rememberSaveable(initialUsername) { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val errors = if (submitted) AuthValidation.login(username, password) else emptyMap()
    val focus = LocalFocusManager.current
    val usernameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val submit: () -> Unit = {
        if (!busy) {
        submitted = true
        val result = AuthValidation.login(username, password)
        when {
            "username" in result -> usernameFocus.requestFocus()
            "password" in result -> passwordFocus.requestFocus()
            else -> { focus.clearFocus(); onContinue(username.trim(), password) }
        }
        }
    }
    Column(Modifier.fillMaxSize().background(RunTrackColors.Background).imePadding().testTag("loginScreen")) {
        AuthBrand(R.drawable.auth_logo_login)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthTabs(false, {}, { password = ""; onSignup() }, enabled = !busy)
            if (registrationReviewed) Text("회원가입이 완료됐습니다.", Modifier.testTag("registrationReviewed"),
                style = MaterialTheme.typography.bodyMedium, color = RunTrackColors.Accent)
            RunTrackCard {
                if (serverError != null) Text(serverError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("authError"), style = MaterialTheme.typography.bodyMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("아이디")
                    AuthInput(username, { username = it }, "", "loginUsername", errors["username"], usernameFocus, description = "아이디", enabled = !busy)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("비밀번호")
                    AuthInput(password, { password = it }, "", "loginPassword", errors["password"], passwordFocus,
                        password = true, ime = ImeAction.Done, done = submit, description = "비밀번호", enabled = !busy)
                }
                PrimaryActionButton("로그인", submit, Modifier.testTag("loginSubmit"), busy = busy)
            }
        }
    }
}

@Composable
fun SignupScreen(busy: Boolean = false, serverError: String? = null, onBack: () -> Unit, onComplete: (SignupInput) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var checkedId by remember { mutableStateOf<String?>(null) }
    var idCheckRequested by remember { mutableStateOf(false) }
    fun validate(input: SignupInput) = AuthValidation.signup(input).toMutableMap().apply {
        if (OfflineAdminAccount.isReserved(input.username)) put("username", "이미 사용 중인 아이디입니다.")
    }
    val errors = if (submitted) validate(SignupInput(username, password, confirmation, email, weight)) else emptyMap()
    val focus = LocalFocusManager.current
    val requests = remember { listOf("username", "password", "confirmation", "email", "weight").associateWith { FocusRequester() } }
    val submit: () -> Unit = {
        if (!busy) {
        submitted = true
        val input = SignupInput(username, password, confirmation, email, weight)
        val result = validate(input)
        if (result.isEmpty()) {
            focus.clearFocus(); onComplete(input)
        } else requests.getValue(result.keys.first()).requestFocus()
        }
    }
    Column(Modifier.fillMaxSize().background(RunTrackColors.Background).imePadding().testTag("signupScreen")) {
        AuthBrand(R.drawable.auth_logo_signup)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthTabs(true, { password = ""; confirmation = ""; onBack() }, {}, enabled = !busy)
            RunTrackCard {
                if (serverError != null) Text(serverError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("authError"), style = MaterialTheme.typography.bodyMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("아이디")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        AuthInput(username, { username = it; checkedId = null; idCheckRequested = false }, "", "signupUsername",
                            errors["username"] ?: if (idCheckRequested) AuthValidation.usernameError(username) else null,
                            requests.getValue("username"), modifier = Modifier.weight(1f), description = "아이디", enabled = !busy)
                        OutlinedButton(onClick = {
                            idCheckRequested = true
                            if (AuthValidation.usernameError(username) == null && !OfflineAdminAccount.isReserved(username)) checkedId = username.trim()
                            else requests.getValue("username").requestFocus()
                        }, enabled = !busy, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.width(82.dp).height(56.dp).testTag("checkUsername")) {
                            Text("입력 확인", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (idCheckRequested && OfflineAdminAccount.isReserved(username) && "username" !in errors) Text("이미 사용 중인 아이디입니다.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else if (checkedId == username.trim() && serverError == null) Text("아이디 형식이 올바릅니다. 중복 여부는 가입 시 확인합니다.",
                        modifier = Modifier.testTag("usernameFormatValid"), style = MaterialTheme.typography.bodySmall, color = RunTrackColors.Accent)
                }
                SignupField("비밀번호", password, { password = it }, "8자 이상 입력", "signupPassword", errors["password"], requests.getValue("password"), password = true, enabled = !busy)
                SignupField("비밀번호 확인", confirmation, { confirmation = it }, "", "signupConfirmation", errors["confirmation"], requests.getValue("confirmation"), password = true, enabled = !busy)
                SignupField("이메일", email, { email = it }, "runner@example.com", "signupEmail", errors["email"], requests.getValue("email"), keyboard = KeyboardType.Email, enabled = !busy)
                SignupField("체중 (kg, 숫자만)", weight, { weight = it }, "예: 70.5", "signupWeight", errors["weight"], requests.getValue("weight"), keyboard = KeyboardType.Decimal, ime = ImeAction.Done, done = submit, enabled = !busy)
            }
        }
        Surface(shadowElevation = 6.dp, color = RunTrackColors.Surface) {
            Box(Modifier.padding(20.dp)) { PrimaryActionButton("가입완료", submit, Modifier.testTag("signupSubmit"), busy = busy) }
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
    }
}

@Composable
private fun AuthTabs(signup: Boolean, login: () -> Unit, join: () -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().background(RunTrackColors.Selection, RoundedCornerShape(16.dp)).padding(4.dp)) {
        listOf("로그인", "회원가입").forEachIndexed { index, label ->
            val active = (index == 1) == signup
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp))
                .background(if (active) RunTrackColors.Surface else Color.Transparent)
                .semantics { selected = active }.clickable(enabled = enabled, role = Role.Tab, onClick = if (index == 0) login else join)
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
                        keyboard: KeyboardType = KeyboardType.Text, ime: ImeAction = ImeAction.Next, done: (() -> Unit)? = null, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        AuthInput(value, change, hint, tag, error, requester, password = password, keyboard = keyboard, ime = ime, done = done, description = label, enabled = enabled)
    }
}

@Composable
private fun AuthInput(value: String, change: (String) -> Unit, hint: String, tag: String, error: String?,
                      requester: FocusRequester, modifier: Modifier = Modifier, password: Boolean = false,
                      keyboard: KeyboardType = KeyboardType.Text, ime: ImeAction = ImeAction.Next, done: (() -> Unit)? = null,
                      description: String = hint, enabled: Boolean = true) {
    val focus = LocalFocusManager.current
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(value, change, modifier.fillMaxWidth().focusRequester(requester).testTag(tag).semantics { contentDescription = description },
        enabled = enabled, singleLine = true, placeholder = if (hint.isNotEmpty()) {{ Text(hint, style = MaterialTheme.typography.bodyMedium) }} else null,
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
