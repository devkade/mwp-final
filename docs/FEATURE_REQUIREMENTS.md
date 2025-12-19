# 기능 요구사항 충족 현황

## 개요

본 문서는 Gym Equipment Detection System의 기능 요구사항 충족 여부와 구현 위치를 정리합니다.

**범례**: ✅ 구현 완료 | ⚠️ 부분 구현 | ❌ 미구현

---

## 1. Edge 시스템 (YOLOv5)

| 번호 | 요구사항 | 상태 | 구현 위치 |
|------|----------|------|-----------|
| 1-1 | YOLOv5 pretrained model 사용 | ✅ | `yolov5/yolov5s.pt` (14MB) |
| 1-2 | MS COCO 80 클래스 검출 | ✅ | `yolov5/data/coco.yaml:16-97` |
| 1-3 | Change Detection | ✅ | `yolov5/changedetection.py:51-137` |
| 1-4 | HTTP RESTful API 사용 | ✅ | `yolov5/changedetection.py:139-182` |
| 1-5 | 환경변수/YAML 설정 지원 | ✅ | `yolov5/changedetection.py:17-48` |

### 1-1. YOLOv5 Pretrained Model

- **파일**: `yolov5/yolov5s.pt`
- **크기**: 14MB
- **모델**: YOLOv5s (Small) - 속도와 정확도 균형
- **대체 가능**: yolov5m.pt, yolov5l.pt, yolov5x.pt 등으로 교체 가능

### 1-2. MS COCO 80 클래스

- **설정 파일**: `yolov5/data/coco.yaml`
- **클래스 목록** (80개):
  - 사람: person
  - 탈것: bicycle, car, motorcycle, airplane, bus, train, truck, boat
  - 동물: bird, cat, dog, horse, sheep, cow, elephant, bear, zebra, giraffe
  - 물건: backpack, umbrella, handbag, tie, suitcase, frisbee, skis, snowboard, sports ball, kite, baseball bat, baseball glove, skateboard, surfboard, tennis racket, bottle, wine glass, cup, fork, knife, spoon, bowl, banana, apple, sandwich, orange, broccoli, carrot, hot dog, pizza, donut, cake, chair, couch, potted plant, bed, dining table, toilet, tv, laptop, mouse, remote, keyboard, cell phone, microwave, oven, toaster, sink, refrigerator, book, clock, vase, scissors, teddy bear, hair drier, toothbrush

### 1-3. Change Detection (양방향 감지)

- **클래스**: `ChangeDetection`
- **파일**: `yolov5/changedetection.py:51-137`

```python
# 감지 로직 (75-108행)
# 0 → 1+: 사용 시작 (start)
if prev_count == 0 and curr_count >= 1:
    event_type = 'start'
# 1+ → 0: 사용 종료 (end)
elif prev_count >= 1 and curr_count == 0:
    event_type = 'end'
```

- **이벤트 타입**:
  - `start`: 객체가 없다가 나타남 (0→1+)
  - `end`: 객체가 있다가 사라짐 (1+→0)

### 1-4. HTTP RESTful API 호출

- **인증**: `yolov5/changedetection.py:139-152`
  - Endpoint: `POST /api/auth/login/`
  - Body: `{"security_key": "xxx"}`
  - Response: `{"token": "xxx", "name": "xxx"}`

- **이벤트 전송**: `yolov5/changedetection.py:154-182`
  - Endpoint: `POST /api/machines/{machine_id}/events/`
  - Method: multipart/form-data
  - Fields: `event_type`, `captured_at`, `person_count`, `detections`, `change_info`, `image`

### 1-5. 설정 지원

