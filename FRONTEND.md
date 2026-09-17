# 📱 RunTrack Frontend 개발 가이드

**프로젝트**: RunTrack - AI 기반 맞춤형 러닝 조언 앱  
**담당**: Frontend Developer (Kotlin/Jetpack Compose)  
**기간**: 10주 (9월 17일 ~ 11월 마지막주)  
**참고**: Runday 앱의 UX/UI 디자인 패턴

---

## 📋 목차

1. [개발 일정 및 마일스톤](#개발-일정-및-마일스톤)
2. [기술 스택](#기술-스택)
3. [프로젝트 구조](#프로젝트-구조)
4. [UI 화면 설계](#ui-화면-설계)
5. [디자인 가이드라인](#디자인-가이드라인)
6. [개발 체크리스트](#개발-체크리스트)
7. [테스트 및 배포](#테스트-및-배포)

---

## 📅 개발 일정 및 마일스톤

| 주차 | 일정 | 목표 | 산출물 |
|------|------|------|--------|
| **1주** | 9/17-9/23 | 화면 설계 + Figma 와이어프레임 | 6개 화면 와이어프레임 완성 |
| **2주** | 9/24-9/30 | 로그인/홈 화면 개발 | 로그인, 홈 UI 구현 (MVVM 기본) |
| **3주** | 10/1-10/7 | 기록 리스트/추가 화면 | 러닝 기록 화면 2개 구현 |
| **4주** | 10/8-10/14 | 신발 관리 + 추천 화면 | 신발, 추천 화면 구현 |
| **5주** | 10/15-10/21 | API 연동 + 성능 최적화 | Backend API 통합 |
| **6-7주** | 10/22-11/4 | 버그 수정 + UI 개선 | 안정성 향상 |
| **8-9주** | 11/5-11/18 | 최종 점검 + 추가 기능 | 완성도 향상 |
| **10주** | 11/19-11/25 | 발표 준비 | 발표 자료 준비 |

---

## 🛠️ 기술 스택

### **언어 및 프레임워크**
- **Language**: Kotlin (Java 100% 호환, null-safe)
- **UI Framework**: Jetpack Compose (선언형 UI)
- **Architecture**: MVVM + Repository Pattern
- **Target**: Android API 24 ~ 34

### **핵심 라이브러리**

```gradle
// UI & Compose
implementation 'androidx.compose.ui:ui:1.5.0'
implementation 'androidx.compose.material3:material3:1.1.0'
implementation 'androidx.compose.runtime:runtime:1.5.0'

// Navigation
implementation 'androidx.navigation:navigation-compose:2.7.0'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:okhttp:4.11.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

// Async
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'

// Dependency Injection
implementation 'com.google.dagger:hilt-android:2.47'
kapt 'com.google.dagger:hilt-compiler:2.47'
implementation 'androidx.hilt:hilt-navigation-compose:1.1.0'

// ViewModel
implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1'

// Local Database (선택사항)
implementation 'androidx.room:room-runtime:2.5.2'
kapt 'androidx.room:room-compiler:2.5.2'

// Testing
testImplementation 'junit:junit:4.13.2'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
```

---

## 📁 프로젝트 구조

```
android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── kotlin/com/runtrack/
│               ├── MainActivity.kt
│               ├── RunTrackApp.kt
│               │
│               ├── di/                    # Dependency Injection
│               │   └── AppModule.kt
│               │
│               ├── data/                  # Data Layer
│               │   ├── api/
│               │   │   ├── ApiServices.kt
│               │   │   └── RetrofitClient.kt
│               │   ├── models/            # API Response Models
│               │   │   ├── User.kt
│               │   │   ├── Shoe.kt
│               │   │   └── RunningRecord.kt
│               │   └── repository/
│               │       ├── UserRepository.kt
│               │       ├── ShoeRepository.kt
│               │       └── RunningRepository.kt
│               │
│               ├── ui/                    # UI Layer (MVVM)
│               │   ├── screens/
│               │   │   ├── LoginScreen.kt
│               │   │   ├── HomeScreen.kt
│               │   │   ├── RunningListScreen.kt
│               │   │   ├── RunningDetailScreen.kt
│               │   │   ├── ShoeManagementScreen.kt
│               │   │   └── RecommendationScreen.kt
│               │   ├── viewmodels/
│               │   │   ├── AuthViewModel.kt
│               │   │   ├── HomeViewModel.kt
│               │   │   ├── RunningViewModel.kt
│               │   │   └── ShoeViewModel.kt
│               │   ├── components/        # Reusable Components
│               │   │   ├── RunningRecordCard.kt
│               │   │   ├── ShoeCard.kt
│               │   │   ├── StatisticsChart.kt
│               │   │   └── WeatherWidget.kt
│               │   ├── navigation/
│               │   │   └── NavGraph.kt
│               │   └── theme/
│               │       ├── Theme.kt
│               │       ├── Color.kt
│               │       └── Type.kt
│               │
│               └── util/
│                   ├── Constants.kt
│                   └── Extensions.kt

└── build.gradle.kts
```

---

## 🎨 UI 화면 설계

### **Figma 디자인 원칙 (Runday 참고)**

Runday 앱의 우수한 UX 특징:
- ✅ 간결하고 직관적인 레이아웃
- ✅ 명확한 정보 계층 (주요 정보 우선)
- ✅ 부드러운 색상과 애니메이션
- ✅ 빠른 데이터 입력 UX
- ✅ 실시간 피드백 및 통계 시각화

---

## 화면 1️⃣: 로그인 / 회원가입 화면

### **UI 구조**

```
┌─────────────────────────────┐
│                             │
│   🏃 RunTrack 로고          │
│                             │
├─────────────────────────────┤
│  ●●●●● 로그인  ○○○○○ 가입  │  ← 탭 네비게이션
├─────────────────────────────┤
│                             │
│  📧 이메일                  │
│  ┌──────────────────────┐   │
│  │ user@example.com     │   │
│  └──────────────────────┘   │
│                             │
│  🔐 비밀번호                │
│  ┌──────────────────────┐   │
│  │ ••••••••••           │   │
│  └──────────────────────┘   │
│                             │
│  ┌──────────────────────┐   │
│  │   로그인             │   │
│  └──────────────────────┘   │
│                             │
│  비밀번호 찾기   회원가입    │
│                             │
└─────────────────────────────┘
```

### **회원가입 추가 필드**

```
회원가입 탭 클릭 시:
├─ 이메일 (검증: 중복 체크)
├─ 비밀번호 (조건: 8자 이상, 특수문자 포함)
├─ 비밀번호 확인 (일치 검증)
├─ 이름 (한글/영문, 최대 50자)
├─ 체중 (kg, 숫자만)
├─ 신장 (cm, 숫자만)
└─ [가입 완료] 버튼

성공 → HomeScreen으로 자동 이동
```

### **개발 항목**

- [ ] **LoginScreen.kt**: 로그인 UI + 입력 검증
- [ ] **SignupScreen.kt**: 회원가입 UI + 실시간 검증
- [ ] **AuthViewModel.kt**: 로그인/가입 로직
- [ ] **TokenManager**: JWT 토큰 저장/관리

---

## 화면 2️⃣: 홈 화면 (Main Dashboard)

### **UI 구조**

```
┌─────────────────────────────┐
│ 👤 김철수         ⚙️ 설정  │
├─────────────────────────────┤
│                             │
│ 📊 이번 주 통계              │
│ ┌──────────────────────┐   │
│ │ Mo Tu We Th Fr Sa Su │   │ ← 막대 그래프
│ │ ██ ░░ ██ ░░ ██ ░░ ██ │   │
│ │  5  0  3  0 10  0  5 │   │ (km)
│ └──────────────────────┘   │
│ 총: 23.5km | 평균 페이스: 6:12/km
│                             │
│ ─────────────────────────── │
│                             │
│ 🤖 AI 추천 신발              │
│ ┌──────────────────────┐   │
│ │ Nike Air Zoom        │   │
│ │ Pegasus 41           │   │
│ │                      │   │
│ │ "당신의 패턴에      │   │
│ │  최적의 신발입니다" │   │
│ │ [자세히 보기] ▶      │   │
│ └──────────────────────┘   │
│                             │
│ 🌤️  오늘 기상               │
│ 22°C | 습도 65% | 풍속 5m/s │
│ "좋은 날씨입니다!"           │
│                             │
├─────────────────────────────┤
│ [➕ 기록 추가]               │
│ [📋 기록 보기]               │
│ [👟 신발 관리]               │
└─────────────────────────────┘
```

### **Runday 디자인 특징**

- ✅ **카드 기반 레이아웃**: 각 섹션을 카드로 분리
- ✅ **색상 강조**: 중요 정보는 컬러풀하게 표현
- ✅ **빠른 액션**: 메인 화면에서 주요 기능 접근 가능
- ✅ **애니메이션**: 수치 변경 시 부드러운 애니메이션

### **개발 항목**

- [ ] **HomeScreen.kt**: 메인 대시보드 UI
- [ ] **StatisticsChart.kt**: 주간 통계 막대 그래프
- [ ] **WeatherWidget.kt**: 기상 정보 위젯
- [ ] **RecommendationCard.kt**: AI 추천 카드
- [ ] **HomeViewModel.kt**: 데이터 조회 및 통계 계산

---

## 화면 3️⃣: 러닝 기록 리스트 화면

### **UI 구조**

```
┌─────────────────────────────┐
│ 러닝 기록        🔍 검색     │
│                 📋 필터      │
├─────────────────────────────┤
│                             │
│ 2026-09-17 (수요일)         │
│ ┌──────────────────────┐   │
│ │ 🏃 5.20km            │   │
│ │ ⏱️ 32분              │   │
│ │ 👟 Nike Pegasus     │   │
│ │ ⏱️ 페이스: 6:09/km  │   │
│ │ 🌡️ 22°C             │   │
│ │                      │   │
│ │ [편집] [삭제]        │   │
│ └──────────────────────┘   │
│                             │
│ 2026-09-16 (화요일)         │
│ ┌──────────────────────┐   │
│ │ 🏃 10.00km           │   │
│ │ ⏱️ 65분              │   │
│ │ 👟 Nike Vaporfly    │   │
│ │ ⏱️ 페이스: 6:30/km  │   │
│ │ 🌡️ 18°C             │   │
│ │ [편집] [삭제]        │   │
│ └──────────────────────┘   │
│                             │
│ 2026-09-15 (목요일)         │
│ ┌──────────────────────┐   │
│ │ 🏃 3.50km            │   │
│ └──────────────────────┘   │
│                             │
│ [스크롤 로드]                │
└─────────────────────────────┘
```

### **필터 기능**

```
필터 클릭 → BottomSheet
├─ 날짜 범위 선택 (Start ~ End)
├─ 신발 선택 (MultiSelect)
├─ 페이스 범위 (6:00~7:00)
└─ [적용] [초기화]
```

### **개발 항목**

- [ ] **RunningListScreen.kt**: 리스트 UI + 페이지네이션
- [ ] **RunningRecordCard.kt**: 기록 카드 컴포넌트
- [ ] **FilterBottomSheet.kt**: 필터 UI
- [ ] **RunningViewModel.kt**: 리스트 로드 및 필터링

---

## 화면 4️⃣: 러닝 기록 추가/수정 화면

### **UI 구조**

```
┌─────────────────────────────┐
│ 러닝 기록 추가  [X 닫기]    │
├─────────────────────────────┤
│                             │
│ 📅 날짜                     │
│ ┌──────────────────────┐   │
│ │ 2026-09-17           │   │ ← DatePicker
│ └──────────────────────┘   │
│                             │
│ ⏱️ 시간 (분)                │
│ ┌──────────────────────┐   │
│ │ 32                   │   │
│ └──────────────────────┘   │
│                             │
│ 📍 거리 (km)                │
│ ┌──────────────────────┐   │
│ │ 5.20                 │   │
│ └──────────────────────┘   │
│                             │
│ 👟 신발 선택                │
│ ┌──────────────────────┐   │
│ │ ▼ Nike Pegasus       │   │
│ └──────────────────────┘   │
│                             │
│ 🌡️ 기상 정보 (자동 조회)    │
│ 온도: 22°C                  │
│ 습도: 65%                   │
│ 풍속: 5 m/s                 │
│                             │
│ 😊 체감                     │
│ ┌──────────────────────┐   │
│ │ ▼ Good               │   │
│ └──────────────────────┘   │
│ (Great / Good / Normal / Tired / Bad)
│                             │
│ 📝 메모                     │
│ ┌──────────────────────┐   │
│ │ (선택사항) ...       │   │
│ └──────────────────────┘   │
│                             │
│ ┌──────────────────────┐   │
│ │   저장               │   │
│ └──────────────────────┘   │
│                             │
└─────────────────────────────┘
```

### **Runday 특징 적용**

- ✅ **빠른 입력**: 최소한의 필드만 필수 입력
- ✅ **자동 채우기**: 기상 정보 자동 조회 (위치 권한)
- ✅ **입력 검증**: 실시간 유효성 검사 (거리 > 0, 시간 > 0)
- ✅ **드롭다운 UI**: 신발 선택, 체감 선택

### **개발 항목**

- [ ] **AddRunningScreen.kt**: 기록 추가 화면
- [ ] **EditRunningScreen.kt**: 기록 편집 화면
- [ ] **DatePicker 통합**: 날짜 선택 UI
- [ ] **TimePicker 통합**: 시간 선택 UI
- [ ] **WeatherAPI 호출**: 기상 정보 자동 조회
- [ ] **입력 검증 로직**: Validation helper

---

## 화면 5️⃣: 신발 관리 화면

### **UI 구조**

```
┌─────────────────────────────┐
│ 신발 관리        [➕ 추가]   │
├─────────────────────────────┤
│                             │
│ Nike Air Zoom Pegasus       │
│ ┌──────────────────────┐   │
│ │ 👟 [신발 이미지]     │   │
│ │ ────────────────────  │   │
│ │ 브랜드: Nike          │   │
│ │ 모델: Air Zoom P...   │   │
│ │ 사이즈: 42            │   │
│ │ 색상: Black           │   │
│ │ 구매: 2026-01-15      │   │
│ │ ────────────────────  │   │
│ │ 누적 주행: 245.50km   │   │
│ │ 상태: ⚠️ 교체 권장    │   │
│ │ (수명 1000km)         │   │
│ │                       │   │
│ │ [편집] [삭제]         │   │
│ └──────────────────────┘   │
│                             │
│ Nike Vaporfly               │
│ ┌──────────────────────┐   │
│ │ 👟 [신발 이미지]     │   │
│ │ ────────────────────  │   │
│ │ 모델: Vaporfly       │   │
│ │ 누적 주행: 120.75km   │   │
│ │ 상태: ✅ 정상         │   │
│ │                       │   │
│ │ [편집] [삭제]         │   │
│ └──────────────────────┘   │
│                             │
│ Adidas Ultraboost           │
│ ┌──────────────────────┐   │
│ │ 누적 주행: 85.20km    │   │
│ │ [편집] [삭제]         │   │
│ └──────────────────────┘   │
│                             │
│ [스크롤]                    │
└─────────────────────────────┘
```

### **신발 추가/수정 다이얼로그**

```
┌──────────────────────┐
│ 신발 추가            │
├──────────────────────┤
│ 브랜드                │
│ ┌────────────────┐   │
│ │ Nike           │   │
│ └────────────────┘   │
│                      │
│ 모델명                │
│ ┌────────────────┐   │
│ │ Air Zoom...    │   │
│ └────────────────┘   │
│                      │
│ 사이즈                │
│ ┌────────────────┐   │
│ │ 42             │   │
│ └────────────────┘   │
│                      │
│ 색상                 │
│ ┌────────────────┐   │
│ │ Black          │   │
│ └────────────────┘   │
│                      │
│ 구매 날짜              │
│ ┌────────────────┐   │
│ │ 2026-01-15     │   │
│ └────────────────┘   │
│                      │
│ [저장] [취소]        │
└──────────────────────┘
```

### **개발 항목**

- [ ] **ShoeManagementScreen.kt**: 신발 리스트 UI
- [ ] **ShoeCard.kt**: 신발 카드 컴포넌트
- [ ] **AddShoeDialog.kt**: 신발 추가/수정 다이얼로그
- [ ] **ShoeViewModel.kt**: 신발 CRUD 로직
- [ ] **상태 표시**: 교체 여부 알림 (200px > 색상 변경)

---

## 화면 6️⃣: AI 신발 추천 화면

### **UI 구조**

```
┌─────────────────────────────┐
│ 🤖 신발 추천     [새로고침]  │
├─────────────────────────────┤
│                             │
│ 1순위                       │
│ ┌──────────────────────┐   │
│ │ 👟 Nike Air Zoom     │   │
│ │    Pegasus 41        │   │
│ │                      │   │
│ │ 💡 추천 이유:        │   │
│ │ "당신의 신체 정보    │   │
│ │  (75kg, 180cm)와    │   │
│ │  최근 러닝 패턴      │   │
│ │  (주 3회, 평균 5km) │   │
│ │  을 고려할 때,       │   │
│ │  가벼우면서 충분한   │   │
│ │  쿠셔닝이 최적입니다"│   │
│ │                      │   │
│ │ 💰 가격대: $100-120  │   │
│ │ 👍 장점:             │   │
│ │    · 가벼움          │   │
│ │    · 통풍성          │   │
│ │    · 내구성          │   │
│ │ 👎 단점:             │   │
│ │    · 비용            │   │
│ │                      │   │
│ │ [더보기] [구매 가능] │   │
│ └──────────────────────┘   │
│                             │
│ 2순위                       │
│ ┌──────────────────────┐   │
│ │ 👟 Adidas Ultraboost │   │
│ │ 23                   │   │
│ │                      │   │
│ │ 💡 추천 이유: ...    │   │
│ │ [더보기]             │   │
│ └──────────────────────┘   │
│                             │
│ 3순위                       │
│ ┌──────────────────────┐   │
│ │ 👟 Asics GelNimbus   │   │
│ │ 25                   │   │
│ └──────────────────────┘   │
│                             │
│ [스크롤]                    │
└─────────────────────────────┘
```

### **개발 항목**

- [ ] **RecommendationScreen.kt**: 추천 리스트 UI
- [ ] **RecommendationCard.kt**: 추천 카드 컴포넌트
- [ ] **RecommendationViewModel.kt**: AI API 호출
- [ ] **새로고침 버튼**: Pull-to-refresh 기능

---

## 🎨 디자인 가이드라인

### **색상 팔레트** (Material Design 3 기반)

```kotlin
// Primary Colors (RunTrack 브랜드)
val Primary = Color(0xFF1F77D2)          // 파란색 (주요 액션)
val Secondary = Color(0xFF4CAF50)        // 초록색 (성공/완료)
val Tertiary = Color(0xFFFFA500)         // 주황색 (경고/권장)

// Status Colors
val SuccessGreen = Color(0xFF4CAF50)     // 성공
val WarningOrange = Color(0xFFFFA500)    // 경고 (교체 권장)
val ErrorRed = Color(0xFFEF5350)         // 에러
val InfoBlue = Color(0xFF2196F3)         // 정보

// Background & Surface
val Background = Color(0xFFFAFAFA)       // 밝은 배경
val Surface = Color(0xFFFFFFFF)          // 카드 배경
val SurfaceVariant = Color(0xFFF5F5F5)   // 약한 배경

// Text Colors
val OnBackground = Color(0xFF1C1B1F)     // 주요 텍스트
val OnSurface = Color(0xFF49454E)        // 보조 텍스트
val OnSurfaceVariant = Color(0xFF79747E) // 약한 텍스트
```

### **Typography**

```kotlin
val RunTrackTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    )
)
```

### **간격 및 크기**

```kotlin
object Spacing {
    val xs = 4.dp      // 최소 간격
    val sm = 8.dp      // 작은 간격
    val md = 16.dp     // 표준 간격
    val lg = 24.dp     // 큰 간격
    val xl = 32.dp     // 매우 큰 간격
}

object CornerRadius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xl = 28.dp
}

// 카드 높이
val CardHeight = 120.dp
val ChartHeight = 180.dp
```

### **애니메이션**

```kotlin
// 기본 전환 애니메이션
const val DefaultAnimationDuration = 300

// 로드 중 애니메이션
@Composable
fun LoadingAnimation() {
    // 로딩 스피너
}

// 수치 변경 애니메이션
val animatedDistance = animateFloatAsState(
    targetValue = distance.toFloat(),
    animationSpec = tween(durationMillis = 500)
)
```

---

## 📱 컴포넌트 라이브러리

### **재사용 가능한 컴포넌트**

```kotlin
// 1. 버튼 스타일
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)

// 2. 카드 컴포넌트
@Composable
fun RunTrackCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)

// 3. 입력 필드
@Composable
fun RunTrackTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
)

// 4. 통계 차트
@Composable
fun WeeklyChart(
    weeklyData: List<Float>,
    modifier: Modifier = Modifier
)

// 5. 기록 카드
@Composable
fun RunningRecordCard(
    record: RunningRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
)
```

---

## 🔗 API 연동

### **Retrofit 설정**

```kotlin
// RetrofitClient.kt
object RetrofitClient {
    private const val BASE_URL = "http://your-backend-url/api/"
    
    val instance: ApiServices by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(ApiServices::class.java)
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(tokenInterceptor) // JWT 토큰 자동 추가
        .build()
}

// ApiServices.kt
interface ApiServices {
    // Auth
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("users/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse
    
    // User
    @GET("users/me")
    suspend fun getCurrentUser(): UserResponse
    
    // Running
    @GET("running")
    suspend fun getRunningRecords(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): List<RunningRecord>
    
    @POST("running")
    suspend fun addRunningRecord(@Body record: RunningRecord): RunningRecord
    
    @GET("running/statistics/weekly")
    suspend fun getWeeklyStats(): WeeklyStats
    
    // Recommendations
    @POST("recommendations/shoes")
    suspend fun getShoeRecommendations(): List<ShoeRecommendation>
    
    // Weather
    @GET("weather/advice")
    suspend fun getWeatherAdvice(): WeatherAdvice
}
```

### **ViewModel 패턴**

```kotlin
// HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: RunningRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            try {
                val weeklyStats = repository.getWeeklyStats()
                val recommendations = repository.getShoeRecommendations()
                val weather = repository.getWeatherAdvice()
                
                _uiState.value = UiState.Success(
                    weeklyStats = weeklyStats,
                    recommendations = recommendations,
                    weather = weather
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

---

## ✅ 개발 체크리스트

### **1주차: 와이어프레임 설계**
- [ ] Figma 프로젝트 생성 및 팀 초대
- [ ] 6개 화면 와이어프레임 완성
- [ ] 색상/타이포그래피 정의
- [ ] 프로토타입 링크 팀에 공유
- [ ] 코드 검토 및 피드백 수집

### **2주차: 로그인/홈 화면**
- [ ] Compose 기본 프로젝트 구조 생성
- [ ] LoginScreen UI 구현
- [ ] SignupScreen UI 구현
- [ ] 입력 검증 로직
- [ ] AuthViewModel 구현
- [ ] 토큰 저장 및 관리

### **3주차: 기록 리스트/추가**
- [ ] RunningListScreen UI
- [ ] RunningRecordCard 컴포넌트
- [ ] 기록 추가 UI
- [ ] DatePicker/TimePicker 통합
- [ ] 기상 정보 자동 조회
- [ ] 기본 데이터 바인딩

### **4주차: 신발/추천 화면**
- [ ] ShoeManagementScreen UI
- [ ] ShoeCard 컴포넌트
- [ ] 신발 추가/수정/삭제 UI
- [ ] RecommendationScreen UI
- [ ] 카드 레이아웃

### **5주차: API 연동 + 최적화**
- [ ] Retrofit 설정 완료
- [ ] 모든 API 엔드포인트 연동
- [ ] ViewModel 데이터 바인딩
- [ ] 로딩 상태 표시
- [ ] 에러 처리

### **6-7주차: 버그 수정 및 UI 개선**
- [ ] UI 미세조정
- [ ] 애니메이션 추가
- [ ] 성능 최적화
- [ ] 네트워크 에러 처리
- [ ] 사용자 피드백 반영

### **8-9주차: 최종 점검**
- [ ] UI 테스트 (기기별 호환성)
- [ ] 오프라인 모드 테스트
- [ ] 성능 모니터링
- [ ] 보안 검토

### **10주차: 발표 준비**
- [ ] 사용자 가이드 작성
- [ ] 스크린샷 촬영
- [ ] 데모 비디오 녹화
- [ ] 발표 자료 준비

---

## 🧪 테스트 및 배포

### **UI 테스트**

```kotlin
// RunningListScreenTest.kt
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun testRunningListDisplaysRecords() {
    composeTestRule.setContent {
        RunningListScreen(
            records = listOf(
                RunningRecord(id = 1, distance = 5.2, duration = 32, ...),
                RunningRecord(id = 2, distance = 10.0, duration = 65, ...)
            )
        )
    }
    
    composeTestRule.onNodeWithText("5.20km").assertIsDisplayed()
    composeTestRule.onNodeWithText("10.00km").assertIsDisplayed()
}
```

### **배포 준비**

```gradle
android {
    compileSdk 34
    
    defaultConfig {
        targetSdk 34
        minSdk 24
    }
    
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile(...)
            signingConfig signingConfigs.release
        }
    }
}
```

---

## 📞 문의 및 참고

**Backend API 명세**: `backend/docs/API_SPECIFICATION.md`  
**프로젝트 일정**: `SETUP.md`  
**팀 역할**: `ROLES.md`  
**GitHub**: https://github.com/jae827kim/RunTrack

---

**작성일**: 2026년 9월 17일  
**최종 업데이트**: 2026년 9월 17일  
**Frontend 담당**: Developer

