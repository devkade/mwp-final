# Test Design: Epic 2 - Usage Event Monitoring & Detection

**Date:** 2025-12-19
**Author:** Kade
**Status:** Draft

---

## Executive Summary

**Scope:** Full test design for Epic 2

**Risk Summary:**
- Total risks identified: 10
- High-priority risks (≥6): 4
- Critical categories: SEC, PERF, DATA

**Coverage Summary:**
- P0 scenarios: 24 tests (48 hours)
- P1 scenarios: 26 tests (26 hours)
- P2/P3 scenarios: 19 tests (9.5 hours)
- **Total effort**: 83.5 hours (~11 days)

---

## Risk Assessment

### High-Priority Risks (Score ≥6)

| Risk ID | Category | Description | Probability | Impact | Score | Mitigation | Owner | Timeline |
|---------|----------|-------------|-------------|--------|-------|------------|-------|----------|
| R-201 | SEC | Edge 토큰 하드코딩/환경변수 노출 | 2 | 3 | 6 | 환경변수 또는 config 파일로 분리, .gitignore 추가 | Edge | Sprint 2 |
| R-202 | DATA | Event 이미지 업로드 실패 시 데이터 손실 | 3 | 2 | 6 | 로컬 큐잉 + 재시도 로직 구현 | Edge | Sprint 2 |
| R-203 | PERF | 대량 이벤트 조회 시 API 응답 지연 | 2 | 3 | 6 | 페이지네이션, 인덱스 최적화 | Backend | Sprint 2 |
| R-204 | DATA | Change Detection 오탐지 (false positive/negative) | 3 | 2 | 6 | confidence threshold 조정, 디바운싱 로직 | Edge | Sprint 2 |

### Medium-Priority Risks (Score 3-4)

| Risk ID | Category | Description | Probability | Impact | Score | Mitigation | Owner |
|---------|----------|-------------|-------------|--------|-------|------------|-------|
| R-205 | TECH | YOLOv5 모델 로딩 실패/메모리 부족 | 1 | 3 | 3 | 경량 모델 사용, 에러 핸들링 | Edge |
| R-206 | TECH | multipart/form-data 이미지 업로드 파싱 오류 | 2 | 2 | 4 | 서버 측 파일 크기 제한, 검증 로직 | Backend |
| R-207 | BUS | 이벤트 필터링 결과 불일치 (날짜/타입) | 2 | 2 | 4 | 필터 로직 단위 테스트 강화 | Backend |
| R-208 | OPS | Android 이미지 로딩 OOM (Out of Memory) | 2 | 2 | 4 | 썸네일 크기 제한, Glide/Picasso 사용 | Android |

### Low-Priority Risks (Score 1-2)

| Risk ID | Category | Description | Probability | Impact | Score | Action |
|---------|----------|-------------|-------------|--------|-------|--------|
| R-209 | BUS | 이벤트 상세 화면 메타데이터 표시 오류 | 1 | 2 | 2 | UI 테스트로 검증 |
| R-210 | OPS | 이미지 저장 경로 권한 문제 (Edge) | 1 | 2 | 2 | 디렉토리 생성 로직 추가 |

---

## Test Coverage Plan

### P0 (Critical) - Run on every commit

**Criteria**: Blocks core journey + High risk (≥6) + No workaround

| Requirement | Test Level | Risk Link | Test Count | Owner | Notes |
|-------------|------------|-----------|------------|-------|-------|
| FR8: Event Posting API | API | R-202, R-206 | 6 | QA | start/end, 이미지, JSON 필드 검증 |
| FR11: Event List API (필터링) | API | R-203, R-207 | 5 | QA | event_type, date_from/to, pagination |
| FR3: Change Detection (start) | Unit | R-204 | 3 | DEV | 0→1 감지, 상태 관리 |
| FR3: Change Detection (end) | Unit | R-204 | 2 | DEV | 1→0 감지 |
| FR4: Edge → Server 전송 | Integration | R-201, R-202 | 4 | QA | 인증, 401 재인증, 재시도 큐 |
| **NEW** Edge 401 재인증 후 재전송 | Integration | R-201 | 2 | QA | 토큰 만료 → 재인증 → pending 전송 |
| **NEW** 서버 복구 후 큐 재전송 | Integration | R-202 | 2 | QA | 서버 다운 → 복구 → 자동 재전송 |

**Total P0**: 24 tests, 48 hours

### P1 (High) - Run on PR to main

