# 기말고사 프로젝트 테스트 계획

**기반 문서**: `_bmad-output/epics.md`

---

## 1. 테스트 환경 설정

### 필수 조건
- Django 서버: `localhost:8000` 또는 PythonAnywhere
- Android 에뮬레이터: API 24+ (Android 7.0)
- YOLOv5 환경: Python 3.8+, PyTorch, OpenCV
- 테스트 데이터: ApiUser, GymMachine, MachineEvent 샘플 데이터

### 서버 시작
```bash
cd PhotoBlogServer
source venv/bin/activate
python manage.py runserver
```

### 테스트 데이터 생성
```bash
python manage.py shell
```
```python
from django.contrib.auth.models import User
from blog.models import ApiUser, GymMachine

# ApiUser 생성
user = User.objects.create_user(username='gym_admin', password='admin123')
api_user = ApiUser.objects.create(
    user=user,
    name='헬스장 관리자',
    security_key='test-security-key-12345'
)

# GymMachine 생성
machine = GymMachine.objects.create(
    name='런닝머신 #1',
    machine_type='treadmill',
    location='1층 A구역',
    description='ProForm 상업용 런닝머신'
)
```

---

## 2. Epic 1: Equipment Discovery & System Access

### 2.1 Backend 테스트

#### 2.1.1 인증 API 테스트

| TC | 테스트 항목 | 엔드포인트 | 예상 결과 |
|----|------------|------------|----------|
| BE-1-01 | 유효한 보안키 로그인 | `POST /api/auth/login/` | 200 OK, token 반환 |
| BE-1-02 | 잘못된 보안키 로그인 | `POST /api/auth/login/` | 401 Unauthorized |
| BE-1-03 | 빈 보안키 로그인 | `POST /api/auth/login/` | 400 Bad Request |
| BE-1-04 | 토큰 유효성 검증 | 인증 필요 API 호출 | 200 OK (유효), 401 (무효) |

**테스트 명령어:**
```bash
# BE-1-01: 유효한 보안키 로그인
curl -X POST http://localhost:8000/api/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"security_key": "test-security-key-12345"}'

# BE-1-02: 잘못된 보안키
curl -X POST http://localhost:8000/api/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"security_key": "wrong-key"}'
```

#### 2.1.2 운동기구 목록 API 테스트

| TC | 테스트 항목 | 엔드포인트 | 예상 결과 |
|----|------------|------------|----------|
| BE-1-05 | 기구 목록 조회 (인증됨) | `GET /api_root/machines/` | 200 OK, JSON 배열 |
| BE-1-06 | 기구 목록 조회 (미인증) | `GET /api_root/machines/` | 401 Unauthorized |
| BE-1-07 | 기구 상세 조회 | `GET /api_root/machines/{id}/` | 200 OK, 기구 정보 |
| BE-1-08 | 존재하지 않는 기구 조회 | `GET /api_root/machines/999/` | 404 Not Found |

**테스트 명령어:**
```bash
# BE-1-05: 기구 목록 조회
curl -H "Authorization: Token <token>" \
  http://localhost:8000/api_root/machines/
```

### 2.2 Android 테스트

#### 2.2.1 로그인 화면 테스트

| TC | 테스트 항목 | 입력 | 예상 결과 |
|----|------------|------|----------|
| AN-1-01 | 앱 최초 실행 | - | Login 화면 표시 |
| AN-1-02 | 유효한 보안키 입력 | `test-security-key-12345` | Equipment List 화면 이동 |
| AN-1-03 | 잘못된 보안키 입력 | `wrong-key` | 에러 메시지 표시 |
| AN-1-04 | 빈 입력 제출 | 빈 문자열 | 유효성 검사 에러 |
| AN-1-05 | 저장된 토큰으로 재시작 | - | 자동 로그인, Equipment List 이동 |

#### 2.2.2 운동기구 목록 화면 테스트

