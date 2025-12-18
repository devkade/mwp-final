# Test Design: Epic 3 - Usage Analytics & Statistics

**Date:** 2025-12-19
**Author:** Kade
**Status:** Draft

---

## Executive Summary

**Scope:** Full test design for Epic 3

**Risk Summary:**
- Total risks identified: 6
- High-priority risks (≥6): 1
- Critical categories: PERF, DATA

**Coverage Summary:**
- P0 scenarios: 8 tests (16 hours)
- P1 scenarios: 16 tests (16 hours)
- P2/P3 scenarios: 12 tests (6 hours)
- **Total effort**: 38 hours (~5 days)

---

## Risk Assessment

### High-Priority Risks (Score ≥6)

| Risk ID | Category | Description | Probability | Impact | Score | Mitigation | Owner | Timeline |
|---------|----------|-------------|-------------|--------|-------|------------|-------|----------|
| R-301 | PERF | 대량 이벤트 집계 시 API 응답 지연 (>2s) | 3 | 2 | 6 | DB 인덱스 최적화, 캐싱, 비동기 집계 | Backend | Sprint 3 |

### Medium-Priority Risks (Score 3-4)

| Risk ID | Category | Description | Probability | Impact | Score | Mitigation | Owner |
|---------|----------|-------------|-------------|--------|-------|------------|-------|
| R-302 | DATA | 일별 사용량 집계 시 타임존 불일치 | 2 | 2 | 4 | Asia/Seoul 타임존 명시적 처리 | Backend |
| R-303 | TECH | 차트 라이브러리 호환성 (Android) | 2 | 2 | 4 | MPAndroidChart 버전 고정 | Android |
| R-304 | BUS | 빈 통계 데이터 시 UI 표시 오류 | 2 | 2 | 4 | Empty State 명시적 처리 | Android |

### Low-Priority Risks (Score 1-2)

| Risk ID | Category | Description | Probability | Impact | Score | Action |
|---------|----------|-------------|-------------|--------|-------|--------|
| R-305 | BUS | 날짜 범위 선택 UI 혼란 | 1 | 2 | 2 | DateRangePicker 사용성 테스트 |
| R-306 | OPS | 통계 API 캐시 무효화 타이밍 | 1 | 1 | 1 | 캐시 TTL 설정 (5분) |

---

## Test Coverage Plan

### P0 (Critical) - Run on every commit

**Criteria**: Blocks core journey + High risk (≥6) + No workaround

| Requirement | Test Level | Risk Link | Test Count | Owner | Notes |
|-------------|------------|-----------|------------|-------|-------|
| FR13: Statistics API 기본 | API | R-301 | 4 | QA | 총 start/end, 일별 사용량 |
| FR13: Statistics API 필터링 | API | R-302 | 3 | QA | date_from/to 필터 |
| FR13: Statistics API 인증 | API | - | 1 | QA | 미인증 → 401 |

**Total P0**: 8 tests, 16 hours

### P1 (High) - Run on PR to main

**Criteria**: Important features + Medium risk (3-4) + Common workflows

| Requirement | Test Level | Risk Link | Test Count | Owner | Notes |
|-------------|------------|-----------|------------|-------|-------|
| FR20: Android Statistics 화면 | E2E | R-303, R-304 | 5 | QA | 메트릭 카드, 차트 표시 |
| FR20: Android 날짜 범위 선택 | E2E | R-305 | 3 | QA | DateRangePicker 동작 |
| FR20: Android Empty State | E2E | R-304 | 2 | QA | 데이터 없는 경우 |
| **NEW** 동작 선택 메뉴 UI | E2E | - | 2 | QA | 이벤트 목록 / 사용 통계 메뉴 표시 |
| **NEW** DateRangePicker 취소 동작 | E2E | R-305 | 1 | QA | 취소 시 기존 범위 유지 |
| **NEW** 새 이벤트 추가 후 통계 갱신 | Integration | R-306 | 2 | QA | 캐시 무효화 + 실시간 반영 |
| **NEW** 기구 이름 타이틀 표시 | E2E | - | 1 | QA | "런닝머신 #1 통계" 타이틀 |

**Total P1**: 16 tests, 16 hours

### P2 (Medium) - Run nightly/weekly

**Criteria**: Secondary features + Low risk (1-2) + Edge cases

