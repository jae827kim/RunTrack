# 🐳 Docker 설정 가이드

**DevOps & QA 팀을 위한 RunTrack 백엔드 개발 환경 안내 (Docker Desktop, VS Code 사용 예시)**

이 문서는 실제 `backend/Dockerfile`, 루트 `docker-compose.yml`, 백엔드 코드를 기준으로 작성했다. 팀원이 GitHub에서 내려받아 환경변수를 설정한 뒤 `docker compose up -d --build`로 FastAPI·PostgreSQL·Redis를 실행하는 것이 목적이다. 운영 서버 배포는 [DEVOPS_CI_CD.md](DEVOPS_CI_CD.md)의 후속 단계다.

---

## 📋 목차

1. [개요](#개요)
2. [Dockerfile 작성](#dockerfile-작성)
3. [docker-compose.yml 작성](#docker-composeyml-작성)
4. [실행 방법](#실행-방법)
5. [명령어 레퍼런스](#명령어-레퍼런스)
6. [주간 체크리스트](#주간-체크리스트)
7. [트러블슈팅](#트러블슈팅)
8. [CI/CD 연계](#cicd-연계)
9. [참고 자료](#참고-자료)

---

## 개요

Dockerfile은 Backend 이미지를 만드는 조리법이고, 이미지는 Python과 애플리케이션을 묶은 실행 단위다. Compose는 Backend·PostgreSQL·Redis 컨테이너를 한 번에 만들고 연결한다. 이름 붙은 Docker 볼륨은 컨테이너와 별도로 DB·Redis 데이터를 보관한다. 이 문서의 Compose 구성은 팀원 PC에서 개발·검증할 때 사용한다.

### Docker란?

```text
개발자 PC (Windows; VS Code는 선택)
  └─ Docker Desktop (Linux containers / WSL 2)
       ├─ backend : Python 3.11 + FastAPI, 호스트 포트 8000
       ├─ db      : PostgreSQL 15, 호스트 127.0.0.1:5432
       └─ redis   : Redis 7, 호스트 127.0.0.1:6379
```

PostgreSQL과 Redis를 Windows에 따로 설치하지 않는다. 서비스는 Compose 내부 네트워크에서 `db`, `redis`라는 이름으로 서로 찾는다. API는 `http://localhost:8000`에서 접근한다.

개발 환경을 먼저 실행·검증하고, 확인된 이미지를 운영 환경에 적용하는 것이 향후 목표다. 현재 Compose 설정은 로컬 개발용이다.

### 왜 필요한가?

| 기존 개발상의 어려움 | 현재 구성 |
| --- | --- |
| 팀원마다 Python·DB 버전과 실행 명령이 다름 | Python 3.11 기반 Backend 이미지와 Compose 서비스 사용 |
| DB·Redis 시작 전에 Backend가 실행됨 | 두 서비스의 인증 헬스체크가 통과한 뒤 Backend 시작 |
| Redis 암호와 접속 URL이 다름 | 루트 `.env`의 같은 비밀번호를 Redis와 `REDIS_URL`에 사용 |
| 새 DB에 테이블이 없음 | 완전히 빈 개발 DB에만 모델 테이블 생성 |
| 컨테이너 재시작 후 데이터 손실 우려 | PostgreSQL·Redis 각각 이름 붙은 볼륨 사용 |

### 준비할 것

1. [Docker Desktop Windows 설치 안내](https://docs.docker.com/desktop/setup/install/windows-install/)에 따라 설치하고 **Linux containers** 엔진을 시작한다. Windows 10에서는 WSL 2 백엔드를 사용한다.
2. Git을 준비한다. VS Code를 사용한다면 **Terminal → New Terminal**에서 아래 명령을 실행한다. 일반 PowerShell 터미널에서도 같은 명령을 사용할 수 있다.
3. Docker Desktop에서 엔진이 실행 중인 것을 확인한다.

```powershell
docker --version
docker compose version
docker info
git --version
```

`docker info`가 Linux 엔진 연결 오류를 내면 이미지 빌드도 실행되지 않는다. Docker Desktop 상태부터 해결한다. **Docker로 개발할 때 호스트 Python 가상환경이나 별도 PostgreSQL·Redis 설치는 필요 없다.**

---

## Dockerfile 작성

### 위치

파일 위치는 `backend/Dockerfile`이다.

### 전체 코드

아래 코드는 현재 파일과 동일하다.

```dockerfile
# Multi-stage build for optimized image size
# Stage 1: Build stage
FROM python:3.11-slim AS builder

WORKDIR /app

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --user --no-cache-dir -r requirements.txt

# Stage 2: Runtime stage
FROM python:3.11-slim

WORKDIR /app

# Set environment variables
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH=/root/.local/bin:$PATH

# Copy only necessary files from builder
COPY --from=builder /root/.local /root/.local

# Copy application code
COPY . .

# Create non-root user for security (optional)
# RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
# USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "from urllib.request import urlopen; urlopen('http://localhost:8000/health', timeout=2)"

# Expose port
EXPOSE 8000

# Run the application
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 상세 설명

| 단계 | 실제 동작 | 이유 |
| --- | --- | --- |
| `python:3.11-slim` | Builder와 Runtime 모두 Python 3.11 사용 | 팀의 권장 Python 버전과 일치 |
| Builder의 `gcc` | 빌드 단계에서만 컴파일 도구 설치 | Runtime 이미지에 컴파일 도구를 넣지 않음 |
| `COPY requirements.txt` → `pip install` | 의존성을 소스보다 먼저 설치 | 소스만 바뀔 때 패키지 설치 레이어 재사용 |
| `pip install --user --no-cache-dir` | 패키지를 Builder의 `/root/.local`에 설치 | pip 다운로드 캐시를 이미지에 남기지 않음 |
| `COPY --from=builder` | 설치된 패키지만 Runtime에 복사 | Runtime 크기 절감 |
| `COPY . .` | Backend 소스 복사 | `backend/.dockerignore`가 비밀 파일·가상환경·캐시를 제외 |
| `PYTHONUNBUFFERED` | 로그를 즉시 출력 | `docker compose logs`에서 확인하기 쉬움 |
| `HEALTHCHECK` | 컨테이너 내부에서 `/health` 요청 | FastAPI 프로세스 응답 확인 |
| `CMD` | Uvicorn으로 FastAPI 실행 | 이미지 기본 실행은 개발 DB를 자동 초기화하지 않음 |

`/health`는 요청마다 DB와 Redis를 검사하는 엔드포인트가 아니다. **개발용 Compose의 시작 전 연결 검사**는 `backend/app/docker_start.py`가 PostgreSQL `SELECT 1`과 인증된 Redis `PING`으로 수행한다. 빈 개발 DB에서만 테이블을 만들고, 기존 DB에서 일부 테이블이 없으면 자동 변경 대신 오류를 낸다. Dockerfile의 기본 실행 명령은 이 개발용 초기화를 호출하지 않는다.

### 빌드 방법

`backend/.dockerignore`는 `.env`, `.venv`, `venv`, Python 캐시, 로그, 생성된 문서 등을 빌드 컨텍스트에서 제외한다. 기존 테스트를 이미지 안에서 실행할 수 있도록 `backend/tests`는 포함한다. Compose는 기존 개발 방식대로 `./backend:/app`을 마운트하고 Uvicorn `--reload`를 사용한다. 호스트에서 `backend/app` 소스를 수정하면 서버가 다시 시작된다. `requirements.txt`나 Dockerfile을 바꾸면 `docker compose up -d --build`로 이미지를 다시 빌드한다. 이 소스 마운트는 개발용이며 CI의 Docker 이미지 빌드에는 적용되지 않는다. `--reload`는 DB 스키마를 변경하지 않으므로 모델이 바뀌었다면 마이그레이션을 별도로 검토한다.

```powershell
# 저장소 루트에서 Backend 이미지만 빌드
docker compose build backend
```

### 실행 방법 (단독)

단독 `docker build`도 가능하지만, **단독 `docker run`은 DB·Redis 주소와 비밀값, 네트워크를 함께 설정해야 한다.** 팀원 개발 환경에서는 세 서비스를 함께 시작하는 Compose를 사용한다.

> 재현성 범위: 직접 지정한 주요 Python 패키지 버전은 `requirements.txt`에 고정돼 있다. 다만 `python:3.11-slim`·`postgres:15-alpine`·`redis:7-alpine` 태그와 간접 의존성은 미래의 재빌드에서 바뀔 수 있다. 완전히 동일한 이미지가 필요한 경우 별도 작업으로 이미지 digest와 전체 의존성 잠금 파일을 관리해야 한다.

---

## docker-compose.yml 작성

### 위치

파일은 저장소 루트의 `docker-compose.yml`이다.

### 전체 코드

아래는 현재 적용된 전체 설정이다.

```yaml
services:
  # Backend FastAPI 서버 (VS Code 개발용 소스 마운트)
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8000:8000"
    environment:
      DATABASE_URL: postgresql://runtrack_user:${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD in .env}@db:5432/runtrack
      REDIS_URL: redis://:${REDIS_PASSWORD:?Set REDIS_PASSWORD in .env}@redis:6379/0
      SECRET_KEY: ${SECRET_KEY:?Set SECRET_KEY in .env}
      DEBUG: "False"
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - ./backend:/app
    command: ["sh", "-c", "python -m app.docker_start && exec uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload --reload-dir /app/app"]
    restart: unless-stopped

  # PostgreSQL 데이터베이스
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: runtrack
      POSTGRES_USER: runtrack_user
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD in .env}
    ports:
      - "127.0.0.1:5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "PGPASSWORD=\"$$POSTGRES_PASSWORD\" psql -h 127.0.0.1 -U \"$$POSTGRES_USER\" -d \"$$POSTGRES_DB\" -tAc 'SELECT 1' | grep -q 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    restart: unless-stopped

  # Redis 캐시 서버
  redis:
    image: redis:7-alpine
    environment:
      REDIS_PASSWORD: ${REDIS_PASSWORD:?Set REDIS_PASSWORD in .env}
    command: ["sh", "-c", "exec redis-server --appendonly yes --requirepass \"$$REDIS_PASSWORD\""]
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD-SHELL", "REDISCLI_AUTH=\"$$REDIS_PASSWORD\" redis-cli ping | grep -q PONG"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    restart: unless-stopped

# Named volumes for data persistence
volumes:
  postgres_data:
  redis_data:
```

### 상세 설명

#### 📦 `backend`: FastAPI

- `build.context: ./backend`가 위 Dockerfile과 `backend/.dockerignore`를 사용한다.
- `DATABASE_URL`과 `REDIS_URL`은 컨테이너 내부 호스트명 `db`·`redis`를 사용한다. Windows에서 쓰는 `localhost`를 컨테이너 URL에 넣으면 연결되지 않는다.
- `depends_on: condition: service_healthy`는 PostgreSQL과 Redis가 인증 헬스체크를 통과할 때까지 Backend 시작을 기다린다.
- Compose의 개발용 `command`는 Dockerfile의 기본 Uvicorn 명령을 덮어쓰고 연결 검사와 `--reload`를 실행한다. `app.docker_start`가 두 서비스에 직접 연결하고, 빈 개발 DB의 `users`·`shoes`·`running_records`·`running_sessions` 테이블을 생성한다.
- `./backend:/app`은 호스트에서 편집한 소스를 컨테이너에 반영한다. `--reload-dir /app/app`은 앱 코드만 감시한다. 호스트의 `backend/.env`가 있다면 개발 컨테이너에도 보이므로 Git에 올리거나 다른 사람과 공유하지 않는다.
- `8000:8000`은 API를 호스트에 공개한다. 휴대전화 등 다른 기기의 접속이 필요하면 Windows 방화벽과 네트워크 정책을 별도로 확인한다.

#### 🗄️ `db`: PostgreSQL

- `postgres:15-alpine` 이미지를 사용하며 DB 이름은 `runtrack`, 계정은 `runtrack_user`다.
- 비밀번호는 Git에 넣지 않는 루트 `.env`의 `POSTGRES_PASSWORD`에서 읽는다.
- 헬스체크는 비밀번호로 접속해 `SELECT 1`을 수행한다. `pg_isready`만 실행하는 것보다 인증·DB 선택까지 확인한다.
- `postgres_data` 볼륨을 `/var/lib/postgresql/data`에 마운트한다. 호스트 포트 `5432`는 `127.0.0.1`에만 바인딩한다.

#### 🔴 `redis`: Redis

- `redis:7-alpine` 이미지를 사용하고 `REDIS_PASSWORD`로 인증한다. Backend의 `REDIS_URL`에도 같은 값이 들어간다.
- 헬스체크는 인증된 `redis-cli ping`의 `PONG`을 확인한다. 암호 없이 `redis-cli ping`하면 `NOAUTH`가 정상이다.
- `--appendonly yes`와 `redis_data` 볼륨으로 데이터를 보관한다. 호스트 포트 `6379`는 `127.0.0.1`에만 바인딩한다.
- 현재 Backend는 **Redis 연결을 검사**한다. 별도의 캐시 비즈니스 기능을 새로 구현한 것은 아니다.

#### 💾 이름 붙은 볼륨

`postgres_data`와 `redis_data`는 각 서비스의 데이터 저장소다. `docker compose stop`, `restart`, 일반 `down`으로는 삭제되지 않는다. 프로젝트 이름을 바꾸면 Compose 볼륨 이름도 달라질 수 있으므로 기존 볼륨을 사용 중이라면 프로젝트 이름을 임의로 바꾸지 않는다.

### 환경변수 준비

VS Code를 사용한다면 **View → Command Palette → Git: Clone**을 선택해 `https://github.com/jae827kim/RunTrack.git`을 `C:\Github` 아래에 클론한다. 다른 Git 도구로 클론해도 된다. 완료 후 `C:\Github\RunTrack` 폴더를 연다. 탐색기에서 루트 `.env.example`을 복사해 같은 위치에 `.env`로 붙여넣는다.

루트 `.env.example`의 실제 항목은 다음과 같다.

```dotenv
# Copy to .env in the repository root. Use newly generated hex values, never these examples.
# Generate three separate hex values; see backend/docs/DOCKER_SETUP.md for VS Code terminal commands.
# Hex avoids URL-encoding issues in DATABASE_URL and REDIS_URL.
POSTGRES_PASSWORD=replace_with_random_hex
REDIS_PASSWORD=replace_with_random_hex
SECRET_KEY=replace_with_random_hex
```

루트 `.env`의 `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `SECRET_KEY`를 **서로 다른 새 값**으로 교체한다. 예시 문자열은 사용하지 않는다. VS Code 통합 터미널에서 다음 명령을 **세 번** 실행하면 URL 인코딩이 필요 없는 64자리 16진수 값을 만들 수 있다.

```powershell
py -3.11 -c "import secrets; print(secrets.token_hex(32))"
```

호스트에 Python 3.11이 없거나 `py` 명령을 사용할 수 없다면, 같은 VS Code 터미널의 Windows PowerShell에서 아래 명령을 세 번 실행한다. 이를 위해 Python을 따로 설치할 필요는 없다.

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
([BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
```

`.env`는 Git에서 무시되며 팀원 각자 로컬에서 만든다. `backend/.env.example`은 Docker 없이 Python을 호스트에서 직접 실행할 때 쓰는 별도 예시다. 현재 Google API 키는 Docker 시작에 필요하지 않다.

**기존 PostgreSQL 볼륨의 암호 주의:** 이미 초기화된 DB에서 루트 `.env`의 `POSTGRES_PASSWORD`만 바꾸면 DB 내부 계정 암호는 바뀌지 않는다. 기존 데이터를 지우지 말고 원래 암호를 사용하거나 DB 담당자와 계정 암호 변경 절차를 확인한다.

---

## 실행 방법

모든 명령은 **RunTrack 저장소 루트**에서 실행한다. 아래는 VS Code 통합 터미널을 예로 들었으며, 일반 PowerShell 터미널에서도 동일하게 실행할 수 있다. 실제 컨테이너는 Docker Desktop의 Linux 엔진에서 동작한다.

### 1️⃣ 첫 실행

```powershell
docker compose config --quiet
docker compose up -d --build
```

`config --quiet`은 환경변수와 YAML 설정을 검사하면서 비밀번호를 터미널에 출력하지 않는다. 첫 실행에는 Python·PostgreSQL·Redis 이미지를 다운로드하므로 시간이 걸린다. `-d`는 백그라운드 실행이며, `--build`는 Backend 이미지를 최신 소스로 빌드한다.

### 2️⃣ 컨테이너·API 상태 확인

먼저 아래 명령으로 세 서비스가 모두 `healthy`가 될 때까지 확인한다. 첫 시작이나 재시작 직후에는 `health: starting`이 표시될 수 있으므로 잠시 뒤 다시 실행한다.

```powershell
docker compose ps
```

세 서비스가 `healthy`가 되면 API와 로그를 확인한다.

```powershell
Invoke-RestMethod http://localhost:8000/health
Start-Process http://localhost:8000/docs
docker compose logs --tail=50 backend
```

`ps`에서 세 서비스가 `healthy`인지 확인한다. `/health`는 `{"status":"healthy"}`, `/docs`는 FastAPI API 문서를 보여야 한다. Backend 로그에 `PostgreSQL and Redis connections verified`가 나타나면 시작 시 두 연결 검사에 성공한 것이다.

### 3️⃣ DB 테이블과 Redis 인증 확인

```powershell
docker compose exec db psql -U runtrack_user -d runtrack -c '\dt'
docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli ping'
```

DB에는 `users`, `shoes`, `running_records`, `running_sessions`가 보여야 하고 Redis는 `PONG`을 반환해야 한다. `/health` 응답만으로 DB·Redis의 현재 연결 상태를 단정하지 않는다.

### 4️⃣ 기존 pytest 실행

```powershell
docker compose run --rm --no-deps backend pytest tests/ -q
```

테스트는 임시 SQLite DB를 사용하며 설정된 개발 PostgreSQL 연결을 막는다. `--rm`은 이 일회성 **테스트 컨테이너만** 실행 후 제거한다. DB·Redis 볼륨은 삭제하지 않는다.

### 5️⃣ 중지·재시작·데이터 유지 확인

작업을 잠시 멈췄다가 다시 시작할 때:

```powershell
docker compose stop
docker compose start
```

실행 중인 서비스 프로세스를 재시작할 때:

```powershell
docker compose restart
```

컨테이너·네트워크를 제거하고 나중에 다시 만들 때:

```powershell
docker compose down
docker compose up -d
```

- `stop`은 컨테이너를 중지하고, `start`는 다시 시작한다.
- `restart`는 서비스 프로세스를 재시작한다. 시작 로그와 `ps`가 다시 `healthy`인지 확인한다.
- `down`은 컨테이너와 네트워크를 제거하지만 이름 붙은 데이터 볼륨은 유지한다. 이후 `up -d`로 재시작한다.

아래 데이터 유지 검사는 **개발 환경에서만** 실행한다. 같은 VS Code 터미널에서 각 블록을 순서대로 실행하며, 조회 결과를 확인한 뒤 정리 블록을 실행한다. 중요한 데이터는 Docker 볼륨과 별도로 백업한다.

Redis에는 기존 키와 겹치지 않는 고유한 검사 키를 만든다.

```powershell
$probeKey = "runtrack:docker:probe:$([guid]::NewGuid().ToString('N'))"
docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli SET "$1" persisted' sh $probeKey
docker compose restart
docker compose ps
```

세 서비스가 다시 `healthy`가 된 뒤 값을 조회한다.

```powershell
docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli GET "$1"' sh $probeKey
```

`persisted`가 나오면 재시작 후에도 남은 것이다. 결과를 확인한 뒤 **방금 만든 검사 키만** 제거한다. 조회에 실패했다면 원인을 확인한 뒤 정리한다.

```powershell
docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli DEL "$1"' sh $probeKey
```

PostgreSQL도 **개발 DB에서만** 임의 이름의 검사 테이블을 만들어 같은 방법으로 확인할 수 있다. 아래 명령은 같은 VS Code 터미널에서 순서대로 실행한다. 재시작 후 `docker compose ps`에서 서비스가 다시 `healthy`인지 확인하고 조회한다.

```powershell
$probeTable = "runtrack_docker_probe_$([guid]::NewGuid().ToString('N'))"
docker compose exec db psql -v ON_ERROR_STOP=1 -U runtrack_user -d runtrack -c "CREATE TABLE $probeTable (marker text NOT NULL); INSERT INTO $probeTable (marker) VALUES ('persisted');"
docker compose restart
docker compose ps
```

세 서비스가 다시 `healthy`가 된 뒤 값을 조회한다.

```powershell
docker compose exec db psql -v ON_ERROR_STOP=1 -U runtrack_user -d runtrack -tAc "SELECT marker FROM $probeTable;"
```

`persisted`가 나오면 DB 데이터가 재시작 후에도 남은 것이다. 결과를 확인한 뒤 **이 블록에서 새로 만든 검사 테이블만** 제거한다. 조회에 실패했다면 원인을 확인한 뒤 정리한다.

```powershell
docker compose exec db psql -v ON_ERROR_STOP=1 -U runtrack_user -d runtrack -c "DROP TABLE $probeTable;"
```

### 6️⃣ 빈 DB 초기화와 기존 DB 마이그레이션

처음 만든 **완전히 빈 개발 DB**에서는 Backend가 모델의 네 테이블을 생성한다. 기존 DB에 테이블이 일부만 있으면 자동 생성·수정하지 않고 오류를 낸다. 현재 저장소에는 Alembic 패키지는 있지만 실행 환경과 버전별 마이그레이션 파일은 없다.

시작 검사는 테이블 이름의 존재 여부를 확인하며 기존 컬럼·제약조건까지 모델과 비교하지는 않는다. 모델 변경을 기존 DB에 반영하려면 별도의 마이그레이션이 필요하다.

기존 DB에 `users`, `shoes`, `running_records`가 있고 `running_sessions`만 없다면 **백업을 확인한 뒤** 기존 스크립트를 실행한다.

```powershell
docker compose run --rm backend python -m app.migrate_running_sessions
docker compose up -d backend
```

이 스크립트는 `running_sessions` 테이블만 `checkfirst=True`로 생성한다. 다른 테이블이 없거나 스키마가 모델과 다르면 DB 담당자와 변경 방법을 정한다. 기존 볼륨을 삭제해 문제를 숨기지 않는다.

### 로컬 검증 기록 — 2026-11-10

Windows PC의 Docker Desktop Linux 엔진(`29.8.1`), Compose `v5.5.1`, Backend Python `3.11.18`에서 다음을 확인했다. 팀원별 환경 확인은 아래 주간 체크리스트를 사용한다.

| 검증 | 결과 |
| --- | --- |
| `docker compose config --quiet` | 통과 |
| `docker compose up -d --build` | 빌드·실행 성공. 기존 빌드 캐시 사용 |
| 컨테이너·API | 세 서비스 `healthy`, `/health` 정상 응답, `/docs` HTTP 200 |
| PostgreSQL·Redis | 네 모델 테이블, 인증 `PONG`, Backend 시작 연결 검사 통과 |
| 컨테이너 내 pytest | 91개 통과, 의존성 사용 중단 예정 경고 2개 |
| 재시작 후 데이터 유지 | PostgreSQL·Redis의 고유한 검사 값 모두 유지. 검사 데이터만 정리 |
| 빈 개발 DB 초기화 | 별도 임시 DB에 네 테이블 생성 확인 후 임시 DB만 정리 |

이 기록은 로컬 Docker 검증 결과다. 캐시 없는 전체 재빌드, 기존 DB 마이그레이션 스크립트의 실제 적용, 이번 변경의 GitHub Actions 실행은 이 기록에 포함하지 않는다.

---

## 명령어 레퍼런스

| 목적 | 저장소 루트에서 실행할 명령 |
| --- | --- |
| Compose 문법과 필수 변수 검사 | `docker compose config --quiet` |
| Backend 이미지만 빌드 | `docker compose build backend` |
| 세 서비스 빌드·시작 | `docker compose up -d --build` |
| 상태 확인 | `docker compose ps` |
| 모든 로그 확인 | `docker compose logs --tail=100` |
| 특정 서비스 로그 | `docker compose logs --tail=100 backend` |
| 로그 계속 보기 | `docker compose logs -f backend` |
| 컨테이너 안에서 명령 실행 | `docker compose exec backend python --version` |
| 테스트 실행 | `docker compose run --rm --no-deps backend pytest tests/ -q` |
| 일시 중지 / 다시 시작 | `docker compose stop` / `docker compose start` |
| 세 서비스 재시작 | `docker compose restart` |
| 컨테이너만 제거하고 볼륨 유지 | `docker compose down` |
| Docker 저장공간 사용량 보기 | `docker system df` |

현재 Docker CLI에서는 `docker compose`(띄어쓰기)를 사용한다. 기존 문서의 `docker-compose` 예시와 `container_name` 고정 이름은 현재 설정에 맞지 않는다. 서비스 대상 명령에는 컨테이너 이름 대신 `backend`, `db`, `redis`를 쓴다.

**데이터 삭제 주의:** `docker compose down -v`는 RunTrack의 DB·Redis 볼륨을 삭제한다. `docker system prune -a --volumes`는 다른 프로젝트의 데이터까지 제거할 수 있다. 두 명령은 일반적인 종료·공간 정리에 사용하지 않는다.

---

<a id="주간-체크리스트"></a>

## 📝 주간 체크리스트

### Week 1 — 팀원 환경 준비

- [ ] RunTrack 클론을 열고 Docker Desktop의 Linux 엔진이 실행 중인지 확인
- [ ] 루트 `.env.example`을 `.env`로 복사하고 세 비밀값을 각자 새 값으로 설정
- [ ] `docker compose config --quiet` 검사
- [ ] `docker compose up -d --build` 실행
- [ ] `docker compose ps`에서 세 서비스 `healthy` 확인

### Week 2 — 기능·데이터 검증

- [ ] `/health`와 `/docs` 접속 확인
- [ ] PostgreSQL 네 테이블과 Redis 인증 `PING` 확인
- [ ] `docker compose run --rm --no-deps backend pytest tests/ -q` 실행
- [ ] 컨테이너 재시작 후 개발 DB와 Redis 데이터 유지 확인
- [ ] Backend 앱 코드를 바꿨다면 자동 재시작을 확인하고, 의존성을 바꿨다면 `up -d --build`로 새 이미지 반영

### Week 3+ — Production 준비

- [x] 멀티스테이지 빌드와 컨테이너 헬스체크 설정
- [ ] CI의 테스트·린트·이미지 게시 결과 확인
- [ ] 운영 서버 배포와 헬스체크 절차 구현·검증
- [ ] 운영 로그 수집·분석 및 성능 최적화

운영 배포는 [DEVOPS_CI_CD.md](DEVOPS_CI_CD.md)의 계획에 따라 후속 작업으로 진행한다. 이 개발용 Compose 검증이 운영 배포 완료를 뜻하지는 않는다.

---

<a id="트러블슈팅"></a>

## 🔧 트러블슈팅

### Q1. Docker Desktop 엔진에 연결되지 않음

`docker info`가 `dockerDesktopLinuxEngine` 파이프 오류를 내면 Docker Desktop을 열고 Linux containers 엔진이 실행 중인지 확인한다. WSL `HCS/ERROR_NO_SYSTEM_RESOURCES`가 나오면 Windows의 남은 메모리·가상 메모리·C: 공간을 확인하고 사용하지 않는 앱을 닫은 뒤 Windows를 재시작한다. **Docker 데이터 디스크나 볼륨을 삭제하지 않는다.**

### Q2. 포트 8000·5432·6379가 이미 사용 중

```powershell
Get-NetTCPConnection -LocalPort 8000,5432,6379 -ErrorAction SilentlyContinue
docker compose ps
```

기존 프로그램을 확인한 뒤 중지하거나 Compose의 **호스트 쪽** 포트만 변경한다. 예를 들어 `"8001:8000"`이면 API 접속 주소는 `http://localhost:8001`이 된다. 관련 없는 컨테이너를 강제 삭제하지 않는다.

### Q3. DB 연결 거부 또는 `db`가 `unhealthy`

```powershell
docker compose logs --tail=100 db
docker compose logs --tail=100 backend
```

루트 `.env`의 `POSTGRES_PASSWORD`와 기존 볼륨의 DB 계정 암호가 같은지 확인한다. Backend의 DB 호스트는 `localhost`가 아니라 Compose 서비스 이름 `db`다. `depends_on`은 정상 상태를 기다리지만 암호 오류나 잘못된 기존 스키마를 수정하지는 않는다.

### Q4. Redis `NOAUTH` 또는 Backend Redis 연결 실패

암호 없이 실행한 `redis-cli ping`의 `NOAUTH`는 인증이 작동한다는 뜻이다. 위의 `REDISCLI_AUTH` 명령으로 `PONG`을 확인한다. 루트 `.env`의 `REDIS_PASSWORD`가 Redis 실행 명령과 Backend `REDIS_URL`에 모두 들어가는지 `docker compose config --quiet`과 로그로 확인한다. `docker compose config`를 `--quiet` 없이 출력하면 비밀값이 화면에 나올 수 있으므로 공유하지 않는다.

### Q5. Backend 시작 시 기존 DB의 테이블 누락 오류

로그에 빠진 테이블 이름이 나온다. 완전히 빈 DB만 자동 초기화한다. 기존 DB에서 `running_sessions`만 없다면 위의 마이그레이션 절차를 사용하고, 다른 스키마 문제는 DB 담당자와 확인한다.

### Q6. 빌드가 느리거나 공간이 부족함

`docker system df`로 이미지·캐시·볼륨 사용량을 먼저 확인한다. 이미지를 다시 빌드하면 소스만 바뀐 경우 패키지 설치 레이어가 재사용된다. 태그가 새 digest를 가리키면 캐시가 새로 만들어질 수 있다. 저장소는 OneDrive보다 `C:\Github`처럼 동기화되지 않는 폴더에 두는 편이 안정적이다. 볼륨 삭제를 포함한 전체 정리 명령을 바로 실행하지 않는다.

---

## CI/CD 연계

[DEVOPS_CI_CD.md](DEVOPS_CI_CD.md)는 lint·test → Docker 이미지 빌드·게시 → 운영 배포의 계획을 설명한다. 현재 실행되는 워크플로와 차이가 있는 부분은 `.github/workflows/`의 YAML 파일을 기준으로 확인한다.

| 워크플로 | 현재 트리거와 실제 작업 |
| --- | --- |
| `test.yml` | `main` 대상 PR의 `backend/**` 변경 또는 `main` 푸시의 `backend/**`·`test.yml` 변경 시 실행. Python 3.11에서 pytest와 커버리지 보고서를 생성 |
| `lint.yml` | `main` 대상 PR의 `backend/**` 변경 또는 `main` 푸시의 `backend/**`·`lint.yml` 변경 시 실행. 치명적 Flake8 오류를 검사하고 나머지는 보고용으로 실행 |
| `deploy.yml` | `main` 푸시에서 `backend/**`·`docker-compose.yml`·`deploy.yml` 변경 시 시작. Docker Hub 자격 정보가 모두 있으면 Backend 이미지를 빌드·게시. 운영 서버 배포 단계는 예시 명령만 있음 |

현재 `deploy.yml`은 `test.yml`·`lint.yml`의 성공을 기다리지 않는다. 따라서 `main` 직접 푸시에서 테스트가 실패해도 자격 정보가 있다면 이미지 게시가 진행될 수 있다. 실행 순서 보장은 별도 CI 개선 항목이다. 루트 `.env`는 Git에 올라가지 않으며 CI의 이미지 빌드에는 필요 없다.

CI의 PostgreSQL·Redis 서비스는 로컬 Compose와 별개다. 현재 pytest는 임시 SQLite를 사용하고 구성된 개발 PostgreSQL 연결을 막으므로, CI 통과만으로 Docker Compose의 DB·Redis 연결과 데이터 지속성을 증명하지 않는다. 로컬 검증은 이 문서의 `실행 방법`을 따른다.

---

<a id="참고-자료"></a>

## 📞 참고 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 가이드](https://docs.docker.com/compose/)
- [Python 이미지 빌드 가이드](https://docs.docker.com/language/python/build-images/)
- [WSL 설정 가이드](https://learn.microsoft.com/windows/wsl/wsl-config)
- [RunTrack CI/CD 문서](DEVOPS_CI_CD.md)