**Criteria**: Important features + Medium risk (3-4) + Common workflows

| Requirement | Test Level | Risk Link | Test Count | Owner | Notes |
|-------------|------------|-----------|------------|-------|-------|
| FR18: Android Event List 화면 | E2E | R-208 | 8 | QA | 목록, 필터, Empty State, 스크롤, 날짜필터 |
| FR19: Android Event Detail 화면 | E2E | R-209 | 5 | QA | 이미지 로딩, 메타데이터, 뒤로가기 |
| FR12: Event Detail API | API | - | 3 | QA | 상세 조회, 404 처리 |
| FR5: Edge 데이터 형식 | Unit | - | 4 | DEV | detections JSON, change_info JSON |
| **NEW** Android 날짜 범위 필터 UI | E2E | R-207 | 3 | QA | DateRangePicker 선택 및 적용 |
| **NEW** 페이지네이션 "더 보기" 동작 | E2E | R-203 | 2 | QA | 추가 데이터 로딩 |
| **NEW** Event Detail 변화 정보 표시 | E2E | R-209 | 2 | QA | 이전/현재 인원, 대상 클래스 |

**Total P1**: 27 tests, 27 hours

### P2 (Medium) - Run nightly/weekly

**Criteria**: Secondary features + Low risk (1-2) + Edge cases

| Requirement | Test Level | Test Count | Owner | Notes |
|-------------|------------|------------|-------|-------|
| FR1: YOLOv5 모델 로딩 | Unit | 2 | DEV | 모델 초기화, 에러 핸들링 |
| FR2: COCO 80 클래스 감지 | Integration | 2 | DEV | person 외 클래스 처리 |
| NFR6: 이미지 리사이즈 640x480 | Unit | 2 | DEV | 크기 검증, 품질 |
| FR14: EventAdapter | Component | 3 | DEV | ViewHolder, 클릭, 타입별 색상 |
| FR9: Image Retrieval API | API | 2 | QA | 이미지 URL 접근, 404 |
| NFR8: 네트워크 에러 (Event List) | E2E | 3 | QA | 오프라인, 타임아웃 |
| **NEW** 이벤트 시간 표시 형식 | E2E | 1 | QA | "2024-01-15 10:30:00" 포맷 검증 |
| **NEW** 화면 회전 상태 유지 | E2E | 2 | QA | EventList/Detail 회전 시 데이터 유지 |
| **NEW** 다중 person 감지 count 정확성 | Unit | 2 | DEV | 1→2, 2→3 시 count 값 검증 |

**Total P2**: 19 tests, 9.5 hours

---

## Execution Order

### Smoke Tests (<5 min)

**Purpose**: Fast feedback, catch build-breaking issues

- [ ] POST /api/machines/1/events/ with valid data → 201 Created (1min)
- [ ] GET /api/machines/1/events/ → 200 OK (30s)
- [ ] Edge ChangeDetection 초기화 성공 (30s)
- [ ] Android Event List 화면 로딩 (1min)

**Total**: 4 scenarios

### P0 Tests (<15 min)

**Purpose**: Critical path validation

#### Backend Event API
- [ ] BE-2-01: Event 생성 (start, 이미지 포함) → 201
- [ ] BE-2-02: Event 생성 (end, 이미지 포함) → 201
- [ ] BE-2-03: Event 생성 (이미지 없음) → 400
- [ ] BE-2-04: Event 생성 (잘못된 machine_id) → 404
- [ ] BE-2-05: Event 생성 (미인증) → 401
- [ ] BE-2-06: Event 생성 (잘못된 event_type) → 400

#### Backend Event List API
- [ ] BE-2-07: Event 목록 조회 (전체)
- [ ] BE-2-08: Event 목록 조회 (event_type=start)
- [ ] BE-2-09: Event 목록 조회 (event_type=end)
- [ ] BE-2-10: Event 목록 조회 (date_from/to)
- [ ] BE-2-11: Event 목록 페이지네이션

#### Edge Change Detection
- [ ] ED-2-01: 0→1 감지 (start 이벤트)
- [ ] ED-2-02: 1→0 감지 (end 이벤트)
- [ ] ED-2-03: 1→2 (변화 없음, API 호출 X)
- [ ] ED-2-04: 0→0 (변화 없음, API 호출 X)
- [ ] ED-2-05: 상태 유지 검증

