# 🗄️ RunTrack 데이터베이스 설계

**작성일**: 2026년 9월 17일  
**설계자**: PM  
**프로젝트**: AI 기반 맞춤형 러닝 조언 앱

---

## 📊 ERD (Entity Relationship Diagram)

```
┌─────────────────┐
│      User       │
├─────────────────┤
│ id (PK)         │
│ email (UNIQUE)  │
│ password_hash   │
│ name            │
│ weight_kg       │
│ height_cm       │
│ created_at      │
│ updated_at      │
└────────┬────────┘
         │
         │ 1:N (한 명이 여러 신발)
         ├─────────────────┬────────────────────┐
         │                 │                    │
    ┌────▼──────────────┐  │            ┌───────▼────────────┐
    │      Shoe         │  │            │  RunningRecord     │
    ├───────────────────┤  │            ├────────────────────┤
    │ id (PK)           │  │            │ id (PK)            │
    │ user_id (FK)      │  │            │ user_id (FK)       │
    │ brand             │  │            │ shoe_id (FK)       │
    │ model             │◄─┘            │ distance_km        │
    │ size              │  1:N          │ duration_minutes   │
    │ color             │  (한 신발로   │ date               │
    │ purchase_date     │   여러 기록)  │ temperature        │
    │ cumulative_km     │◄──────────────┤ humidity           │
    │ created_at        │               │ wind_speed         │
    │ updated_at        │               │ feeling            │
    └───────────────────┘               │ created_at         │
                                        │ updated_at         │
                                        └────────────────────┘
```

**관계 설명:**
- **User (1) ─── (N) Shoe**: 한 사용자는 여러 신발 소유 가능
- **User (1) ─── (N) RunningRecord**: 한 사용자는 여러 러닝 기록 입력 가능
- **Shoe (1) ─── (N) RunningRecord**: 한 신발로 여러 번의 러닝 가능 (누적 km 증가)

---

## 📋 테이블 명세

### **1. User 테이블 (사용자 정보)**

**목적**: 앱 사용자의 기본 정보 및 인증 관리

| 필드명 | 데이터타입 | 제약조건 | 설명 |
|--------|-----------|---------|------|
| `id` | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 사용자 고유 ID |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 이메일 (회원 식별) |
| `password_hash` | VARCHAR(255) | NOT NULL | bcrypt 해싱된 비밀번호 |
| `name` | VARCHAR(100) | NOT NULL | 사용자 이름 |
| `weight_kg` | DECIMAL(5,2) | NOT NULL | 체중 (kg 단위) |
| `height_cm` | INTEGER | NOT NULL | 신장 (cm 단위) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 계정 생성 시간 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 마지막 수정 시간 |

**인덱스**:
```sql
CREATE INDEX idx_user_email ON "user"(email);
```

**예시 데이터**:
```
id  | email              | name    | weight_kg | height_cm
1   | kim@example.com    | 김철수  | 75.50     | 180
2   | lee@example.com    | 이영미  | 62.30     | 165
```

---

### **2. Shoe 테이블 (신발 인벤토리)**

**목적**: 사용자가 소유한 러닝화 정보 및 누적 주행거리 관리

| 필드명 | 데이터타입 | 제약조건 | 설명 |
|--------|-----------|---------|------|
| `id` | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 신발 고유 ID |
| `user_id` | INTEGER | NOT NULL, FK → user(id) | 신발 소유 사용자 |
| `brand` | VARCHAR(100) | NOT NULL | 신발 브랜드 (예: Nike, Adidas) |
| `model` | VARCHAR(100) | NOT NULL | 신발 모델명 (예: Air Zoom Pegasus) |
| `size` | VARCHAR(10) | NOT NULL | 신발 사이즈 (예: 42, 280) |
| `color` | VARCHAR(50) | NULL | 신발 색상 (선택사항) |
| `purchase_date` | DATE | NOT NULL | 신발 구매 날짜 |
| `cumulative_km` | DECIMAL(8,2) | DEFAULT 0 | 신발의 누적 주행거리 |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 기록 생성 시간 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 마지막 수정 시간 |

**제약조건**:
```sql
FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
```
- user 삭제 시 해당 사용자의 신발도 모두 삭제 (데이터 무결성 보장)

**인덱스**:
```sql
CREATE INDEX idx_shoe_user_id ON shoe(user_id);
```

