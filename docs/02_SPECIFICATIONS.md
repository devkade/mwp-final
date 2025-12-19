# 시스템 스펙 (System Specifications)

## 1. 기술 스택

| 컴포넌트 | 기술 | 버전 | 용도 |
|---------|------|------|------|
| **Backend** | Django | 5.2 | 웹 프레임워크 |
| | Django REST Framework | 3.15 | REST API |
| | SQLite3 | - | 데이터베이스 |
| | Pillow | - | 이미지 처리 |
| **Android** | Java | 17 | 앱 개발 |
| | Android SDK | API 36 | 타겟 플랫폼 |
| | Min SDK | API 24 | 최소 지원 (Android 7.0) |
| | MPAndroidChart | - | 차트 라이브러리 |
| **Edge** | Python | 3.14 | 런타임 |
| | PyTorch | - | 딥러닝 프레임워크 |
| | YOLOv5s | - | 객체 감지 모델 |
| | OpenCV | - | 영상 처리 |

---

## 2. API 스펙

### 2.1 인증 API

#### POST `/api/auth/login/`

보안키로 로그인하여 인증 토큰 획득

| 항목 | 값 |
|------|-----|
| Method | POST |
| Auth | None |
| Content-Type | application/json |

**Request Body:**
```json
{
  "security_key": "string (64자)"
}
```

**Response (200 OK):**
```json
{
  "token": "string",
  "name": "string"
}
```

**Error Responses:**
- `401 Unauthorized`: 유효하지 않거나 비활성 보안키

---

### 2.2 기구 API

#### GET `/api_root/machines/`

활성 운동 기구 목록 조회

| 항목 | 값 |
|------|-----|
| Method | GET |
| Auth | Token |
| Pagination | 20 items/page |

**Response:**
```json
{
  "count": 10,
  "next": "url | null",
  "previous": "url | null",
  "results": [
    {
      "id": 1,
      "name": "런닝머신 01",
      "machine_type": "treadmill",
      "location": "1층 A구역",
      "thumbnail": "/media/machines/xxx.jpg",
      "is_active": true,
      "event_count": 42,
      "last_event": {
        "event_type": "end",
        "captured_at": "2025-12-19T18:30:00Z"
      }
    }
  ]
}
```

#### GET `/api_root/machines/{id}/stats/`

기구별 사용 통계 조회

| 항목 | 값 |
|------|-----|
| Method | GET |
| Auth | Token |
| Query Params | date_from, date_to (YYYY-MM-DD) |

**Response:**
```json
{
  "machine_id": 1,
  "machine_name": "런닝머신 01",
  "total_starts": 42,
  "total_ends": 40,
  "daily_usage": [
    {"date": "2025-12-18", "count": 5},
    {"date": "2025-12-19", "count": 8}
  ]
}
```

---

### 2.3 이벤트 API

#### GET `/api/machines/{machine_id}/events/`

기구별 이벤트 목록 조회

| 항목 | 값 |
|------|-----|
| Method | GET |
| Auth | Token |
| Pagination | 20 items/page |

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| event_type | string | N | start / end |
| date_from | string | N | YYYY-MM-DD |
| date_to | string | N | YYYY-MM-DD |

**Response:**
```json
{
  "count": 50,
  "next": "url | null",
  "previous": "url | null",
  "results": [
    {
      "id": 100,
      "machine": 1,
      "machine_name": "런닝머신 01",
      "event_type": "start",
      "event_type_display": "사용 시작",
      "image": "/media/events/2025/12/19/capture.jpg",
      "captured_at": "2025-12-19T19:30:00Z",
      "person_count": 1
    }
  ]
}
```

#### POST `/api/machines/{machine_id}/events/`

이벤트 생성 (Edge에서 호출)

| 항목 | 값 |
|------|-----|
| Method | POST |
| Auth | Token |
| Content-Type | multipart/form-data |

**Request Fields:**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| event_type | string | Y | start / end |
| captured_at | datetime | Y | ISO 8601 형식 |
| person_count | integer | Y | 감지된 인원 수 |
| image | file | Y | 캡처 이미지 |
| detections | JSON string | N | YOLO 감지 결과 |
| change_info | JSON string | N | 상태 변화 정보 |

**Response (201 Created):** 생성된 이벤트 객체

---

## 3. 데이터베이스 스펙

### 3.1 ApiUser (보안키 사용자)