| TC | 테스트 항목 | 동작 | 예상 결과 |
|----|------------|------|----------|
| AN-1-06 | 기구 목록 로딩 | 화면 진입 | RecyclerView에 기구 목록 표시 |
| AN-1-07 | 기구 카드 정보 표시 | - | 이름, 위치, 상태 표시 |
| AN-1-08 | 기구 탭 | 기구 카드 클릭 | 이벤트 목록 화면 이동 (빈 상태) |
| AN-1-09 | Pull to Refresh | 당겨서 새로고침 | 목록 갱신 |
| AN-1-10 | 빈 상태 표시 | 이벤트 없는 기구 탭 | "이벤트가 없습니다" 메시지 |

---

## 3. Epic 2: Usage Event Monitoring & Detection

### 3.1 Backend 테스트

#### 3.1.1 이벤트 생성 API 테스트 (Edge → Server)

| TC | 테스트 항목 | 엔드포인트 | 예상 결과 |
|----|------------|------------|----------|
| BE-2-01 | 이벤트 생성 (start) | `POST /api/machines/{id}/events/` | 201 Created |
| BE-2-02 | 이벤트 생성 (end) | `POST /api/machines/{id}/events/` | 201 Created |
| BE-2-03 | 이미지 없이 이벤트 생성 | `POST /api/machines/{id}/events/` | 400 Bad Request |
| BE-2-04 | 잘못된 machine_id | `POST /api/machines/999/events/` | 404 Not Found |
| BE-2-05 | 미인증 이벤트 생성 | `POST /api/machines/{id}/events/` | 401 Unauthorized |

**테스트 명령어:**
```bash
# BE-2-01: 이벤트 생성
curl -X POST http://localhost:8000/api/machines/1/events/ \
  -H "Authorization: Token <token>" \
  -F "image=@test_image.jpg" \
  -F "event_type=start" \
  -F "captured_at=2024-01-15T10:30:00" \
  -F "person_count=1" \
  -F 'detections={"person": [{"bbox": [100, 50, 300, 400], "confidence": 0.95}]}' \
  -F 'change_info={"event_type": "start", "prev_count": 0, "curr_count": 1}'
```

#### 3.1.2 이벤트 조회 API 테스트

| TC | 테스트 항목 | 엔드포인트 | 예상 결과 |
|----|------------|------------|----------|
| BE-2-06 | 이벤트 목록 조회 | `GET /api/machines/{id}/events/` | 200 OK, JSON 배열 |
| BE-2-07 | event_type 필터 (start) | `?event_type=start` | start 이벤트만 반환 |
| BE-2-08 | event_type 필터 (end) | `?event_type=end` | end 이벤트만 반환 |
| BE-2-09 | 날짜 범위 필터 | `?date_from=2024-01-01&date_to=2024-01-31` | 해당 기간 이벤트만 |
| BE-2-10 | 이벤트 상세 조회 | `GET /api_root/events/{id}/` | 200 OK, 상세 정보 |
| BE-2-11 | 페이지네이션 | `?page=1&page_size=10` | 10개씩 페이징 |

### 3.2 Edge 시스템 테스트

#### 3.2.1 YOLOv5 Change Detection 테스트

| TC | 테스트 항목 | 시나리오 | 예상 결과 |
|----|------------|----------|----------|
| ED-2-01 | Start 이벤트 감지 | 사람 없음 → 사람 감지 (0→1) | start 이벤트 POST |
| ED-2-02 | End 이벤트 감지 | 사람 감지 → 사람 없음 (1→0) | end 이벤트 POST |
| ED-2-03 | 상태 유지 (변화 없음) | 사람 계속 감지 | API 호출 없음 |
| ED-2-04 | 이미지 리사이즈 | 원본 이미지 처리 | 640x480 리사이즈 |
| ED-2-05 | 서버 연결 실패 처리 | 서버 다운 | 로컬 저장, 에러 로그 |
| ED-2-06 | 복수 기기 지원 | machine_id 설정 | 올바른 machine_id로 POST |