| Requirement | Test Level | Test Count | Owner | Notes |
|-------------|------------|------------|-------|-------|
| Statistics 차트 렌더링 | Component | 3 | DEV | 막대 그래프, 선 그래프 |
| Statistics API 성능 | Performance | 2 | QA | 1000+ 이벤트 집계 시간 |
| 타임존 처리 | Unit | 2 | DEV | Asia/Seoul 변환 |
| 캐싱 동작 | Integration | 1 | QA | 캐시 hit/miss |
| **NEW** 증감 표시 "▲ +12" | E2E | 1 | QA | 주간 변화량 표시 검증 |
| **NEW** 차트 X축 날짜 라벨 형식 | E2E | 1 | QA | "01, 03, 05..." 형식 검증 |
| **NEW** 화면 회전 상태 유지 | E2E | 2 | QA | Statistics 화면 회전 시 데이터 유지 |

**Total P2**: 12 tests, 6 hours

---

## Execution Order

### Smoke Tests (<3 min)

**Purpose**: Fast feedback, catch build-breaking issues

- [ ] GET /api_root/machines/1/stats/ → 200 OK (30s)
- [ ] Android Statistics 화면 로딩 (1min)
- [ ] 메트릭 카드 표시 확인 (30s)

**Total**: 3 scenarios

### P0 Tests (<10 min)

**Purpose**: Critical path validation

#### Backend Statistics API
- [ ] BE-3-01: 기구 통계 조회 → 200 OK
- [ ] BE-3-02: total_starts 값 검증
- [ ] BE-3-03: total_ends 값 검증
- [ ] BE-3-04: daily_usage 배열 검증
- [ ] BE-3-05: date_from 필터 적용
- [ ] BE-3-06: date_to 필터 적용
- [ ] BE-3-07: date_from + date_to 조합
- [ ] BE-3-08: 미인증 요청 → 401

**Total**: 8 scenarios

### P1 Tests (<20 min)

**Purpose**: Important feature coverage

#### Android UI
- [ ] AN-3-01: Statistics 화면 진입
- [ ] AN-3-02: 총 사용 시작 횟수 카드 표시
- [ ] AN-3-03: 총 사용 종료 횟수 카드 표시
- [ ] AN-3-04: 일별 사용량 차트 표시
- [ ] AN-3-05: 차트 데이터 포인트 정확성
- [ ] AN-3-06: 날짜 범위 선택 다이얼로그
- [ ] AN-3-07: 날짜 범위 적용 후 데이터 갱신
- [ ] AN-3-08: 잘못된 날짜 범위 처리 (from > to)
- [ ] AN-3-09: 데이터 없는 기간 Empty State
- [ ] AN-3-10: 로딩 중 ProgressBar 표시
- [ ] **NEW** AN-3-11: 동작 선택 메뉴 (이벤트/통계) 표시
- [ ] **NEW** AN-3-12: 동작 선택 메뉴에서 통계 선택
- [ ] **NEW** AN-3-13: DateRangePicker 취소 시 기존 범위 유지
- [ ] **NEW** AN-3-14: 새 이벤트 추가 후 통계 자동 갱신
- [ ] **NEW** AN-3-15: 통계 API 캐시 무효화 검증
- [ ] **NEW** AN-3-16: 기구 이름 타이틀 "런닝머신 #1 통계" 표시

**Total**: 16 scenarios

### P2/P3 Tests (<30 min)

**Purpose**: Full regression coverage

- [ ] AN-3-11: 막대 그래프 렌더링 (Component)
- [ ] AN-3-12: 차트 애니메이션 (Component)
- [ ] AN-3-13: 차트 터치 인터랙션 (Component)
- [ ] BE-3-09: 1000+ 이벤트 집계 성능 (<500ms)
- [ ] BE-3-10: 10000+ 이벤트 집계 성능 (<2s)
- [ ] BE-3-11: Asia/Seoul 타임존 변환 (Unit)
- [ ] BE-3-12: UTC → KST 날짜 경계 처리 (Unit)
- [ ] BE-3-13: 캐시 동작 검증 (Integration)
- [ ] **NEW** AN-3-17: 증감 표시 "▲ +12 (이번 주)" 검증 (E2E)
- [ ] **NEW** AN-3-18: 차트 X축 날짜 라벨 형식 검증 (E2E)
- [ ] **NEW** AN-3-19: 화면 회전 시 Statistics 데이터 유지 (E2E)
- [ ] **NEW** AN-3-20: 화면 회전 시 차트 다시 렌더링 (E2E)