#### Edge → Server 통합
- [ ] ED-2-06: 이벤트 전송 성공
- [ ] ED-2-07: 서버 다운 시 로컬 저장
- [ ] **NEW** ED-2-20: 401 재인증 후 pending 이벤트 전송
- [ ] **NEW** ED-2-21: 401 재인증 실패 시 에러 처리
- [ ] **NEW** ED-2-22: 서버 복구 감지 후 큐 재전송
- [ ] **NEW** ED-2-23: 큐 재전송 순서 보장 (FIFO)

**Total**: 22 scenarios

### P1 Tests (<30 min)

**Purpose**: Important feature coverage

#### Android UI
- [ ] AN-2-01: Event List 로딩 및 표시
- [ ] AN-2-02: Event 타입 필터 (전체)
- [ ] AN-2-03: Event 타입 필터 (시작)
- [ ] AN-2-04: Event 타입 필터 (종료)
- [ ] AN-2-05: Pull to Refresh
- [ ] AN-2-06: Event 카드 탭 → Detail 이동
- [ ] AN-2-07: Event Detail 이미지 로딩
- [ ] AN-2-08: Event Detail 메타데이터 표시
- [ ] AN-2-09: 뒤로가기 동작

#### Backend Detail API
- [ ] BE-2-12: Event 상세 조회 → 200
- [ ] BE-2-13: 존재하지 않는 Event → 404
- [ ] BE-2-14: 미인증 상세 조회 → 401

#### Edge Data Format
- [ ] ED-2-08: detections JSON 형식 검증
- [ ] ED-2-09: change_info JSON 형식 검증
- [ ] ED-2-10: captured_at ISO 형식 검증
- [ ] ED-2-11: 이미지 파일 형식 검증

#### NEW - Android 추가 테스트
- [ ] **NEW** AN-2-16: 날짜 범위 필터 DateRangePicker 표시
- [ ] **NEW** AN-2-17: 날짜 범위 선택 후 필터 적용
- [ ] **NEW** AN-2-18: 잘못된 날짜 범위 에러 처리
- [ ] **NEW** AN-2-19: 페이지네이션 "더 보기" 버튼 탭
- [ ] **NEW** AN-2-20: 추가 데이터 로딩 및 표시
- [ ] **NEW** AN-2-21: Event Detail 변화 정보 섹션 표시
- [ ] **NEW** AN-2-22: 이전/현재 인원 값 정확성

**Total**: 23 scenarios

### P2/P3 Tests (<60 min)

**Purpose**: Full regression coverage

- [ ] ED-2-12: YOLOv5 모델 로딩 (Unit)
- [ ] ED-2-13: 모델 로딩 실패 핸들링 (Unit)
- [ ] ED-2-14: person 외 클래스 무시 (Integration)
- [ ] ED-2-15: 다중 person 감지 (Integration)
- [ ] ED-2-16: 이미지 640x480 리사이즈 (Unit)
- [ ] ED-2-17: 리사이즈 품질 검증 (Unit)
- [ ] AN-2-10: EventAdapter 바인딩 (Component)
- [ ] AN-2-11: START 칩 녹색 표시 (Component)
- [ ] AN-2-12: END 칩 빨간색 표시 (Component)
- [ ] BE-2-15: 이미지 URL 직접 접근 (API)
- [ ] BE-2-16: 존재하지 않는 이미지 → 404 (API)
- [ ] AN-2-13: 오프라인 에러 표시 (E2E)
- [ ] AN-2-14: 타임아웃 처리 (E2E)
- [ ] AN-2-15: 서버 500 에러 처리 (E2E)
- [ ] **NEW** AN-2-23: 이벤트 시간 표시 형식 검증 (E2E)
- [ ] **NEW** AN-2-24: 화면 회전 시 EventList 상태 유지 (E2E)
- [ ] **NEW** AN-2-25: 화면 회전 시 EventDetail 상태 유지 (E2E)
- [ ] **NEW** ED-2-24: 다중 person 감지 시 count 정확성 (Unit)
- [ ] **NEW** ED-2-25: person 1→2 시 API 호출 안함 검증 (Unit)

**Total**: 19 scenarios

---

## Resource Estimates

### Test Development Effort

| Priority | Count | Hours/Test | Total Hours | Notes |
|----------|-------|------------|-------------|-------|
| P0 | 22 | 2.0 | 44 | Edge 통합, 이미지 업로드 복잡 |
| P1 | 23 | 1.0 | 23 | E2E 및 API 표준 테스트 |
| P2 | 19 | 0.5 | 9.5 | Unit/Component 테스트 |
| **Total** | **64** | **-** | **76.5** | **~10 days** |