**테스트 명령어:**
```bash
cd yolov5
source venv/bin/activate

# 웹캠으로 테스트
python detect.py --weights yolov5s.pt --source 0 --conf-thres 0.5

# 테스트 비디오로 테스트
python detect.py --weights yolov5s.pt --source test_video.mp4
```

### 3.3 Android 테스트

#### 3.3.1 사용 이력 화면 테스트

| TC | 테스트 항목 | 동작 | 예상 결과 |
|----|------------|------|----------|
| AN-2-01 | 이벤트 목록 로딩 | 화면 진입 | RecyclerView에 이벤트 표시 |
| AN-2-02 | 이벤트 카드 정보 | - | 타입, 시간, 썸네일 표시 |
| AN-2-03 | 이벤트 타입 필터 (전체) | "전체" 칩 선택 | 모든 이벤트 표시 |
| AN-2-04 | 이벤트 타입 필터 (시작) | "시작" 칩 선택 | start 이벤트만 표시 |
| AN-2-05 | 이벤트 타입 필터 (종료) | "종료" 칩 선택 | end 이벤트만 표시 |
| AN-2-06 | 날짜 범위 필터 | DatePicker 선택 | 해당 기간 이벤트만 |
| AN-2-07 | 스크롤 페이지네이션 | 하단 스크롤 | 추가 이벤트 로딩 |
| AN-2-08 | Pull to Refresh | 당겨서 새로고침 | 목록 갱신 |

#### 3.3.2 이벤트 상세 화면 테스트

| TC | 테스트 항목 | 동작 | 예상 결과 |
|----|------------|------|----------|
| AN-2-09 | 상세 화면 진입 | 이벤트 카드 탭 | 이벤트 상세 화면 표시 |
| AN-2-10 | 풀사이즈 이미지 | - | 전체 이미지 표시 |
| AN-2-11 | 메타데이터 표시 | - | 시간, 타입, 감지 정보 표시 |
| AN-2-12 | 뒤로가기 | 뒤로 버튼 | 이벤트 목록으로 복귀 |

---

## 4. Epic 3: Usage Analytics & Statistics

### 4.1 Backend 테스트

#### 4.1.1 통계 API 테스트

| TC | 테스트 항목 | 엔드포인트 | 예상 결과 |
|----|------------|------------|----------|
| BE-3-01 | 기구 통계 조회 | `GET /api_root/machines/{id}/stats/` | 200 OK, 통계 데이터 |
| BE-3-02 | 날짜 범위 통계 | `?date_from=...&date_to=...` | 해당 기간 통계 |
| BE-3-03 | 일별 사용량 집계 | - | daily_usage 배열 반환 |
| BE-3-04 | 총 시작/종료 카운트 | - | total_starts, total_ends 반환 |
| BE-3-05 | 데이터 없는 기간 | 이벤트 없는 날짜 범위 | 빈 배열 또는 0 카운트 |

**테스트 명령어:**
```bash
# BE-3-01: 기구 통계 조회
curl -H "Authorization: Token <token>" \
  "http://localhost:8000/api_root/machines/1/stats/"

# BE-3-02: 날짜 범위 통계
curl -H "Authorization: Token <token>" \
  "http://localhost:8000/api_root/machines/1/stats/?date_from=2024-01-01&date_to=2024-01-31"
```

### 4.2 Android 테스트

#### 4.2.1 사용 통계 화면 테스트

| TC | 테스트 항목 | 동작 | 예상 결과 |
|----|------------|------|----------|
| AN-3-01 | 통계 화면 진입 | 통계 메뉴 탭 | 통계 화면 표시 |
| AN-3-02 | 총 사용 횟수 카드 | - | 시작/종료 횟수 표시 |
| AN-3-03 | 일별 사용량 차트 | - | 막대/선 그래프 표시 |
| AN-3-04 | 날짜 범위 선택 | DateRangePicker | 해당 기간 통계 갱신 |
| AN-3-05 | 데이터 없음 상태 | 이벤트 없는 기구 | "데이터가 없습니다" 메시지 |
| AN-3-06 | 로딩 상태 | API 호출 중 | ProgressBar 표시 |

