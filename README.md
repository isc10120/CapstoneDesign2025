# CapstoneDesign2025
임시 레포 제목입니다.

회의록 등을 올리는 드라이브:
https://drive.google.com/drive/u/0/folders/1QcczFAZHuL8FF6tPKrwO50WiHKIlXnuK   
   
팀원 개인 개발 블로그
- 전희원: https://won-ki.tistory.com/
- 이서연: https://velog.io/@gamuza/posts
- 공세영: https://techno-0302.tistory.com/

---

## 권한

앱 실행 시 아래 권한이 필요합니다:

| 권한 | 용도 |
|------|------|
| SYSTEM_ALERT_WINDOW | 다른 앱 위에 오버레이 표시 |
| FOREGROUND_SERVICE | 백그라운드 오버레이 서비스 유지 |
| SCHEDULE_EXACT_ALARM | 넛지 알람 정확한 시간 예약 |
| PACKAGE_USAGE_STATS | DND 앱 사용 감지 |
| POST_NOTIFICATIONS | 알림 표시 |

## How to Build

**사전 요구사항**
- Android Studio Hedgehog 이상
- JDK 11 이상

**빌드 방법**

  ### 레포 클론
  git clone https://github.com/isc10120/capstonedesign2025.git
  cd capstonedesign2025/Frontend

  ### 디버그 APK 빌드
  ./gradlew assembleDebug

  ### 빌드 결과물 위치: app/build/outputs/apk/debug/app-debug.apk

**Android Studio에서 실행**
1. Frontend/ 폴더를 Android Studio로 열기
2. API 26 이상의 에뮬레이터 또는 실제 기기 연결
3. Run > Run 'app' 실행

## How to Install

1. 빌드된 app-debug.apk를 기기에 전송
2. 기기 설정 > 보안 > 출처를 알 수 없는 앱 허용
3. APK 설치 후 실행
4. 앱 최초 실행 시 요청하는 권한 모두 허용:
   - 다른 앱 위에 표시 권한 (SYSTEM_ALERT_WINDOW)
   - 사용 정보 접근 권한 (PACKAGE_USAGE_STATS)
   - 정확한 알람 권한 (SCHEDULE_EXACT_ALARM)

---

# Frontend — Android 앱

## 기술 스택

- **언어**: Kotlin
- **최소 SDK**: API 26 (Android 8.0)
- **타겟 SDK**: API 36
- **주요 라이브러리**:
  - Retrofit2 + Gson (REST API 통신)
  - OkHttp STOMP (WebSocket PVP 실시간 통신)
  - Coil (이미지 로딩)
  - AndroidX ViewModel / LiveData / Coroutines

## 주요 기능

### 1. 오버레이 위젯 시스템
다른 앱 위에 단어 카드를 띄워 **스마트폰 사용 중 자연스럽게 단어를 노출**합니다.
`OverlayService` (Foreground Service)가 상시 실행되며 알람 스케줄러에 따라 넛지를 트리거합니다.

| 위젯 종류 | 동작 방식 | 종료 조건 |
|-----------|-----------|-----------|
| **Morning Overlay** | 앱 실행 시 전체화면 오버레이로 오늘의 학습 난이도 선택 | 난이도 선택 |
| **Word List Overlay** | 오늘의 단어 목록을 슬라이드 드로어 형태로 표시 | 닫기 버튼 |
| **Nudge Drag** | 단어 카드를 일정 시간(1.5초) 드래그해야 닫힘 | 드래그 유지 |
| **Nudge Tap** | 단어 카드를 6회 연속 탭해야 닫힘 | 6회 탭 |
| **Nudge Bounce** | 단어 카드가 화면을 튕기며 이동, 4회 튕긴 후 플리킹으로 닫힘 | 빠른 플릭 |

넛지 타입은 랜덤(ACTION_SHOW_NUDGE_RANDOM)으로 선택되며,
사용자가 설정한 **방해 금지 앱 목록**과 **넛지 간격**에 따라 자동 스케줄링됩니다.

### 2. 오늘의 단어 (Today's Word)
- 서버에서 설정한 난이도에 맞는 단어 목록 수신
- 단어별 학습 진행도 로컬 저장 (WordProgressManager)

### 3. PVE 카드 배틀
- 덱 선택 → 던전 스테이지 → 단어 문제 풀이 → 몬스터와 배틀
- 스킬 카드 시스템 (방어, 독, 마비, 강화 등 상태이상 포함)
- PveBattleEngine에서 로컬 배틀 로직 처리

### 4. PVP 실시간 배틀
- WebSocket(STOMP) 기반 실시간 1:1 대전
- 단어 문제 정답 시 스킬 카드 획득 및 사용
- 수집한 카드 관리 (CollectedCardsActivity)

### 5. 기타
- 회원가입 / 로그인 (JWT 인증)
- 랭킹 리더보드
- 컬렉션 (획득 카드 목록)
- 설정: 모닝 오버레이 시간, 넛지 간격, DND 앱 목록

## How to Test

**단위 테스트 실행**
  cd Frontend
  ./gradlew test

**오버레이 위젯 수동 테스트**
- 앱 실행 후 홈 화면으로 나가기
- 다른 앱(예: YouTube) 실행
- 설정된 넛지 간격 후 오버레이 위젯 자동 표시 확인