**예시 데이터**:
```
id  | user_id | brand | model             | size | cumulative_km
1   | 1       | Nike  | Air Zoom Pegasus  | 42   | 245.50
2   | 1       | Nike  | Vaporfly          | 42   | 120.75
3   | 2       | Adidas| Ultraboost        | 38   | 85.20
```

**동작 로직**:
- RunningRecord 추가 시: `cumulative_km += distance_km`
- RunningRecord 삭제 시: `cumulative_km -= distance_km`
- RunningRecord 수정 시: 거리 변경량만큼 조정

---

### **3. RunningRecord 테이블 (러닝 기록)**

**목적**: 각 러닝 활동의 상세 정보 저장 (거리, 시간, 신발, 기상 조건)

| 필드명 | 데이터타입 | 제약조건 | 설명 |
|--------|-----------|---------|------|
| `id` | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 기록 고유 ID |
| `user_id` | INTEGER | NOT NULL, FK → user(id) | 기록한 사용자 |
| `shoe_id` | INTEGER | NULL, FK → shoe(id) | 사용한 신발 (선택사항) |
| `distance_km` | DECIMAL(6,2) | NOT NULL | 러닝 거리 (km) |
| `duration_minutes` | INTEGER | NOT NULL | 러닝 시간 (분) |
| `date` | DATE | NOT NULL | 러닝 날짜 |
| `temperature` | DECIMAL(4,1) | NULL | 기온 (섭씨) |
| `humidity` | INTEGER | NULL | 습도 (%) - 0~100 |
| `wind_speed` | DECIMAL(4,1) | NULL | 풍속 (m/s) |
| `feeling` | VARCHAR(50) | NULL | 주관적 체감 (great, good, normal, tired, bad) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 기록 생성 시간 |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 마지막 수정 시간 |

**제약조건**:
```sql
FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
FOREIGN KEY (shoe_id) REFERENCES shoe(id) ON DELETE SET NULL
```
- user 삭제 시: 해당 사용자의 모든 기록 삭제
- shoe 삭제 시: 기록은 유지하되 shoe_id만 NULL로 설정

**인덱스**:
```sql
CREATE INDEX idx_running_record_user_id ON running_record(user_id);
CREATE INDEX idx_running_record_date ON running_record(date);
CREATE INDEX idx_running_record_shoe_id ON running_record(shoe_id);
```

**계산 필드** (데이터베이스에 저장하지 않음, 필요 시 계산):
- `pace` = duration_minutes / distance_km (분/km)
- `speed` = distance_km / (duration_minutes / 60) (km/h)

**예시 데이터**:
```
id  | user_id | shoe_id | distance_km | duration_minutes | date       | temperature | feeling
1   | 1       | 1       | 5.20        | 32               | 2026-09-15 | 22.5        | great
2   | 1       | 2       | 10.00       | 65               | 2026-09-16 | 18.0        | good
3   | 1       | 1       | 3.50        | 20               | 2026-09-17 | 25.0        | tired
4   | 2       | 3       | 5.00        | 30               | 2026-09-15 | 21.0        | good
```

---

## 🔗 1:N 관계도 상세 설명

### **관계 1: User (1) ─── (N) Shoe**

**의미**: "한 명의 사용자는 여러 신발을 소유할 수 있다"

```
User ID=1 (김철수)
│
├─ Shoe ID=1: Nike Air Zoom Pegasus 42 (훈련화)
│  └─ cumulative_km = 245.50
│
├─ Shoe ID=2: Nike Vaporfly 42 (경기화)
│  └─ cumulative_km = 120.75
│
└─ Shoe ID=3: Adidas Ultraboost 42 (일상화)
   └─ cumulative_km = 0

User ID=2 (이영미)
│
└─ Shoe ID=4: Adidas Ultraboost 38 (훈련화)
   └─ cumulative_km = 85.20
```

**데이터 관계**:
```
user 테이블의 id=1 (김철수)
  ↓
shoe 테이블에서 user_id=1인 모든 행 검색 (3개)
  → 김철수의 신발 3개 반환
```

**SQL 쿼리 예시**:
```sql
-- 사용자 1의 모든 신발 조회
SELECT * FROM shoe WHERE user_id = 1;

-- 사용자 1의 신발 개수
SELECT COUNT(*) FROM shoe WHERE user_id = 1;
```

---

### **관계 2: User (1) ─── (N) RunningRecord**