```sql
CREATE TABLE blog_apiuser (
  id          INTEGER PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  security_key VARCHAR(64) UNIQUE NOT NULL,
  user_id     INTEGER UNIQUE REFERENCES auth_user(id),
  is_active   BOOLEAN DEFAULT TRUE,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 3.2 GymMachine (운동 기구)

```sql
CREATE TABLE blog_gymmachine (
  id          INTEGER PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  machine_type VARCHAR(20) NOT NULL,
  location    VARCHAR(100),
  description TEXT,
  thumbnail   VARCHAR(200),
  is_active   BOOLEAN DEFAULT TRUE,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**machine_type 값:**
| 값 | 설명 |
|-----|------|
| treadmill | 런닝머신 |
| bench_press | 벤치프레스 |
| squat_rack | 스쿼트랙 |
| lat_pulldown | 랫풀다운 |
| leg_press | 레그프레스 |
| cable_machine | 케이블머신 |
| dumbbell | 덤벨존 |
| other | 기타 |

### 3.3 MachineEvent (사용 이벤트)

```sql
CREATE TABLE blog_machineevent (
  id           INTEGER PRIMARY KEY,
  machine_id   INTEGER NOT NULL REFERENCES blog_gymmachine(id),
  event_type   VARCHAR(10) NOT NULL,  -- 'start' or 'end'
  image        VARCHAR(200),
  captured_at  DATETIME NOT NULL,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  person_count INTEGER DEFAULT 0,
  detections   TEXT,  -- JSON
  change_info  TEXT   -- JSON
);

CREATE INDEX idx_event_machine_date ON blog_machineevent(machine_id, captured_at);
CREATE INDEX idx_event_type_date ON blog_machineevent(event_type, captured_at);
```

---

## 4. 설정 스펙

### 4.1 Django 설정 (mysite/settings.py)

```python
# 언어/시간대
LANGUAGE_CODE = "ko"
TIME_ZONE = "Asia/Seoul"

# 허용 호스트
ALLOWED_HOSTS = [
    "127.0.0.1",
    "10.0.2.2",  # Android 에뮬레이터
    ".pythonanywhere.com"
]

# REST Framework
REST_FRAMEWORK = {
    'DEFAULT_AUTHENTICATION_CLASSES': [
        'rest_framework.authentication.TokenAuthentication',
        'rest_framework.authentication.SessionAuthentication',
    ],
    'DEFAULT_PAGINATION_CLASS': 'rest_framework.pagination.PageNumberPagination',
    'PAGE_SIZE': 20
}

# 미디어 파일
MEDIA_URL = '/media/'
MEDIA_ROOT = BASE_DIR / 'media'
```

### 4.2 YOLOv5 Edge 설정

**환경변수:**

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| GYM_HOST | https://mouseku.pythonanywhere.com | API 서버 주소 |
| GYM_SECURITY_KEY | (필수) | 인증 보안키 |
| GYM_MACHINE_ID | 1 | 대상 기구 ID |
| GYM_TARGET_CLASS | person | 감지 대상 클래스 |

**YAML 설정 (config/gym_detection.yaml):**
```yaml
host: https://mouseku.pythonanywhere.com
security_key: your-key-here
machine_id: 1
target_class: person
```

### 4.3 Android 빌드 설정

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        minSdk = 24
        targetSdk = 36
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://mouseku.pythonanywhere.com/\"")
        }
    }
}
```

---

## 5. 성능 스펙

### 5.1 Edge 시스템

| 항목 | 값 |
|------|-----|
| 디바운스 시간 | 5.0초 |
| 이미지 저장 크기 | 640x480 |
| 지원 소스 | 웹캠, RTSP, 동영상 파일 |

### 5.2 API 서버

| 항목 | 값 |
|------|-----|
| 페이지 크기 | 20 items/page |
| 이미지 저장 경로 | media/events/{년}/{월}/{일}/ |
| 최대 요청 크기 | 10MB (이미지 포함) |

### 5.3 Android 앱

| 항목 | 값 |
|------|-----|
| 최소 안드로이드 버전 | 7.0 (API 24) |
| 네트워크 타임아웃 | 30초 |
| 이미지 캐싱 | 메모리 내 (세션 한정) |

---

## 6. 요구사항 충족 현황

| 시스템 | 요구사항 | 구현 | 구현율 |
|--------|----------|------|--------|
| Edge System | 5개 | 5개 | 100% |
| Service System | 6개 | 6개 | 100% |
| Client System | 4개 | 4개 | 100% |
| **합계** | **15개** | **15개** | **100%** |

---

*문서 버전: 1.0 | 작성일: 2025-12-20*
