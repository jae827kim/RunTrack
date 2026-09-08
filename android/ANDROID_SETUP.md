# RunTrack 안드로이드 앱 가이드

## 📋 개요

RunTrack 안드로이드 앱은 **Kotlin**과 **Jetpack Compose**로 구축된 현대적인 모바일 애플리케이션입니다.

- **언어**: Kotlin
- **API 레벨**: 24+ (Android 7.0+)
- **UI 프레임워크**: Jetpack Compose
- **아키텍처**: MVVM + Repository Pattern
- **DI**: Hilt
- **HTTP 클라이언트**: Retrofit + OkHttp

---

## 🚀 빠른 시작

### 1. 사전 요구사항

- **Android Studio**: 최신 버전 (Hedgehog 2023.1.1+)
- **JDK**: 17 이상
- **SDK**: API 34 타겟, API 24 최소

### 2. 프로젝트 열기

```bash
# 프로젝트 클론
git clone https://github.com/your-repo/runtrack.git
cd runtrack/android

# Android Studio에서 열기
# File > Open > android 폴더 선택
```

### 3. 환경 설정

**local.properties 파일 생성:**
```properties
sdk.dir=/path/to/your/android/sdk
```

**build.gradle.kts에서 API 키 설정:**
```kotlin
buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"YOUR_KEY_HERE\"")
buildConfigField("String", "API_BASE_URL", "\"http://localhost:8000/\"")
```

### 4. 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew build

# 에뮬레이터에 설치
./gradlew installDebug

# 또는 Android Studio의 Run 버튼 클릭
```

---

## 📁 프로젝트 구조

```
android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/runtrack/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── RunTrackApp.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   ├── home/
│   │   │   │   │   ├── shoes/
│   │   │   │   │   ├── running/
│   │   │   │   │   └── recommendations/
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/
│   │   │   │   │   │   ├── ApiServices.kt
│   │   │   │   │   │   └── RetrofitClient.kt
│   │   │   │   │   ├── models/
│   │   │   │   │   │   ├── User.kt
│   │   │   │   │   │   ├── Shoe.kt
│   │   │   │   │   │   └── RunningRecord.kt
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── ShoeRepository.kt
│   │   │   │   │       └── ...
│   │   │   │   └── di/
│   │   │   │       └── AppModule.kt
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── colors.xml
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   └── test/ & androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 🔗 API 연동

### Retrofit 서비스 설정

```kotlin
// RetrofitClient.kt에서 자동으로 초기화됨
val userService = RetrofitClient.userService
val shoeService = RetrofitClient.shoeService
val runningService = RetrofitClient.runningService
val weatherService = RetrofitClient.weatherService
```

### API 호출 예시

```kotlin
// Repository에서
suspend fun getShoes(): Result<List<Shoe>> = try {
    val response = shoeService.getShoes()
    if (response.isSuccessful) {
        Result.success(response.body() ?: emptyList())
    } else {
        Result.failure(Exception("실패: ${response.message()}"))
    }
} catch (e: Exception) {
    Result.failure(e)
}

// ViewModel에서
viewModelScope.launch {
    val result = repository.getShoes()
    result.onSuccess { shoes ->
        _uiState.value = uiState.value.copy(shoes = shoes)
    }.onFailure { error ->
        // 에러 처리
    }
}
```

---

## 🎨 Compose 화면 구축

### 기본 구조

```kotlin
@Composable
fun ShoeListScreen(
    viewModel: ShoeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is ShoeUiState.Loading -> LoadingScreen()
        is ShoeUiState.Success -> SuccessScreen(uiState.shoes)
        is ShoeUiState.Error -> ErrorScreen(uiState.message)
    }
}
```

### 상태 관리

```kotlin
data class ShoeUiState(
    val shoes: List<Shoe> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ShoeViewModel @Inject constructor(
    private val repository: ShoeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShoeUiState())
    val uiState: StateFlow<ShoeUiState> = _uiState.asStateFlow()

    fun loadShoes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.getShoes()
            // ...
        }
    }
}
```

---

## 📍 GPS & 위치 권한

### 필요한 권한

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

### 런타임 권한 요청

```kotlin
// Android 6.0+ (API 23+)에서는 런타임 권한 필요
val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        // 위치 서비스 시작
    }
}

Button(onClick = {
    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
})
```

---

## 🧪 테스트

### 유닛 테스트

```bash
./gradlew test
```

### 계측 테스트 (Device/Emulator)

```bash
./gradlew connectedAndroidTest
```

### 테스트 작성

```kotlin
@RunWith(AndroidRunner::class)
class ShoeRepositoryTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: ShoeRepository

    @Before
    fun setUp() {
        repository = ShoeRepository()
    }

    @Test
    fun testGetShoes() = runTest {
        val result = repository.getShoes()
        assert(result.isSuccess)
    }
}
```

---

## 🔐 토큰 관리

### 토큰 저장 (DataStore)

```kotlin
private val tokenDataStore = context.createDataStore("token_store")

suspend fun saveToken(token: String) {
    tokenDataStore.edit { preferences ->
        preferences[stringPreferencesKey("access_token")] = token
    }
}

fun getToken(): Flow<String?> = tokenDataStore.data.map { preferences ->
    preferences[stringPreferencesKey("access_token")]
}
```

### 토큰 인터셉터

```kotlin
httpClient.addInterceptor { chain ->
    val token = // DataStore에서 토큰 읽기
    val originalRequest = chain.request()
    val authorizedRequest = originalRequest.newBuilder()
        .addHeader("Authorization", "Bearer $token")
        .build()
    chain.proceed(authorizedRequest)
}
```

---

## 🐛 트러블슈팅

### "Cannot resolve symbol" 오류
```bash
# Gradle 동기화 재시도
File > Sync Now
또는
./gradlew sync
```

### 빌드 실패
```bash
# 클린 빌드
./gradlew clean build

# 캐시 초기화
./gradlew --stop
./gradlew build
```

### 에뮬레이터 연결 문제
```bash
# 에뮬레이터 재시작
emulator -avd emulator_name

# 또는 Android Studio의 Device Manager에서 재시작
```

---

## 📚 참고 링크

- [Jetpack Compose 공식 문서](https://developer.android.com/jetpack/compose)
- [Hilt 의존성 주입](https://developer.android.com/training/dependency-injection/hilt-android)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Retrofit](https://square.github.io/retrofit/)
- [Android 위치 권한](https://developer.android.com/training/location)

---

## 🤝 개발 팁

### 핫 리로드
Jetpack Compose는 코드 변경 시 자동으로 UI를 업데이트합니다.

### Logcat 필터링
```bash
# Timber 로그만 보기
adb logcat | grep "RunTrack"
```

### 메모리 프로파일링
- Android Studio > Profiler > Memory
- 메모리 누수 감지 및 최적화

---

**Happy Coding! 🎉**