**의미**: "한 명의 사용자는 여러 러닝 기록을 남길 수 있다"

```
User ID=1 (김철수)
│
├─ RunningRecord ID=1: 2026-09-15, 5.20km, 32분
├─ RunningRecord ID=2: 2026-09-16, 10.00km, 65분
├─ RunningRecord ID=3: 2026-09-17, 3.50km, 20분
└─ ... (수백 개 가능)

User ID=2 (이영미)
│
├─ RunningRecord ID=4: 2026-09-15, 5.00km, 30분
└─ ... (많은 기록)
```

**통계 계산**:
```
User ID=1의 누적 거리: 5.20 + 10.00 + 3.50 + ... = 총 X km
User ID=1의 총 시간: 32 + 65 + 20 + ... = 총 Y분
User ID=1의 평균 페이스: 총 Y분 / 총 X km
```

**SQL 쿼리 예시**:
```sql
-- 사용자 1의 모든 러닝 기록 조회
SELECT * FROM running_record WHERE user_id = 1 ORDER BY date DESC;

-- 사용자 1의 누적 거리
SELECT SUM(distance_km) FROM running_record WHERE user_id = 1;

-- 사용자 1의 월간 통계 (2026년 9월)
SELECT 
  DATE_TRUNC('week', date) AS week,
  SUM(distance_km) AS weekly_distance,
  AVG(duration_minutes / distance_km) AS avg_pace
FROM running_record
WHERE user_id = 1 AND date BETWEEN '2026-09-01' AND '2026-09-30'
GROUP BY week;
```

---

### **관계 3: Shoe (1) ─── (N) RunningRecord**

**의미**: "한 신발로 여러 번의 러닝을 할 수 있다" → 누적 km이 증가

```
Shoe ID=1 (Nike Air Zoom Pegasus)
│
├─ RunningRecord ID=1: 2026-09-15, 5.20km ─┐
├─ RunningRecord ID=2: 2026-09-16, 0.00km  ├─ cumulative_km = 245.50
├─ RunningRecord ID=3: 2026-09-17, 3.50km  │ (누적된 거리)
└─ ... (50번 사용)                         ┘

Shoe ID=2 (Nike Vaporfly)
│
├─ RunningRecord ID=2: 2026-09-16, 10.00km ┐
├─ RunningRecord ID=... : ...              ├─ cumulative_km = 120.75
└─ ... (20번 사용)                         ┘
```

**신발 교체 판정**:
```
일반적인 러닝화 수명: 800-1000 km
→ cumulative_km > 1000 시 교체 권장 알림

SQL: SELECT * FROM shoe WHERE cumulative_km > 1000;
```

**SQL 쿼리 예시**:
```sql
-- 신발 1을 사용한 모든 러닝 기록
SELECT * FROM running_record WHERE shoe_id = 1 ORDER BY date;

-- 신발 1의 총 사용 횟수
SELECT COUNT(*) FROM running_record WHERE shoe_id = 1;

-- 신발 1의 누적 km (직접 계산 예시)
SELECT SUM(distance_km) FROM running_record WHERE shoe_id = 1;
```

**자동 업데이트 로직** (Backend에서 구현):
```
RunningRecord 추가 시:
  1. INSERT INTO running_record (...)
  2. UPDATE shoe SET cumulative_km = cumulative_km + new_distance
     WHERE id = shoe_id

RunningRecord 삭제 시:
  1. DELETE FROM running_record WHERE id = ...
  2. UPDATE shoe SET cumulative_km = cumulative_km - deleted_distance
     WHERE id = shoe_id

RunningRecord 수정 시:
  1. old_distance = 조회
  2. distance_diff = new_distance - old_distance
  3. UPDATE running_record SET distance_km = new_distance
  4. UPDATE shoe SET cumulative_km = cumulative_km + distance_diff
```

---

## 🔐 데이터 무결성 및 제약조건

### **Foreign Key (외래키) 설정**

| 자식 테이블 | 외래키 | 부모 테이블 | 액션 |
|-----------|-------|----------|------|
| shoe | user_id | user(id) | ON DELETE CASCADE |
| running_record | user_id | user(id) | ON DELETE CASCADE |
| running_record | shoe_id | shoe(id) | ON DELETE SET NULL |

**의미**:
- User 삭제 → 해당 user의 모든 Shoe/RunningRecord 자동 삭제
- Shoe 삭제 → RunningRecord의 shoe_id는 NULL로 설정 (기록은 유지)