**Total**: 12 scenarios

---

## Resource Estimates

### Test Development Effort

| Priority | Count | Hours/Test | Total Hours | Notes |
|----------|-------|------------|-------------|-------|
| P0 | 8 | 2.0 | 16 | 집계 로직 검증 복잡 |
| P1 | 16 | 1.0 | 16 | E2E 표준 테스트 |
| P2 | 12 | 0.5 | 6 | 성능/Component 테스트 |
| **Total** | **36** | **-** | **38** | **~5 days** |

### Prerequisites

**Test Data:**
- GymMachine factory (Epic 1)
- MachineEvent factory with various dates (Epic 2)
- 최소 100개 이상의 테스트 이벤트 데이터

**Tooling:**
- pytest + pytest-django for Backend
- pytest-benchmark for 성능 테스트
- Android JUnit / Espresso for Android
- MPAndroidChart for 차트 테스트

**Environment:**
- Django 개발 서버 + 테스트 DB
- Android 에뮬레이터 (API 24+)
- 성능 테스트용 대량 데이터 세트

---

## Quality Gate Criteria

### Pass/Fail Thresholds

- **P0 pass rate**: 100% (no exceptions)
- **P1 pass rate**: ≥95%
- **P2/P3 pass rate**: ≥90%
- **High-risk mitigations**: 100% complete

### Coverage Targets

- **Statistics API**: 100%
- **집계 로직**: ≥90%
- **UI 표시**: ≥80%
- **성능 요구사항**: 100%

### Performance Requirements

- **Statistics API 응답 시간**: < 500ms (1000 이벤트)
- **Statistics API 응답 시간**: < 2s (10000 이벤트)
- **차트 렌더링 시간**: < 1s

---

## Mitigation Plans

### R-301: 대량 이벤트 집계 시 API 응답 지연 (Score: 6)

**Mitigation Strategy:** DB 인덱스 + 쿼리 최적화 + 캐싱

```python
# models.py - 인덱스 추가
class MachineEvent(models.Model):
    class Meta:
        indexes = [
            models.Index(fields=['machine', '-captured_at']),
            models.Index(fields=['event_type', '-captured_at']),
            # 통계 쿼리 최적화용 복합 인덱스
            models.Index(fields=['machine', 'event_type', 'captured_at']),
        ]

# views.py - 캐싱 적용
from django.core.cache import cache

class GymMachineViewSet(viewsets.ModelViewSet):
    @action(detail=True, methods=['get'])
    def stats(self, request, pk=None):
        cache_key = f"machine_stats_{pk}_{request.query_params}"
        cached = cache.get(cache_key)
        if cached:
            return Response(cached)

        # 집계 로직...
        result = {...}

        cache.set(cache_key, result, timeout=300)  # 5분 캐시
        return Response(result)
```

**Owner:** Backend
**Timeline:** Sprint 3
**Status:** Planned
**Verification:** 10000 이벤트 집계 시 응답 시간 < 2s

### R-302: 일별 사용량 집계 시 타임존 불일치 (Score: 4)

**Mitigation Strategy:** Asia/Seoul 타임존 명시적 처리

```python
# views.py
from django.utils import timezone
from django.db.models.functions import TruncDate
import pytz

KST = pytz.timezone('Asia/Seoul')

@action(detail=True, methods=['get'])
def stats(self, request, pk=None):
    # 타임존 인식 날짜 변환
    events = machine.events.annotate(
        date_kst=TruncDate('captured_at', tzinfo=KST)
    )

    daily_stats = events.filter(event_type='start').values(
        'date_kst'
    ).annotate(count=Count('id')).order_by('date_kst')

    return Response({
        'daily_usage': [
            {'date': stat['date_kst'].isoformat(), 'count': stat['count']}
            for stat in daily_stats
        ]
    })
```

**Owner:** Backend
**Timeline:** Sprint 3
**Status:** Planned
**Verification:** UTC 자정 전후 이벤트의 날짜 분류 정확성 검증

---

## Test Cases Detail

### Backend Statistics API Tests

#### BE-3-01: 기구 통계 조회
```bash
curl -H "Authorization: Token <token>" \
  "http://localhost:8000/api_root/machines/1/stats/"

# Expected: 200 OK
# Response:
{
  "machine_id": 1,
  "machine_name": "런닝머신 #1",
  "total_starts": 152,
  "total_ends": 150,
  "daily_usage": [
    {"date": "2024-01-13", "count": 12},
    {"date": "2024-01-14", "count": 18},
    {"date": "2024-01-15", "count": 15}
  ]
}
```