**개발 중 빠른 테스트 (OverlayService 직접 트리거)**
  adb shell am startservice \
    -n com.example.zamgavocafront/.service.OverlayService \
    -a ACTION_SHOW_NUDGE_RANDOM

---

# Backend — Spring Boot 서버

## 기술 스택

- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.4.3
- **JDK**: 17
- **DB**: PostgreSQL (로컬) / AWS RDS PostgreSQL (운영)
- **ORM**: Spring Data JPA (Hibernate)
- **인증**: JWT (jjwt 0.12.6) + Spring Security
- **실시간 통신**: WebSocket (STOMP)
- **AI**: Spring AI + OpenAI GPT-4o (단어/스킬 생성), gpt-image-1 (스킬 이미지 생성)
- **Storage**: AWS S3 (스킬 이미지)
- **빌드 도구**: Gradle (Kotlin DSL)

## 소스 코드 구조

```
demo/src/main/kotlin/jamgaVOCA/demo/
├── api/                  # REST 컨트롤러, DTO
│   ├── dto/              # 요청/응답 DTO
│   └── exception/        # 전역 예외 처리
├── config/               # Security, JWT, WebSocket 설정
├── domain/               # JPA 엔티티 및 Repository
│   ├── user/             # 유저, 레벨 유틸
│   ├── battle/           # PVP 배틀, 효과
│   ├── deck/             # 덱, 덱 스킬
│   ├── skill/            # 스킬
│   ├── word/             # 단어
│   └── userwordskill/    # 유저 수집 스킬
├── service/              # 비즈니스 로직
│   └── generateSkill/    # AI 스킬/이미지 생성
└── infra/                # 외부 연동 (S3, 이미지 색상 추출)
```

## 필요한 환경 변수

| 변수명 | 설명 |
|--------|------|
| `OPENAI_API_KEY` | OpenAI API 키 |
| `JWT_SECRET` | JWT 서명 시크릿 |
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 |
| `AWS_SECRET_ACCESS_KEY` | AWS 시크릿 키 |
| `DB_URL` | DB 접속 URL (로컬 기본값: `jdbc:postgresql://localhost:5432/app`) |
| `DB_USER` | DB 유저 (로컬 기본값: `app`) |
| `DB_PASSWORD` | DB 비밀번호 (로컬 기본값: `app`) |
| `ADMIN_SECRET` | 관리자 API 시크릿 (로컬 기본값: `local-admin-secret`) |

## How to Build

**사전 요구사항**
- JDK 17 이상
- PostgreSQL 실행 중 (로컬 빌드 시)
- Gradle (Wrapper 포함되어 있어 별도 설치 불필요)

```bash
# 레포 클론
git clone https://github.com/isc10120/capstonedesign2025.git
cd capstonedesign2025/demo

# 빌드 (테스트 제외)
./gradlew build -x test

# 빌드 결과물 위치: demo/build/libs/demo-0.0.1-SNAPSHOT.jar
```

## How to Install

### Docker Compose로 실행 (권장)

**사전 요구사항**
- Docker Desktop 설치

**1. 환경 변수 파일 생성**

`demo/` 디렉토리에 `.env` 파일을 생성합니다:
```
OPENAI_API_KEY=your_key
JWT_SECRET=your_secret
AWS_ACCESS_KEY_ID=your_key
AWS_SECRET_ACCESS_KEY=your_secret
```

**2. 실행**
```bash
cd demo
docker compose up --build
```

PostgreSQL 컨테이너가 먼저 기동되고 헬스체크 통과 후 앱이 시작됩니다.  
DB 스키마는 최초 실행 시 자동 생성됩니다.  
서버 주소: `http://localhost:8080`

**3. 종료**
```bash
docker compose down          # 컨테이너만 종료 (DB 데이터 유지)
docker compose down -v       # 컨테이너 + DB 볼륨 삭제
```

---

### 로컬 직접 실행 (Docker 없이)

**사전 요구사항**
- JDK 17 이상
- PostgreSQL 실행 중

1. PostgreSQL에서 DB 생성
```sql
CREATE DATABASE app;
CREATE USER app WITH PASSWORD 'app';
GRANT ALL PRIVILEGES ON DATABASE app TO app;
```

2. 환경 변수 설정 후 실행
```bash
export OPENAI_API_KEY=your_key
export JWT_SECRET=your_secret
export AWS_ACCESS_KEY_ID=your_key
export AWS_SECRET_ACCESS_KEY=your_secret

./gradlew bootRun
```

서버 기본 포트: `8080`  
DB 스키마는 JPA `ddl-auto: update` 로 자동 생성됩니다.

## How to Test

**API 수동 테스트**

서버 실행 후 아래 순서로 테스트:

1. 회원가입
```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-up \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"1234","nickName":"테스터"}'
```

2. 로그인 후 발급된 `accessToken` 을 이후 요청 헤더에 포함
```bash
curl -X POST http://localhost:8080/api/v1/auth/sign-in \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"1234"}'
```

3. 이후 인증이 필요한 API는 `-H "Authorization: Bearer {accessToken}"` 헤더 추가