### **제약조건 (Constraints)**

```sql
-- User
ALTER TABLE "user" ADD CONSTRAINT email_format 
CHECK (email LIKE '%@%.%');

-- Shoe
ALTER TABLE shoe ADD CONSTRAINT positive_km
CHECK (cumulative_km >= 0);

-- RunningRecord
ALTER TABLE running_record ADD CONSTRAINT positive_distance
CHECK (distance_km > 0);

ALTER TABLE running_record ADD CONSTRAINT positive_duration
CHECK (duration_minutes > 0);

ALTER TABLE running_record ADD CONSTRAINT valid_humidity
CHECK (humidity >= 0 AND humidity <= 100);

ALTER TABLE running_record ADD CONSTRAINT valid_feeling
CHECK (feeling IN ('great', 'good', 'normal', 'tired', 'bad', NULL));
```

---

## 📈 인덱스 전략

| 테이블 | 인덱스명 | 필드 | 용도 |
|--------|---------|------|------|
| user | idx_user_email | email | 로그인 시 사용자 빠른 검색 |
| shoe | idx_shoe_user_id | user_id | 사용자별 신발 조회 |
| running_record | idx_running_record_user_id | user_id | 사용자별 기록 조회 (통계 계산) |
| running_record | idx_running_record_date | date | 날짜별 기록 조회/필터링 |
| running_record | idx_running_record_shoe_id | shoe_id | 신발별 기록 조회 (누적 km 계산) |

**복합 인덱스** (성능 최적화):
```sql
-- 사용자의 특정 날짜 범위 기록 조회 최적화
CREATE INDEX idx_user_date ON running_record(user_id, date);

-- 신발 교체 시점 판단 (누적 km > 1000인 신발 조회)
CREATE INDEX idx_shoe_cumulative ON shoe(cumulative_km);
```

---

## 🔄 마이그레이션 계획

### **생성 순서** (외래키 관계 때문에 순서 중요)
1. `user` 테이블 생성
2. `shoe` 테이블 생성 (user_id FK)
3. `running_record` 테이블 생성 (user_id, shoe_id FK)

### **Alembic 마이그레이션**
```bash
# 스키마 변경 시마다 실행
alembic revision --autogenerate -m "Add shoe cumulative_km update trigger"
alembic upgrade head
```

---

## 📝 SQL DDL (Data Definition Language)

```sql
-- 1. User 테이블 생성
CREATE TABLE "user" (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    weight_kg DECIMAL(5, 2) NOT NULL,
    height_cm INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON "user"(email);

-- 2. Shoe 테이블 생성
CREATE TABLE shoe (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    size VARCHAR(10) NOT NULL,
    color VARCHAR(50),
    purchase_date DATE NOT NULL,
    cumulative_km DECIMAL(8, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shoe_user_id ON shoe(user_id);
CREATE INDEX idx_shoe_cumulative ON shoe(cumulative_km);

-- 3. RunningRecord 테이블 생성
CREATE TABLE running_record (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    shoe_id INTEGER REFERENCES shoe(id) ON DELETE SET NULL,
    distance_km DECIMAL(6, 2) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    date DATE NOT NULL,
    temperature DECIMAL(4, 1),
    humidity INTEGER,
    wind_speed DECIMAL(4, 1),
    feeling VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_running_record_user_id ON running_record(user_id);
CREATE INDEX idx_running_record_date ON running_record(date);
CREATE INDEX idx_running_record_shoe_id ON running_record(shoe_id);
CREATE INDEX idx_user_date ON running_record(user_id, date);

-- 4. 데이터 무결성 제약조건
ALTER TABLE "user" 
ADD CONSTRAINT email_format CHECK (email LIKE '%@%.%');

ALTER TABLE shoe 
ADD CONSTRAINT positive_km CHECK (cumulative_km >= 0);

ALTER TABLE running_record 
ADD CONSTRAINT positive_distance CHECK (distance_km > 0),
ADD CONSTRAINT positive_duration CHECK (duration_minutes > 0),
ADD CONSTRAINT valid_humidity CHECK (humidity IS NULL OR (humidity >= 0 AND humidity <= 100));
```

---

**완성일**: 2026년 9월 17일  
**다음 단계**: Backend 1, 2팀이 이 스키마를 기반으로 API 설계 진행