#### BE-3-05: date_from 필터 적용
```bash
curl -H "Authorization: Token <token>" \
  "http://localhost:8000/api_root/machines/1/stats/?date_from=2024-01-14"

# Expected: 200 OK
# daily_usage should only include dates >= 2024-01-14
```

#### BE-3-07: date_from + date_to 조합
```bash
curl -H "Authorization: Token <token>" \
  "http://localhost:8000/api_root/machines/1/stats/?date_from=2024-01-10&date_to=2024-01-15"

# Expected: 200 OK
# daily_usage should only include dates between 2024-01-10 and 2024-01-15
```

### Android UI Tests

#### AN-3-01: Statistics 화면 진입
```java
@Test
public void testStatsScreenLoading() {
    // Given: MachineListActivity에서 통계 메뉴 선택
    Intent intent = new Intent(context, StatsActivity.class);
    intent.putExtra("machine_id", 1);
    intent.putExtra("machine_name", "런닝머신 #1");

    ActivityScenario.launch(intent);

    // Then: Statistics 화면 표시
    onView(withId(R.id.tvMachineName))
        .check(matches(withText("런닝머신 #1")));

    onView(withId(R.id.cardTotalStarts))
        .check(matches(isDisplayed()));

    onView(withId(R.id.cardTotalEnds))
        .check(matches(isDisplayed()));
}
```

#### AN-3-04: 일별 사용량 차트 표시
```java
@Test
public void testDailyUsageChart() {
    // Given: Statistics 화면 로딩됨
    ActivityScenario.launch(StatsActivity.class);

    // Then: 차트가 표시되고 데이터 포인트가 있음
    onView(withId(R.id.chartDailyUsage))
        .check(matches(isDisplayed()));

    // 차트에 최소 1개 이상의 데이터 포인트
    onView(withId(R.id.chartDailyUsage))
        .check(matches(hasMinimumChildCount(1)));
}
```

#### AN-3-09: 데이터 없는 기간 Empty State
```java
@Test
public void testEmptyStateForNoData() {
    // Given: 이벤트가 없는 날짜 범위 선택
    // When: 2099년 날짜로 필터 적용
    onView(withId(R.id.btnDateRange)).perform(click());
    // DateRangePicker에서 미래 날짜 선택...

    // Then: Empty State 표시
    onView(withId(R.id.layoutEmptyState))
        .check(matches(isDisplayed()));

    onView(withId(R.id.tvEmptyMessage))
        .check(matches(withText("해당 기간에 데이터가 없습니다")));
}
```

### NEW - 추가된 테스트 케이스

#### AN-3-11: 동작 선택 메뉴 (이벤트/통계) 표시
```java
@Test
public void testActionMenu_displayed() {
    // Given: MachineListActivity에서 기구 선택
    ActivityScenario.launch(MachineListActivity.class);

    // When: 기구 카드 탭
    onView(withId(R.id.recyclerView))
        .perform(actionOnItemAtPosition(0, click()));

    // Then: 동작 선택 메뉴 표시
    onView(withText("이벤트 목록"))
        .check(matches(isDisplayed()));
    onView(withText("사용 통계"))
        .check(matches(isDisplayed()));
}
```

#### AN-3-12: 동작 선택 메뉴에서 통계 선택
```java
@Test
public void testActionMenu_selectStats() {
    // Given: 동작 선택 메뉴 표시 중
    ActivityScenario.launch(MachineListActivity.class);
    onView(withId(R.id.recyclerView))
        .perform(actionOnItemAtPosition(0, click()));

    // When: "사용 통계" 선택
    onView(withText("사용 통계")).perform(click());

    // Then: StatsActivity로 이동
    intended(hasComponent(StatsActivity.class.getName()));
}
```

#### AN-3-13: DateRangePicker 취소 시 기존 범위 유지
```java
@Test
public void testDateRangePicker_cancel() {
    // Given: Statistics 화면, 날짜 범위 2024-01-01 ~ 2024-01-15
    ActivityScenario.launch(StatsActivity.class);
    String originalRange = getDisplayedDateRange();  // "2024-01-01 ~ 01-15"

    // When: DateRangePicker 열고 새 날짜 선택 후 취소
    onView(withId(R.id.btnDateRange)).perform(click());
    selectDateRange("2024-02-01", "2024-02-15");
    onView(withText("취소")).perform(click());

    // Then: 기존 날짜 범위 유지
    onView(withId(R.id.tvDateRange))
        .check(matches(withText(originalRange)));
}
```