### Prerequisites

**Test Data:**
- GymMachine factory (Epic 1에서 생성)
- MachineEvent factory (start/end 타입, 다양한 날짜)
- 테스트 이미지 파일 (640x480 JPEG)

**Tooling:**
- pytest + pytest-django for Backend
- pytest-mock for Edge unit tests
- Android JUnit / Espresso for Android
- Mock server (responses library) for Edge integration

**Environment:**
- Django 개발 서버 + 테스트 DB
- YOLOv5 환경 (Python 3.8+, PyTorch)
- 테스트용 비디오/이미지 샘플
- Android 에뮬레이터 (API 24+)

---

## Quality Gate Criteria

### Pass/Fail Thresholds

- **P0 pass rate**: 100% (no exceptions)
- **P1 pass rate**: ≥95%
- **P2/P3 pass rate**: ≥90%
- **High-risk mitigations**: 100% complete

### Coverage Targets

- **Critical paths (Event 생성/조회)**: ≥85%
- **Change Detection 로직**: 100%
- **API 필터링**: ≥80%
- **Edge cases**: ≥50%

### Non-Negotiable Requirements

- [x] All P0 tests pass
- [x] No high-risk (≥6) items unmitigated
- [x] Event 생성 API 완전 동작
- [x] Change Detection start/end 정확 동작
- [x] 이미지 업로드/조회 동작

---

## Mitigation Plans

### R-201: Edge 토큰 하드코딩 노출 (Score: 6)

**Mitigation Strategy:** 환경변수로 분리
```python
# changedetection.py
import os

SECURITY_KEY = os.environ.get('GYM_SECURITY_KEY')
if not SECURITY_KEY:
    raise ValueError("GYM_SECURITY_KEY environment variable required")
```
```bash
# .env (gitignore에 추가)
GYM_SECURITY_KEY=your-secret-key
GYM_MACHINE_ID=1
```
**Owner:** Edge
**Timeline:** Sprint 2
**Status:** Planned
**Verification:** .git에 키 미포함 확인, 환경변수 없이 실행 시 에러

### R-202: Event 업로드 실패 시 데이터 손실 (Score: 6)

**Mitigation Strategy:** 로컬 큐잉 + 재시도
```python
class ChangeDetection:
    def __init__(self, ...):
        self.pending_queue = []  # 실패한 이벤트 큐

    def _send_event(self, ...):
        try:
            # 전송 시도
            res = requests.post(...)
            if res.status_code not in [200, 201]:
                self._queue_event(event_data)
        except Exception:
            self._queue_event(event_data)

    def retry_pending(self):
        """주기적으로 pending 이벤트 재전송"""
        for event in self.pending_queue[:]:
            if self._send_event(event):
                self.pending_queue.remove(event)
```
**Owner:** Edge
**Timeline:** Sprint 2
**Status:** Planned
**Verification:** 서버 다운 시나리오 테스트, 재연결 후 이벤트 전송 확인

### R-203: 대량 이벤트 조회 시 API 응답 지연 (Score: 6)

**Mitigation Strategy:** 페이지네이션 + 인덱스 최적화
```python
# views.py
from rest_framework.pagination import PageNumberPagination

class EventPagination(PageNumberPagination):
    page_size = 20
    page_size_query_param = 'page_size'
    max_page_size = 100

# models.py (이미 정의됨)
class Meta:
    indexes = [
        models.Index(fields=['machine', '-captured_at']),
        models.Index(fields=['event_type', '-captured_at']),
    ]
```
**Owner:** Backend
**Timeline:** Sprint 2
**Status:** Planned
**Verification:** 1000+ 이벤트 조회 시 응답 시간 < 500ms

### R-204: Change Detection 오탐지 (Score: 6)

**Mitigation Strategy:** Confidence threshold + 디바운싱
```python
class ChangeDetection:
    def __init__(self, ...):
        self.confidence_threshold = 0.5
        self.debounce_seconds = 2.0
        self.last_event_time = None

    def detect_changes(self, ...):
        # Confidence 필터링
        filtered_detections = [
            d for d in detections
            if d['confidence'] >= self.confidence_threshold
        ]

        # 디바운싱
        now = datetime.now()
        if self.last_event_time:
            elapsed = (now - self.last_event_time).total_seconds()
            if elapsed < self.debounce_seconds:
                return None

        # ... 변화 감지 로직 ...
```
**Owner:** Edge
**Timeline:** Sprint 2
**Status:** Planned
**Verification:** 빠른 연속 감지 시 이벤트 1회만 발생