- **환경변수**:
  - `GYM_HOST`: API 서버 주소 (기본: https://mouseku.pythonanywhere.com)
  - `GYM_SECURITY_KEY`: 보안키
  - `GYM_MACHINE_ID`: 운동기구 ID (기본: 1)
  - `GYM_TARGET_CLASS`: 감지 대상 클래스 (기본: person)

---

## 2. Backend 시스템 (Django)

| 번호 | 요구사항 | 상태 | 구현 위치 |
|------|----------|------|-----------|
| 2-1 | 보안키 기반 로그인 | ✅ | `blog/views.py:56-84`, `blog/models.py:22-35` |
| 2-2 | Image Blog 관리 기능 | ✅ | `blog/models.py:6-19`, `blog/admin.py` |
| 2-3 | 게시용 HTTP RESTful API | ✅ | `blog/views.py:86-92` |
| 2-4 | Image 목록/획득 API | ✅ | `blog/views.py:86-92, 130-207` |
| 2-5 | 운동기구 관리 API | ✅ | `blog/views.py:95-127` |
| 2-6 | 이벤트 저장/조회 API | ✅ | `blog/views.py:130-207` |
| 2-7 | 통계 API | ✅ | `blog/views.py:101-127` |

### 2-1. 보안키 기반 로그인

- **모델**: `ApiUser` (`blog/models.py:22-35`)
  ```python
  class ApiUser(models.Model):
      name = models.CharField(max_length=100)
      security_key = models.CharField(max_length=64, unique=True)
      user = models.OneToOneField(User, on_delete=models.CASCADE)
      is_active = models.BooleanField(default=True)
  ```

- **API**: `POST /api/auth/login/` (`blog/views.py:56-84`)
  - Request: `{"security_key": "xxx"}`
  - Response: `{"token": "xxx", "name": "xxx"}`

### 2-2. Image Blog 관리

- **모델**: `Post` (`blog/models.py:6-19`)
  ```python
  class Post(models.Model):
      author = models.ForeignKey(User, on_delete=models.CASCADE)
      title = models.CharField(max_length=200)
      text = models.TextField()
      image = models.ImageField(upload_to='blog_image/%Y/%m/%d/')
      created_date = models.DateTimeField(default=timezone.now)
      published_date = models.DateTimeField(blank=True, null=True)
  ```

- **Admin**: `/admin/blog/post/` 에서 관리 가능

### 2-3. 게시용 API

- **Endpoint**: `POST /api_root/Post/`
- **ViewSet**: `BlogImages` (`blog/views.py:86-92`)
- **인증**: Token 기반 (`Authorization: Token xxx`)
- **Content-Type**: `multipart/form-data`

### 2-4. Image 목록/획득 API

| Endpoint | Method | 설명 |
|----------|--------|------|
| `/api_root/Post/` | GET | 게시물 목록 (페이지네이션) |
| `/api_root/Post/{id}/` | GET | 게시물 상세 |
| `/api_root/machines/` | GET | 운동기구 목록 |
| `/api_root/machines/{id}/` | GET | 운동기구 상세 |
| `/api_root/events/` | GET | 이벤트 목록 |
| `/api_root/events/{id}/` | GET | 이벤트 상세 |
| `/api/machines/{id}/events/` | GET | 기구별 이벤트 목록 |

### 2-5. 운동기구 관리 API

- **모델**: `GymMachine` (`blog/models.py:38-67`)
- **ViewSet**: `GymMachineViewSet` (`blog/views.py:95-127`)
- **기구 타입**: treadmill, bench_press, squat_rack, lat_pulldown, leg_press, cable_machine, dumbbell, other

### 2-6. 이벤트 저장/조회 API

- **모델**: `MachineEvent` (`blog/models.py:70-106`)
- **생성 API**: `POST /api/machines/{id}/events/` (`blog/views.py:169-207`)
- **필수 필드**: `machine`, `event_type`, `image`, `captured_at`
- **선택 필드**: `person_count`, `detections`, `change_info`

### 2-7. 통계 API

- **Endpoint**: `GET /api_root/machines/{id}/stats/`
- **Query Params**: `date_from`, `date_to` (YYYY-MM-DD)
- **Response**:
  ```json
  {
    "machine_id": 1,
    "machine_name": "철봉",
    "total_starts": 10,
    "total_ends": 8,
    "daily_usage": [
      {"date": "2025-12-19", "count": 5}
    ]
  }
  ```

---

## 3. Android 클라이언트

| 번호 | 요구사항 | 상태 | 구현 위치 |
|------|----------|------|-----------|
| 3-1 | Image list view | ✅ | `MainActivity.java`, `MachineListActivity.java` |
| 3-2 | HTTP RESTful API 사용 | ✅ | `services/GymApiService.java` |
| 3-3 | 사용자 시나리오 및 UI | ✅ | 아래 상세 참조 |

### 3-1. Image List View

- **게시물 목록**: `MainActivity.java:55-100`
  - RecyclerView + ImageAdapter
  - Pull-to-refresh 지원

- **운동기구 목록**: `MachineListActivity.java:36-100`
  - RecyclerView + MachineAdapter
  - 에러/빈 상태 처리

### 3-2. HTTP RESTful API 사용

- **서비스 클래스**: `services/GymApiService.java:30-100`
- **인증 서비스**: `services/AuthenticationService.java`
- **세션 관리**: `services/SessionManager.java`, `utils/SecureTokenManager.java`

| API 호출 | 메서드 |
|----------|--------|
| 기구 목록 | `getMachines(MachinesCallback)` |
| 이벤트 목록 | `getEvents(machineId, EventsCallback)` |
| 이벤트 상세 | `getEventDetail(eventId, EventDetailCallback)` |
| 통계 | `getStats(machineId, dateFrom, dateTo, StatsCallback)` |

### 3-3. 사용자 시나리오 및 UI

| 화면 | Activity | 기능 |
|------|----------|------|
| 로그인 | `LoginActivity.java` | 보안키 입력 → 토큰 획득 |
| 기구 목록 | `MachineListActivity.java` | 운동기구 리스트, 클릭 시 이벤트 목록 |
| 이벤트 목록 | `EventListActivity.java` | 기구별 이벤트, 필터링 (날짜/타입) |
| 이벤트 상세 | `EventDetailActivity.java` | 이벤트 상세 정보, 이미지 표시 |
| 통계 | `StatsActivity.java` | 사용 통계 (MPAndroidChart BarChart) |

**사용자 플로우**:
```
로그인 → 기구 목록 → 기구 선택 → 이벤트 목록 → 이벤트 상세
                              ↘ 통계 보기
```

---

## 추가 구현 기능

| 번호 | 기능 | 구현 위치 |
|------|------|-----------|
| A-1 | 페이지네이션 | `mysite/settings.py:72-74` (PAGE_SIZE=20) |
| A-2 | 이벤트 이미지 저장 | `yolov5/changedetection.py:197-221` |
| A-3 | YOLO 검출 결과 JSON 저장 | `MachineEvent.detections` 필드 |
| A-4 | 날짜별 필터링 | `blog/views.py:175-180` |
| A-5 | Build 환경별 API URL 자동 전환 | `build.gradle.kts` (debug/release) |

---

## 테스트 현황

- **Django 백엔드 테스트**: `PhotoBlogServer/blog/tests/` (24개 테스트)
- **Android 테스트**: `PhotoViewer/app/src/test/` (단위 테스트)
- **수동 테스트 문서**: `docs/tests/` (4개 시나리오)

자세한 테스트 실행 방법은 [PROGRESS_AND_TESTING.md](./PROGRESS_AND_TESTING.md) 참조.