#### AN-3-14: 새 이벤트 추가 후 통계 자동 갱신
```java
@Test
public void testStats_autoRefreshAfterNewEvent() {
    // Given: Statistics 화면, total_starts = 152
    ActivityScenario.launch(StatsActivity.class);
    onView(withId(R.id.tvTotalStarts))
        .check(matches(withText("152회")));

    // When: 서버에 새 이벤트 추가 (백그라운드에서)
    addNewEventToServer(machineId, "start");

    // 잠시 대기 또는 Pull to Refresh
    onView(withId(R.id.swipeRefreshLayout)).perform(swipeDown());

    // Then: 통계 갱신됨
    onView(withId(R.id.tvTotalStarts))
        .check(matches(withText("153회")));
}
```

#### AN-3-15: 통계 API 캐시 무효화 검증
```java
@Test
public void testStats_cacheInvalidation() {
    // Given: Statistics 화면 로드 (캐시됨)
    ActivityScenario.launch(StatsActivity.class);
    int initialApiCalls = mockServer.getRequestCount();

    // When: 같은 데이터 다시 요청 (캐시 유효 기간 내)
    onView(withId(R.id.swipeRefreshLayout)).perform(swipeDown());

    // Then: 강제 새로고침으로 API 호출됨
    int afterRefreshCalls = mockServer.getRequestCount();
    assertTrue(afterRefreshCalls > initialApiCalls);
}
```

#### AN-3-16: 기구 이름 타이틀 표시
```java
@Test
public void testStats_machineNameTitle() {
    // Given: 런닝머신 #1의 통계 화면
    Intent intent = new Intent(context, StatsActivity.class);
    intent.putExtra("machine_id", 1);
    intent.putExtra("machine_name", "런닝머신 #1");
    ActivityScenario.launch(intent);

    // Then: 타이틀에 기구 이름 표시
    onView(withId(R.id.toolbar))
        .check(matches(hasDescendant(withText("런닝머신 #1 통계"))));
}
```

#### AN-3-17: 증감 표시 검증
```java
@Test
public void testStats_weeklyChangeDisplay() {
    // Given: 이번 주 +12회 증가 데이터
    setupMockStatsWithWeeklyChange(totalStarts: 152, weeklyChange: 12);
    ActivityScenario.launch(StatsActivity.class);

    // Then: 증감 표시
    onView(withId(R.id.tvStartsChange))
        .check(matches(withText("▲ +12 (이번 주)")));
}
```

#### AN-3-18: 차트 X축 날짜 라벨 형식 검증
```java
@Test
public void testChart_xAxisDateFormat() {
    // Given: Statistics 화면 로드
    ActivityScenario<StatsActivity> scenario =
        ActivityScenario.launch(StatsActivity.class);

    // Then: X축 라벨이 "01", "03", "05" 형식
    scenario.onActivity(activity -> {
        BarChart chart = activity.findViewById(R.id.chartDailyUsage);
        List<String> labels = chart.getXAxis().getLabels();

        // 날짜 형식 검증 (2자리 숫자)
        for (String label : labels) {
            assertTrue(label.matches("\\d{2}"));
        }
    });
}
```

#### AN-3-19: 화면 회전 시 Statistics 데이터 유지
```java
@Test
public void testRotation_statsDataPreserved() {
    // Given: Statistics 화면에 데이터 표시 중
    ActivityScenario<StatsActivity> scenario =
        ActivityScenario.launch(StatsActivity.class);

    // 초기 값 기록
    String initialStarts = getTextFromView(R.id.tvTotalStarts);

    // When: 화면 회전
    scenario.onActivity(activity -> {
        activity.setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    });

    // Then: 데이터 유지 (API 재호출 없이)
    onView(withId(R.id.tvTotalStarts))
        .check(matches(withText(initialStarts)));
}
```

### Performance Tests