---

## Test Cases Detail

### Backend Event API Tests

#### BE-2-01: Event 생성 (start)
```bash
curl -X POST http://localhost:8000/api/machines/1/events/ \
  -H "Authorization: Token <token>" \
  -F "image=@test_image.jpg" \
  -F "event_type=start" \
  -F "captured_at=2024-01-15T10:30:00" \
  -F "person_count=1" \
  -F 'detections={"person": [{"bbox": [100, 50, 300, 400], "confidence": 0.95}]}' \
  -F 'change_info={"event_type": "start", "prev_count": 0, "curr_count": 1}'

# Expected: 201 Created
# Response: {"id": 1, "machine": 1, "event_type": "start", ...}
```

#### BE-2-08: Event 목록 필터 (start만)
```bash
curl -H "Authorization: Token <token>" \
  "http://localhost:8000/api/machines/1/events/?event_type=start"

# Expected: 200 OK
# Response: [{"event_type": "start", ...}, ...]
# All items should have event_type="start"
```

### Edge Change Detection Tests

#### ED-2-01: 0→1 감지 (start)
```python
def test_detect_start_event():
    cd = ChangeDetection(names=['person', 'car'], config={...})
    cd.result_prev = [0, 0]  # 이전: person 0명

    result = cd.detect_changes(
        names=['person', 'car'],
        detected_current=[1, 0],  # 현재: person 1명
        image=mock_image,
        detections_raw=[...]
    )

    assert result is not None
    assert result['event_type'] == 'start'
    assert result['prev_count'] == 0
    assert result['curr_count'] == 1
```

#### ED-2-02: 1→0 감지 (end)
```python
def test_detect_end_event():
    cd = ChangeDetection(names=['person', 'car'], config={...})
    cd.result_prev = [1, 0]  # 이전: person 1명

    result = cd.detect_changes(
        names=['person', 'car'],
        detected_current=[0, 0],  # 현재: person 0명
        image=mock_image,
        detections_raw=[...]
    )

    assert result is not None
    assert result['event_type'] == 'end'
    assert result['prev_count'] == 1
    assert result['curr_count'] == 0
```

### Android UI Tests

#### AN-2-01: Event List 로딩
```java
@Test
public void testEventListLoading() {
    // Given: MachineListActivity에서 기구 선택
    Intent intent = new Intent(context, EventListActivity.class);
    intent.putExtra("machine_id", 1);
    intent.putExtra("machine_name", "런닝머신 #1");

    ActivityScenario.launch(intent);

    // Then: RecyclerView에 이벤트 표시
    onView(withId(R.id.recyclerView))
        .check(matches(isDisplayed()));

    onView(withId(R.id.recyclerView))
        .check(matches(hasMinimumChildCount(1)));
}
```

#### AN-2-03: Event 타입 필터 (시작)
```java
@Test
public void testEventTypeFilterStart() {
    // Given: EventListActivity 표시됨
    // When: "시작" 칩 선택
    onView(withId(R.id.chipStart)).perform(click());

    // Then: start 이벤트만 표시
    onView(withId(R.id.recyclerView))
        .check(matches(allOf(
            hasDescendant(withText("사용 시작")),
            not(hasDescendant(withText("사용 종료")))
        )));
}
```

### NEW - 추가된 테스트 케이스

#### ED-2-20: 401 재인증 후 pending 이벤트 전송
```python
def test_401_reauth_and_resend_pending():
    cd = ChangeDetection(names=['person'], config={...})

    # Given: 토큰 만료 상태로 이벤트 발생
    cd.token = "expired_token"
    mock_server.enqueue_response(401)  # 첫 요청 실패

    # When: 이벤트 감지 및 전송 시도
    cd.detect_changes(detected_current=[1], ...)

    # Then: pending 큐에 저장됨
    assert len(cd.pending_queue) == 1

    # When: 재인증 후 retry
    mock_server.enqueue_response(200, {"token": "new_token"})  # 재인증
    mock_server.enqueue_response(201)  # 이벤트 전송 성공
    cd.retry_pending()

    # Then: pending 큐가 비워짐
    assert len(cd.pending_queue) == 0
```