---

## 5. 비기능 요구사항 (NFR) 테스트

### 5.1 인증/보안 테스트

| TC | 테스트 항목 | 예상 결과 |
|----|------------|----------|
| NFR-01 | 토큰 없이 보호된 API 접근 | 401 Unauthorized |
| NFR-02 | 만료된/잘못된 토큰 사용 | 401 Unauthorized |
| NFR-03 | 로그인 후 토큰 로컬 저장 | SharedPreferences에 저장 |

### 5.2 호환성 테스트

| TC | 테스트 항목 | 예상 결과 |
|----|------------|----------|
| NFR-04 | 기존 Photo Blog API 유지 | `/api_root/Post/` 정상 동작 |
| NFR-05 | 기존 토큰 인증 유지 | `/api-token-auth/` 정상 동작 |
| NFR-06 | Android SDK 24 최소 지원 | API 24 에뮬레이터에서 정상 동작 |

### 5.3 에러 처리 테스트

| TC | 테스트 항목 | 시나리오 | 예상 결과 |
|----|------------|----------|----------|
| NFR-07 | 네트워크 오류 처리 | 서버 다운 | 에러 메시지 표시 |
| NFR-08 | 타임아웃 처리 | 느린 응답 | 적절한 타임아웃 후 에러 |
| NFR-09 | JSON 파싱 오류 | 잘못된 응답 형식 | 에러 로그, 사용자 알림 |

---

## 6. 통합 테스트 시나리오

### 6.1 E2E 시나리오 1: 전체 사용 흐름

1. Android 앱 실행 → Login 화면
2. 보안키 입력 → Equipment List 화면
3. 기구 탭 → Usage History 화면 (빈 상태)
4. Edge 시스템: 사람 감지 (0→1) → start 이벤트 POST
5. Android: Pull to Refresh → start 이벤트 표시
6. Edge 시스템: 사람 퇴장 (1→0) → end 이벤트 POST
7. Android: Pull to Refresh → end 이벤트 추가
8. 이벤트 탭 → Event Detail 화면
9. Statistics 화면 진입 → 통계 확인

### 6.2 E2E 시나리오 2: 필터링 흐름

1. 로그인 완료 상태에서 시작
2. Equipment List → 특정 기구 선택
3. Usage History: 전체 이벤트 확인
4. "시작" 필터 적용 → start 이벤트만 표시
5. "종료" 필터 적용 → end 이벤트만 표시
6. 날짜 범위 설정 → 해당 기간 이벤트만 표시
7. 필터 초기화 → 전체 이벤트 복원

---

## 7. Logcat 확인 필터

### Android 디버깅
```
Tag: MainActivity, LoginActivity, EventAdapter, ApiClient
Level: Debug
```

### 성공 로그 예시
```
D/LoginActivity: Login successful, token saved
D/MainActivity: Machines loaded: 5 items
D/EventAdapter: Events loaded: 15 items
D/EventDetailActivity: Event detail loaded: id=1
```

### 에러 로그 예시
```
E/ApiClient: Network error: Connection refused
E/MainActivity: Failed to load machines: 401 Unauthorized
E/EventAdapter: JSON parsing error: Unexpected token
```

---

## 8. 테스트 결과 기록 템플릿

| Epic | TC ID | 테스트 항목 | 결과 | 비고 |
|------|-------|------------|------|------|
| 1 | BE-1-01 | 유효한 보안키 로그인 | PASS/FAIL | |
| 1 | AN-1-02 | 유효한 보안키 입력 | PASS/FAIL | |
| 2 | ED-2-01 | Start 이벤트 감지 | PASS/FAIL | |
| ... | ... | ... | ... | ... |
