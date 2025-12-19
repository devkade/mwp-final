# 프로젝트 진행 현황 및 테스트 가이드

> 최종 업데이트: 2025-12-19

## 프로젝트 개요

**Gym Equipment Detection System** - 헬스장 운동기구 사용 감지 및 모니터링 시스템

| 컴포넌트 | 기술 스택 | 설명 |
|---------|----------|------|
| PhotoBlogServer | Django REST Framework | 백엔드 API 서버 |
| PhotoViewer | Android (Java) | 모바일 클라이언트 |
| YOLOv5 | Python + PyTorch | Edge 객체 감지 시스템 |

---

## 진행 현황

### Epic 1: Equipment Discovery & System Access (review)

| Story | 상태 | 설명 |
|-------|------|------|
| 1-1 Backend Authentication Setup | review | 토큰 인증 시스템 |
| 1-2 Backend Equipment API | review | 운동기구 CRUD API |
| 1-3 Android Login Screen | review | 로그인 화면 |
| 1-4 Android Equipment List Screen | review | 운동기구 목록 화면 |

### Epic 2: Usage Event Monitoring & Detection (done)

| Story | 상태 | 설명 |
|-------|------|------|
| 2-1 Backend Event Model & Posting API | done | 이벤트 모델 및 POST API |
| 2-2 Backend Event List/Detail API | done | 이벤트 조회 API (페이징) |
| 2-3 Edge Bidirectional Change Detection | done | 양방향 변화 감지 (시작/종료) |
| 2-4 Edge Event Upload Integration | done | Edge→Backend 이벤트 업로드 |
| 2-5 Android Event List Screen | done | 이벤트 목록 화면 |
| 2-6 Android Event Detail Screen | done | 이벤트 상세 화면 |

### Epic 3: Usage Analytics & Statistics (done)

| Story | 상태 | 설명 |
|-------|------|------|
| 3-1 Backend Statistics API | done | 통계 API 엔드포인트 |
| 3-2 Android Statistics Screen | done | 통계 화면 (바 차트) |

---

## 환경 설정

### 1. Django Backend (PhotoBlogServer)

```bash
cd PhotoBlogServer

# 가상환경 생성 및 활성화
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 데이터베이스 마이그레이션
python manage.py migrate

# 슈퍼유저 생성 (최초 1회)
python manage.py createsuperuser

# 개발 서버 실행
python manage.py runserver
```

**서버 주소**: http://127.0.0.1:8000

### 2. Android Client (PhotoViewer)

```bash
cd PhotoViewer

# Gradle 빌드
./gradlew build

# APK 생성
./gradlew assembleDebug
```

**요구사항**:
- Java 17+
- Android SDK (API 24-36)
- Android Studio (권장)

### 3. YOLOv5 Edge System

```bash
cd yolov5

# 가상환경 생성 및 활성화
python -m venv venv
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt
```

---

## 테스트 실행

### Django Backend 테스트

```bash
cd PhotoBlogServer
source venv/bin/activate

# 전체 테스트 실행
python manage.py test

# 앱별 테스트
python manage.py test blog.tests.test_machines   # 운동기구 API
python manage.py test blog.tests.test_events     # 이벤트 API
python manage.py test blog.tests.test_stats      # 통계 API
python manage.py test blog.tests.test_posts      # 블로그 포스트 API

# 테스트 커버리지 (coverage 설치 필요)
coverage run --source='blog' manage.py test
coverage report
```

**테스트 파일 목록**:
- `blog/tests/test_machines.py` - 운동기구 CRUD 테스트
- `blog/tests/test_events.py` - 이벤트 생성/조회 테스트
- `blog/tests/test_stats.py` - 통계 API 테스트
- `blog/tests/test_posts.py` - 블로그 포스트 테스트

### Android Unit 테스트

```bash
cd PhotoViewer

# 전체 Unit 테스트
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.example.photoviewer.GymMachineTest"
./gradlew test --tests "com.example.photoviewer.MachineEventTest"
./gradlew test --tests "com.example.photoviewer.MachineStatsTest"
```

**테스트 파일 목록**:
- `GymMachineTest.java` - 운동기구 모델 테스트
- `MachineEventTest.java` - 이벤트 모델 테스트
- `MachineStatsTest.java` - 통계 모델 테스트
- `LoginActivityTest.java` - 로그인 화면 테스트
- `MachineListActivityTest.java` - 운동기구 목록 테스트