#### ED-2-22: 서버 복구 감지 후 큐 재전송
```python
def test_server_recovery_queue_resend():
    cd = ChangeDetection(names=['person'], config={...})

    # Given: 서버 다운으로 3개 이벤트 pending
    cd.pending_queue = [event1, event2, event3]

    # When: 서버 복구 후 health check 성공
    mock_server.enqueue_response(200)  # health check
    mock_server.enqueue_response(201)  # event1
    mock_server.enqueue_response(201)  # event2
    mock_server.enqueue_response(201)  # event3

    cd.check_server_and_retry()

    # Then: 모든 pending 이벤트 전송됨 (FIFO 순서)
    assert len(cd.pending_queue) == 0
    assert mock_server.requests[1].body == event1
    assert mock_server.requests[2].body == event2
    assert mock_server.requests[3].body == event3
```

#### AN-2-16: 날짜 범위 필터 DateRangePicker 표시
```java
@Test
public void testDateRangePicker_displays() {
    // Given: EventListActivity 표시됨
    Intent intent = new Intent(context, EventListActivity.class);
    intent.putExtra("machine_id", 1);
    ActivityScenario.launch(intent);

    // When: 날짜 범위 버튼 클릭
    onView(withId(R.id.btnDateRange)).perform(click());

    // Then: DateRangePicker 다이얼로그 표시
    onView(withText("날짜 범위 선택"))
        .check(matches(isDisplayed()));
    onView(withText("적용")).check(matches(isDisplayed()));
    onView(withText("취소")).check(matches(isDisplayed()));
}
```

#### AN-2-17: 날짜 범위 선택 후 필터 적용
```java
@Test
public void testDateRangeFilter_applied() {
    // Given: EventListActivity with DateRangePicker
    ActivityScenario.launch(EventListActivity.class);
    onView(withId(R.id.btnDateRange)).perform(click());

    // When: 날짜 범위 선택 및 적용
    // (DateRangePicker에서 2024-01-10 ~ 2024-01-15 선택)
    selectDateRange("2024-01-10", "2024-01-15");
    onView(withText("적용")).perform(click());

    // Then: 필터된 결과만 표시
    // API가 ?date_from=2024-01-10&date_to=2024-01-15로 호출됨
    verify(mockApi).getEvents(eq(1), eq("2024-01-10"), eq("2024-01-15"), any());
}
```

#### AN-2-19: 페이지네이션 "더 보기" 버튼 탭
```java
@Test
public void testPagination_loadMore() {
    // Given: 20개 이벤트 표시 중 (page 1)
    setupMockEventsWithPagination(totalEvents: 50, pageSize: 20);
    ActivityScenario.launch(EventListActivity.class);

    // Then: 초기 20개 표시
    onView(withId(R.id.recyclerView))
        .check(matches(hasChildCount(20)));

    // When: "더 보기" 버튼 클릭
    onView(withId(R.id.btnLoadMore)).perform(click());

    // Then: 40개로 증가
    onView(withId(R.id.recyclerView))
        .check(matches(hasChildCount(40)));
}
```

#### AN-2-21: Event Detail 변화 정보 섹션 표시
```java
@Test
public void testEventDetail_changeInfoSection() {
    // Given: start 이벤트 상세 화면
    Intent intent = new Intent(context, EventDetailActivity.class);
    intent.putExtra("event_id", 1);
    ActivityScenario.launch(intent);

    // Then: 변화 정보 섹션 표시
    onView(withId(R.id.sectionChangeInfo))
        .check(matches(isDisplayed()));

    onView(withText("이전 인원"))
        .check(matches(isDisplayed()));
    onView(withId(R.id.tvPrevCount))
        .check(matches(withText("0명")));

    onView(withText("현재 인원"))
        .check(matches(isDisplayed()));
    onView(withId(R.id.tvCurrCount))
        .check(matches(withText("1명")));

    onView(withText("대상 클래스"))
        .check(matches(isDisplayed()));
    onView(withId(R.id.tvTargetClass))
        .check(matches(withText("person")));
}
```

#### AN-2-23: 이벤트 시간 표시 형식 검증
```java
@Test
public void testEventCard_timeFormat() {
    // Given: 이벤트 목록 표시
    ActivityScenario.launch(EventListActivity.class);

    // Then: 시간이 "YYYY-MM-DD HH:mm:ss" 형식으로 표시
    onView(withId(R.id.recyclerView))
        .check(matches(hasDescendant(
            withText(matchesPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))
        )));
}
```