#### BE-3-09: 1000+ 이벤트 집계 성능
```python
@pytest.mark.benchmark
def test_stats_performance_1000_events(benchmark, client, auth_token):
    # Setup: 1000개 이벤트 생성
    machine = GymMachineFactory()
    MachineEventFactory.create_batch(1000, machine=machine)

    def fetch_stats():
        return client.get(
            f'/api_root/machines/{machine.id}/stats/',
            HTTP_AUTHORIZATION=f'Token {auth_token}'
        )

    result = benchmark(fetch_stats)

    assert result.status_code == 200
    # 평균 응답 시간 < 500ms
    assert benchmark.stats['mean'] < 0.5
```

---

## User Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Epic 3 User Flow                                │
└─────────────────────────────────────────────────────────────────────────────┘

[Android App]                                           [Server]
     │                                                      │
     │  Equipment List 화면                                 │
     │  ─────────────────────                               │
     │         │                                            │
     │  기구 선택 → 통계 버튼 탭                            │
     │         │                                            │
     │         ▼                                            │
     │  ┌─────────────────────────────────────────┐         │
     │  │         Statistics 화면                 │         │
     │  │  ┌─────────────┬─────────────┐         │         │
     │  │  │ 총 시작    │ 총 종료    │         │         │
     │  │  │   152회    │   150회    │         │         │
     │  │  └─────────────┴─────────────┘         │         │
     │  │                                         │         │
     │  │  [날짜 범위 선택: 2024-01-01 ~ 01-15]  │         │
     │  │                                         │         │
     │  │  ┌─────────────────────────────┐       │         │
     │  │  │     일별 사용량 차트        │       │         │
     │  │  │  ▓▓▓                        │       │         │
     │  │  │  ▓▓▓ ▓▓▓                    │       │         │
     │  │  │  ▓▓▓ ▓▓▓ ▓▓▓ ▓▓▓           │       │         │
     │  │  │  ─────────────────          │       │         │
     │  │  │  13  14  15  16             │       │         │
     │  │  └─────────────────────────────┘       │         │
     │  └─────────────────────────────────────────┘         │
     │         │                                            │
     │         │  GET /api_root/machines/1/stats/           │
     │         │  ?date_from=2024-01-01&date_to=2024-01-15  │
     │         │───────────────────────────────────────────>│
     │         │                                            │
     │         │  {                                         │
     │         │    "machine_id": 1,                        │
     │         │    "total_starts": 152,                    │
     │         │    "total_ends": 150,                      │
     │         │    "daily_usage": [...]                    │
     │         │  }                                         │
     │         │<───────────────────────────────────────────│
     │         │                                            │
     │  차트 업데이트                                        │
     │         │                                            │
```

---

## API Response Schema

### Statistics API Response

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["machine_id", "machine_name", "total_starts", "total_ends", "daily_usage"],
  "properties": {
    "machine_id": {
      "type": "integer",
      "description": "기구 ID"
    },
    "machine_name": {
      "type": "string",
      "description": "기구 이름"
    },
    "total_starts": {
      "type": "integer",
      "minimum": 0,
      "description": "총 사용 시작 횟수"
    },
    "total_ends": {
      "type": "integer",
      "minimum": 0,
      "description": "총 사용 종료 횟수"
    },
    "daily_usage": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["date", "count"],
        "properties": {
          "date": {
            "type": "string",
            "format": "date",
            "description": "날짜 (YYYY-MM-DD)"
          },
          "count": {
            "type": "integer",
            "minimum": 0,
            "description": "해당 날짜 사용 횟수"
          }
        }
      }
    }
  }
}
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
- Epic: `_bmad-output/epics.md` (Epic 3)
- Backend Spec: `docs/final/implementation/02-backend.md`
- Android Spec: `docs/final/implementation/04-android.md`
- API Reference: `docs/final/implementation/05-api-reference.md`
- Epic 1 Test Design: `_bmad-output/test-design-epic-1.md`
- Epic 2 Test Design: `_bmad-output/test-design-epic-2.md`

### Dependencies on Previous Epics

| Dependency | From Epic | Required For |
|------------|-----------|--------------|
| ApiUser 모델 | Epic 1 | 인증 |
| GymMachine 모델 | Epic 1 | 기구 조회 |
| MachineEvent 모델 | Epic 2 | 통계 집계 데이터 |
| Token 인증 | Epic 1 | API 인증 |

---

**Generated by**: BMad TEA Agent - Test Architect Module
**Workflow**: `_bmad/bmm/testarch/test-design`
**Version**: 4.0 (BMad v6)