### Android Instrumented 테스트

```bash
cd PhotoViewer

# 에뮬레이터 또는 실제 기기 연결 필요
./gradlew connectedAndroidTest
```

### YOLOv5 Edge 테스트

```bash
cd yolov5
source venv/bin/activate

# Change Detection 테스트
python -m pytest tests/test_changedetection.py -v

# 전체 테스트
python -m pytest tests/ -v
```

**테스트 파일 목록**:
- `tests/test_changedetection.py` - 양방향 변화 감지 테스트 (18개 테스트 케이스)

---

## 통합 테스트 시나리오

### 시나리오 1: 운동기구 조회 플로우

1. Django 서버 실행
2. Android 앱에서 로그인
3. 운동기구 목록 조회 확인
4. 개별 운동기구 선택 → 이벤트 목록 표시

```bash
# 1. 서버 실행
cd PhotoBlogServer && source venv/bin/activate && python manage.py runserver

# 2. API 테스트 (curl)
curl -H "Authorization: Token <your-token>" http://127.0.0.1:8000/api_root/machines/
```

### 시나리오 2: 이벤트 감지 및 업로드 플로우

1. Django 서버 실행
2. YOLOv5 웹캠 감지 시작
3. 사람 감지 시 자동 이벤트 생성
4. Android 앱에서 이벤트 확인

```bash
# 1. 서버 실행
cd PhotoBlogServer && source venv/bin/activate && python manage.py runserver

# 2. YOLOv5 감지 시작 (별도 터미널)
cd yolov5 && source venv/bin/activate
python detect.py --weights yolov5s.pt --source 0
```

### 시나리오 3: 통계 조회 플로우

1. Django 서버에 이벤트 데이터 존재
2. Android 앱에서 통계 화면 진입
3. 운동기구별 사용 통계 확인

```bash
# 통계 API 테스트
curl -H "Authorization: Token <your-token>" \
  "http://127.0.0.1:8000/api_root/machines/{id}/stats/?period=week"
```

---

## API 엔드포인트 요약

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| POST | `/api-token-auth/` | 토큰 발급 |
| GET | `/api_root/machines/` | 운동기구 목록 |
| GET | `/api_root/machines/{id}/` | 운동기구 상세 |
| GET | `/api_root/machines/{id}/stats/` | 운동기구 통계 |
| GET | `/api_root/events/` | 이벤트 목록 (페이징) |
| GET | `/api_root/events/{id}/` | 이벤트 상세 |
| POST | `/api_root/events/` | 이벤트 생성 |

**인증**: 모든 API는 `Authorization: Token <token>` 헤더 필요

---

## 문제 해결

### Django 서버

```bash
# 마이그레이션 오류
python manage.py makemigrations
python manage.py migrate

# 정적 파일 오류
python manage.py collectstatic
```

### Android 빌드

```bash
# 캐시 클리어 후 재빌드
./gradlew clean
./gradlew build

# Java 버전 확인
java -version  # Java 17+ 필요
```

### YOLOv5

```bash
# PyTorch 설치 확인
python -c "import torch; print(torch.__version__)"

# CUDA 사용 가능 여부
python -c "import torch; print(torch.cuda.is_available())"
```

---

## 디렉토리 구조

```
mwp-finalterm-blog/
├── PhotoBlogServer/          # Django Backend
│   ├── blog/
│   │   ├── models.py         # Post, Machine, MachineEvent 모델
│   │   ├── views.py          # API ViewSets
│   │   ├── serializers.py    # DRF Serializers
│   │   └── tests/            # 테스트 파일
│   └── mysite/               # Django 설정
│
├── PhotoViewer/              # Android Client
│   └── app/src/main/java/com/example/photoviewer/
│       ├── LoginActivity.java
│       ├── MachineListActivity.java
│       ├── EventListActivity.java
│       ├── EventDetailActivity.java
│       ├── StatsActivity.java
│       ├── models/           # 데이터 모델
│       └── services/         # API 서비스
│
├── yolov5/                   # Edge Detection
│   ├── detect.py             # 메인 감지 스크립트
│   ├── changedetection.py    # 변화 감지 모듈
│   └── tests/                # 테스트 파일
│
└── _bmad-output/             # 프로젝트 관리 문서
    ├── sprint/               # 스프린트 상태
    └── stories/              # 스토리 파일
```