#### AN-2-24: 화면 회전 시 EventList 상태 유지
```java
@Test
public void testRotation_eventListStatePreserved() {
    // Given: EventList에 필터 적용된 상태
    ActivityScenario<EventListActivity> scenario =
        ActivityScenario.launch(EventListActivity.class);
    onView(withId(R.id.chipStart)).perform(click());  // 시작 필터

    // When: 화면 회전
    scenario.onActivity(activity -> {
        activity.setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    });

    // Then: 필터 상태 및 데이터 유지
    onView(withId(R.id.chipStart))
        .check(matches(isChecked()));
    onView(withId(R.id.recyclerView))
        .check(matches(hasMinimumChildCount(1)));
}
```

#### ED-2-24: 다중 person 감지 시 count 정확성
```python
def test_multiple_person_count_accuracy():
    cd = ChangeDetection(names=['person'], config={...})

    # Given: 이전 상태 person 1명
    cd.result_prev = [1]

    # When: 현재 person 3명 감지
    result = cd.detect_changes(
        detected_current=[3],
        image=mock_image,
        detections_raw=[...]
    )

    # Then: 변화 없음 (1→3은 이벤트 아님), count는 정확히 3
    assert result is None  # API 호출 안함
    assert cd.result_prev == [3]  # 상태 업데이트됨
```

---

## User Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Epic 2 User Flow                                │
└─────────────────────────────────────────────────────────────────────────────┘

[Edge System]                    [Server]                     [Android App]
     │                               │                              │
     │  YOLOv5 객체 감지             │                              │
     │  ─────────────────            │                              │
     │         │                     │                              │
     │  person 0→1 감지              │                              │
     │  (start 이벤트)               │                              │
     │         │                     │                              │
     │         │  POST /api/machines/1/events/                      │
     │         │  {image, event_type: "start", ...}                 │
     │         │────────────────────>│                              │
     │         │                     │  201 Created                 │
     │         │<────────────────────│                              │
     │         │                     │                              │
     │  person 1→0 감지              │                              │
     │  (end 이벤트)                 │                              │
     │         │                     │                              │
     │         │  POST /api/machines/1/events/                      │
     │         │  {image, event_type: "end", ...}                   │
     │         │────────────────────>│                              │
     │         │                     │  201 Created                 │
     │         │<────────────────────│                              │
     │                               │                              │
     │                               │      GET /api/machines/1/events/
     │                               │<─────────────────────────────│
     │                               │  [{events}]                  │
     │                               │─────────────────────────────>│
     │                               │                              │
     │                               │                    ┌─────────────────┐
     │                               │                    │  Event List     │
     │                               │                    │  ┌───────────┐  │
     │                               │                    │  │ START 10:30│ │
     │                               │                    │  │ END   10:45│  │
     │                               │                    │  │ START 11:00│ │
     │                               │                    │  └───────────┘  │
     │                               │                    └─────────────────┘
     │                               │                              │
     │                               │      GET /api_root/events/1/ │
     │                               │<─────────────────────────────│
     │                               │  {event detail}              │
     │                               │─────────────────────────────>│
     │                               │                              │
     │                               │                    ┌─────────────────┐
     │                               │                    │  Event Detail   │
     │                               │                    │  [Full Image]   │
     │                               │                    │  Type: START    │
     │                               │                    │  Time: 10:30    │
     │                               │                    │  Person: 1      │
     │                               │                    └─────────────────┘
```

---

## Approval

**Test Design Approved By:**

- [ ] Product Manager: _______ Date: _______
- [ ] Tech Lead: _______ Date: _______
- [ ] QA Lead: _______ Date: _______

---

## Appendix

### Related Documents

- PRD: `docs/final/gym-service-overview.md`
- Epic: `_bmad-output/epics.md` (Epic 2)
- Backend Spec: `docs/final/implementation/02-backend.md`
- Edge Spec: `docs/final/implementation/03-edge-system.md`
- Android Spec: `docs/final/implementation/04-android.md`
- API Reference: `docs/final/implementation/05-api-reference.md`
- Epic 1 Test Design: `_bmad-output/test-design-epic-1.md`

---

**Generated by**: BMad TEA Agent - Test Architect Module
**Workflow**: `_bmad/bmm/testarch/test-design`
**Version**: 4.0 (BMad v6)
