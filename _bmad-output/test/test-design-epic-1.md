# Test Design: Epic 1 - Equipment Discovery & System Access

**Date:** 2025-12-19
**Author:** Kade
**Status:** Draft

---

## Executive Summary

**Scope:** Full test design for Epic 1

**Risk Summary:**
- Total risks identified: 7
- High-priority risks (≥6): 2
- Critical categories: SEC (Security)

**Coverage Summary:**
- P0 scenarios: 11 tests (22 hours)
- P1 scenarios: 17 tests (17 hours)
- P2/P3 scenarios: 15 tests (7.5 hours)
- **Total effort**: 46.5 hours (~6 days)

---

## Risk Assessment

### High-Priority Risks (Score ≥6)

| Risk ID | Category | Description | Probability | Impact | Score | Mitigation | Owner | Timeline |
|---------|----------|-------------|-------------|--------|-------|------------|-------|----------|
| R-001 | SEC | 보안키 brute-force 공격 (rate limiting 미구현) | 2 | 3 | 6 | Django rate limiting 미들웨어 추가 | Backend | Sprint 1 |
| R-002 | SEC | 토큰 로컬 저장 시 평문 노출 위험 | 2 | 3 | 6 | Android EncryptedSharedPreferences 사용 | Android | Sprint 1 |

### Medium-Priority Risks (Score 3-4)

| Risk ID | Category | Description | Probability | Impact | Score | Mitigation | Owner |
|---------|----------|-------------|-------------|--------|-------|------------|-------|
| R-003 | DATA | ApiUser/GymMachine 모델 마이그레이션 실패 | 1 | 3 | 3 | 마이그레이션 전 백업, 테스트 DB 검증 | Backend |
| R-004 | TECH | 기존 Photo Blog API 호환성 깨짐 (/api_root/Post/) | 2 | 2 | 4 | 기존 엔드포인트 회귀 테스트 추가 | Backend |
| R-006 | OPS | Android 네트워크 오류 시 사용자 피드백 부재 | 2 | 2 | 4 | 모든 API 호출에 에러 핸들링 추가 | Android |

### Low-Priority Risks (Score 1-2)

| Risk ID | Category | Description | Probability | Impact | Score | Action |
|---------|----------|-------------|-------------|--------|-------|--------|
| R-005 | BUS | 빈 기구 목록 시 사용자 혼란 | 2 | 1 | 2 | Empty State UI 구현 |
| R-007 | TECH | 에뮬레이터 vs 실제 서버 URL 분기 이슈 | 2 | 1 | 2 | BuildConfig로 환경별 URL 관리 |

### Risk Category Legend

- **SEC**: Security (access controls, auth, data exposure)
- **DATA**: Data Integrity (loss, corruption, inconsistency)
- **TECH**: Technical/Architecture (flaws, integration, scalability)
- **BUS**: Business Impact (UX harm, logic errors)
- **OPS**: Operations (deployment, config, monitoring)

---

## Test Coverage Plan

### P0 (Critical) - Run on every commit

**Criteria**: Blocks core journey + High risk (≥6) + No workaround

| Requirement | Test Level | Risk Link | Test Count | Owner | Notes |
|-------------|------------|-----------|------------|-------|-------|
| FR6: 보안키 로그인 API | API | R-001 | 4 | QA | 유효/무효/빈값/비활성 키 |
| FR21: 토큰 로컬 저장 | Unit | R-002 | 3 | DEV | 저장/조회/삭제 + 암호화 검증 |
| FR10: 기구 목록 API | API | - | 3 | QA | 목록 조회, 인증 검증, 빈 목록 |
| **NEW** 토큰 서버 검증 | API | R-002 | 1 | QA | SplashActivity 토큰 유효성 서버 검증 |

**Total P0**: 11 tests, 22 hours

### P1 (High) - Run on PR to main

**Criteria**: Important features + Medium risk (3-4) + Common workflows

| Requirement | Test Level | Risk Link | Test Count | Owner | Notes |
|-------------|------------|-----------|------------|-------|-------|
| FR16: Android 로그인 화면 | E2E | R-001 | 5 | QA | UI 흐름, 에러 표시, 화면 전환 |
| FR17: Android 기구 목록 화면 | E2E | - | 4 | QA | 목록 표시, 아이템 탭, Pull to Refresh |
| NFR5: 기존 API 호환성 | API | R-004 | 2 | QA | /api_root/Post/, /api-token-auth/ |
| **NEW** 앱 시작 분기 로직 | E2E | - | 2 | QA | SplashActivity 토큰 확인/분기 |
| **NEW** 토큰 만료 처리 | E2E | R-002 | 1 | QA | 만료 토큰 → Login 화면 이동 |
| **NEW** Pull to Refresh 데이터 갱신 | E2E | - | 1 | QA | 새 데이터 추가 후 Refresh 반영 확인 |
| **NEW** 기구 카드 이벤트 개수 정확성 | E2E | - | 1 | QA | 표시 개수와 실제 DB 일치 검증 |
| **NEW** 네트워크 에러 후 재시도 | E2E | R-006 | 1 | QA | 네트워크 복구 후 재시도 동작 |
| **NEW** 오프라인→온라인 복구 | E2E | R-006 | 1 | QA | 네트워크 복구 시 자동 갱신 |

**Total P1**: 18 tests, 18 hours

### P2 (Medium) - Run nightly/weekly

**Criteria**: Secondary features + Low risk (1-2) + Edge cases

| Requirement | Test Level | Risk Link | Test Count | Owner | Notes |
|-------------|------------|-----------|------------|-------|-------|
| FR14: RecyclerView 패턴 | Component | - | 3 | DEV | MachineAdapter, ViewHolder 테스트 |
| FR15: API 클라이언트 패턴 | Unit | - | 4 | DEV | HTTP 연결, JSON 파싱, 에러 처리 |
| NFR8: 네트워크 에러 처리 | E2E | R-006 | 3 | QA | 오프라인, 타임아웃, 서버 에러 |
| **NEW** 로그아웃 기능 | E2E | - | 1 | QA | 토큰 삭제 + Login 화면 이동 |
| **NEW** 화면 회전 상태 유지 | E2E | - | 2 | QA | Configuration Change 시 데이터 보존 |
| **NEW** 앱 백그라운드 복귀 | E2E | - | 1 | QA | 포그라운드 복귀 시 데이터 갱신 |
| **NEW** Activity 재생성 복원 | E2E | - | 1 | QA | 메모리 부족 재생성 시 상태 복원 |

**Total P2**: 15 tests, 7.5 hours

---

## Execution Order

### Smoke Tests (<5 min)

**Purpose**: Fast feedback, catch build-breaking issues

- [ ] POST /api/auth/login/ with valid key → 200 OK (30s)
- [ ] GET /api_root/machines/ with valid token → 200 OK (30s)
- [ ] Android app launch → Login screen displayed (1min)

**Total**: 3 scenarios

### P0 Tests (<10 min)

**Purpose**: Critical path validation

- [ ] BE-1-01: 유효한 보안키 로그인 (API)
- [ ] BE-1-02: 잘못된 보안키 → 401 Unauthorized (API)
- [ ] BE-1-03: 빈 보안키 → 400 Bad Request (API)
- [ ] BE-1-04: 비활성 사용자 → 401 Unauthorized (API)
- [ ] BE-1-05: 기구 목록 조회 (인증됨) (API)
- [ ] BE-1-06: 기구 목록 조회 (미인증) → 401 (API)
- [ ] BE-1-07: 빈 기구 목록 처리 (API)
- [ ] AN-1-01: 토큰 저장 (Unit)
- [ ] AN-1-02: 토큰 조회 (Unit)
- [ ] AN-1-03: 토큰 삭제 (Unit)
- [ ] **NEW** BE-1-10: 토큰 서버 검증 API (API)

**Total**: 11 scenarios

### P1 Tests (<30 min)

**Purpose**: Important feature coverage

- [ ] AN-1-04: 앱 최초 실행 → Login 화면 (E2E)
- [ ] AN-1-05: 유효한 보안키 → MachineList 이동 (E2E)
- [ ] AN-1-06: 잘못된 보안키 → 에러 Toast (E2E)
- [ ] AN-1-07: 빈 입력 → 유효성 에러 (E2E)
- [ ] AN-1-08: 저장된 토큰 → 자동 로그인 (E2E)
- [ ] AN-1-09: 기구 목록 로딩 (E2E)
- [ ] AN-1-10: 기구 카드 정보 표시 (E2E)
- [ ] AN-1-11: 기구 탭 → EventList 이동 (E2E)
- [ ] AN-1-12: Pull to Refresh (E2E)
- [ ] BE-1-08: /api_root/Post/ 호환성 (API)
- [ ] BE-1-09: /api-token-auth/ 호환성 (API)
- [ ] AN-1-13: SplashActivity 토큰 확인 후 분기 (E2E)
- [ ] AN-1-14: 만료된 토큰 → Login 이동 (E2E)
- [ ] **NEW** AN-1-15: Pull to Refresh 후 새 데이터 반영 (E2E)
- [ ] **NEW** AN-1-16: 기구 카드 이벤트 개수 정확성 검증 (E2E)
- [ ] **NEW** AN-1-17: 네트워크 에러 후 재시도 동작 (E2E)
- [ ] **NEW** AN-1-18: 오프라인→온라인 복구 시 자동 갱신 (E2E)

**Total**: 17 scenarios

### P2/P3 Tests (<60 min)

**Purpose**: Full regression coverage

- [ ] AN-2-01: MachineAdapter 바인딩 테스트 (Component)
- [ ] AN-2-02: ViewHolder 클릭 리스너 (Component)
- [ ] AN-2-03: Empty State 표시 (Component)
- [ ] AN-2-04: HttpURLConnection 생성 (Unit)
- [ ] AN-2-05: JSON → GymMachine 파싱 (Unit)
- [ ] AN-2-06: 네트워크 예외 처리 (Unit)
- [ ] AN-2-07: Authorization 헤더 설정 (Unit)
- [ ] AN-2-08: 오프라인 모드 에러 표시 (E2E)
- [ ] AN-2-09: 타임아웃 처리 (E2E)
- [ ] AN-2-10: 서버 500 에러 처리 (E2E)
- [ ] AN-2-11: 로그아웃 → 토큰 삭제 → Login 이동 (E2E)
- [ ] **NEW** AN-2-12: 화면 회전 시 MachineList 상태 유지 (E2E)
- [ ] **NEW** AN-2-13: 화면 회전 시 Login 상태 유지 (E2E)
- [ ] **NEW** AN-2-14: 앱 백그라운드 복귀 후 데이터 갱신 (E2E)
- [ ] **NEW** AN-2-15: Activity 재생성 시 상태 복원 (E2E)

**Total**: 15 scenarios

---

## Resource Estimates

### Test Development Effort

| Priority | Count | Hours/Test | Total Hours | Notes |
|----------|-------|------------|-------------|-------|
| P0 | 11 | 2.0 | 22 | 보안 관련, 복잡한 설정 |
| P1 | 17 | 1.0 | 17 | E2E 및 API 표준 테스트 |
| P2 | 15 | 0.5 | 7.5 | Unit/Component 테스트 |
| **Total** | **43** | **-** | **46.5** | **~6 days** |

### Prerequisites

**Test Data:**
- ApiUser factory (security_key 자동 생성, 활성/비활성 상태)
- GymMachine factory (다양한 machine_type, location)

**Tooling:**
- Django TestCase / pytest-django for Backend
- Android JUnit / Espresso for Android
- curl / httpie for API 수동 테스트

**Environment:**
- Django 개발 서버 (localhost:8000)
- Android 에뮬레이터 (API 24+)
- 테스트용 SQLite 데이터베이스

---

## Quality Gate Criteria

### Pass/Fail Thresholds

- **P0 pass rate**: 100% (no exceptions)
- **P1 pass rate**: ≥95% (waivers required for failures)
- **P2/P3 pass rate**: ≥90% (informational)
- **High-risk mitigations**: 100% complete or approved waivers

### Coverage Targets

- **Critical paths (Login, Machine List)**: ≥80%
- **Security scenarios**: 100%
- **Business logic**: ≥70%
- **Edge cases**: ≥50%

### Non-Negotiable Requirements

- [x] All P0 tests pass
- [x] No high-risk (≥6) items unmitigated
- [x] Security tests (SEC category) pass 100%
- [x] 기존 API 호환성 유지 (NFR5)

---

## Mitigation Plans

### R-001: 보안키 brute-force 공격 (Score: 6)

**Mitigation Strategy:** Django REST Framework throttling 적용
```python
# settings.py
REST_FRAMEWORK = {
    'DEFAULT_THROTTLE_CLASSES': [
        'rest_framework.throttling.AnonRateThrottle',
    ],
    'DEFAULT_THROTTLE_RATES': {
        'anon': '5/minute',  # 로그인 시도 제한
    }
}
```
**Owner:** Backend
**Timeline:** Sprint 1
**Status:** Planned
**Verification:** brute-force 시나리오 테스트 (5회 초과 시 429 응답)

### R-002: 토큰 로컬 저장 평문 노출 (Score: 6)

**Mitigation Strategy:** EncryptedSharedPreferences 사용
```java
// SecureTokenManager.java
MasterKey masterKey = new MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build();

EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
);
```
**Owner:** Android
**Timeline:** Sprint 1
**Status:** Planned
**Verification:** SharedPreferences 파일 직접 검사 시 암호화 확인

---

## Assumptions and Dependencies

### Assumptions

1. Django 서버가 localhost:8000에서 실행 가능
2. Android 에뮬레이터에서 10.0.2.2로 localhost 접근 가능
3. 테스트용 ApiUser, GymMachine 데이터가 사전 생성됨

### Dependencies

1. Django REST Framework 설치 - Required by Sprint 1
2. AndroidX Security 라이브러리 - Required by Sprint 1
3. 기존 Photo Blog 코드베이스 정상 동작

### Risks to Plan

- **Risk**: PythonAnywhere 배포 환경과 로컬 환경 차이
  - **Impact**: 일부 테스트가 배포 환경에서 실패 가능
  - **Contingency**: 환경별 테스트 설정 분리, CI/CD 파이프라인에서 스테이징 테스트

---

## Test Cases Detail

### Backend API Tests

#### BE-1-01: 유효한 보안키 로그인
```bash
curl -X POST http://localhost:8000/api/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"security_key": "test-security-key-12345"}'

# Expected: 200 OK
# Response: {"token": "abc123...", "name": "헬스장 관리자"}
```

#### BE-1-02: 잘못된 보안키 로그인
```bash
curl -X POST http://localhost:8000/api/auth/login/ \
  -H "Content-Type: application/json" \
  -d '{"security_key": "wrong-key"}'

# Expected: 401 Unauthorized
# Response: {"error": "Invalid security key"}
```

#### BE-1-05: 기구 목록 조회 (인증됨)
```bash
curl -H "Authorization: Token <valid_token>" \
  http://localhost:8000/api_root/machines/

# Expected: 200 OK
# Response: [{"id": 1, "name": "런닝머신 #1", ...}, ...]
```

### Android UI Tests

#### AN-1-05: 유효한 보안키 → MachineList 이동
```java
@Test
public void testLoginSuccess_navigatesToMachineList() {
    // Given: Login screen is displayed
    onView(withId(R.id.etSecurityKey))
        .perform(typeText("test-security-key-12345"));

    // When: Login button clicked
    onView(withId(R.id.btnLogin)).perform(click());

    // Then: MachineListActivity is displayed
    intended(hasComponent(MachineListActivity.class.getName()));
}
```

#### AN-1-13: SplashActivity 토큰 확인 후 분기
```java
@Test
public void testSplashActivity_withToken_navigatesToMachineList() {
    // Given: Valid token stored in SharedPreferences
    SecureTokenManager.saveToken(context, "valid-token");

    // When: App launches (SplashActivity)
    ActivityScenario.launch(SplashActivity.class);

    // Then: Navigates to MachineListActivity (skip login)
    intended(hasComponent(MachineListActivity.class.getName()));
}

@Test
public void testSplashActivity_withoutToken_navigatesToLogin() {
    // Given: No token stored
    SecureTokenManager.clearToken(context);

    // When: App launches (SplashActivity)
    ActivityScenario.launch(SplashActivity.class);

    // Then: Navigates to LoginActivity
    intended(hasComponent(LoginActivity.class.getName()));
}
```

#### AN-1-14: 만료된 토큰 → Login 이동
```java
@Test
public void testExpiredToken_navigatesToLogin() {
    // Given: Expired token stored
    SecureTokenManager.saveToken(context, "expired-token");

    // When: API call returns 401 Unauthorized
    // (Token validation fails)
    mockServer.enqueue(new MockResponse().setResponseCode(401));

    ActivityScenario.launch(MachineListActivity.class);

    // Then: Clears token and navigates to LoginActivity
    intended(hasComponent(LoginActivity.class.getName()));
    assertNull(SecureTokenManager.getToken(context));
}
```

#### AN-2-11: 로그아웃 → 토큰 삭제 → Login 이동
```java
@Test
public void testLogout_clearsTokenAndNavigatesToLogin() {
    // Given: User is logged in (on MachineListActivity)
    SecureTokenManager.saveToken(context, "valid-token");
    ActivityScenario<MachineListActivity> scenario =
        ActivityScenario.launch(MachineListActivity.class);

    // When: Logout button/menu clicked
    onView(withId(R.id.btnLogout)).perform(click());

    // Then: Token is cleared and navigates to LoginActivity
    assertNull(SecureTokenManager.getToken(context));
    intended(hasComponent(LoginActivity.class.getName()));
}
```

### NEW - 추가된 테스트 케이스

#### BE-1-10: 토큰 서버 검증 API
```bash
curl -H "Authorization: Token <valid_token>" \
  http://localhost:8000/api/auth/verify/

# Expected: 200 OK (유효한 토큰)
# Response: {"valid": true, "user": "헬스장 관리자"}

curl -H "Authorization: Token <invalid_token>" \
  http://localhost:8000/api/auth/verify/

# Expected: 401 Unauthorized (무효한 토큰)
```

#### AN-1-15: Pull to Refresh 후 새 데이터 반영
```java
@Test
public void testPullToRefresh_showsNewData() {
    // Given: MachineList 표시 중, 서버에 새 기구 추가됨
    ActivityScenario.launch(MachineListActivity.class);
    int initialCount = getRecyclerViewItemCount(R.id.recyclerView);

    // 서버에 새 기구 추가 (mock 또는 실제 API)
    addNewMachineToServer();

    // When: Pull to Refresh 수행
    onView(withId(R.id.swipeRefreshLayout))
        .perform(swipeDown());

    // Then: 새 데이터가 목록에 반영됨
    onView(withId(R.id.recyclerView))
        .check(matches(hasChildCount(initialCount + 1)));
}
```

#### AN-1-16: 기구 카드 이벤트 개수 정확성 검증
```java
@Test
public void testMachineCard_eventCountAccuracy() {
    // Given: 기구에 이벤트 152개 존재
    setupMachineWithEvents(machineId, 152);

    ActivityScenario.launch(MachineListActivity.class);

    // Then: 카드에 "이벤트: 152개" 표시
    onView(withId(R.id.recyclerView))
        .check(matches(hasDescendant(withText(containsString("152")))));
}
```

#### AN-1-17: 네트워크 에러 후 재시도 동작
```java
@Test
public void testNetworkError_retryButtonWorks() {
    // Given: 네트워크 에러 발생
    mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
    ActivityScenario.launch(MachineListActivity.class);

    // Then: 에러 메시지와 재시도 버튼 표시
    onView(withText("네트워크 오류")).check(matches(isDisplayed()));
    onView(withId(R.id.btnRetry)).check(matches(isDisplayed()));

    // When: 네트워크 복구 후 재시도
    mockServer.enqueue(new MockResponse().setBody("[{\"id\":1}]"));
    onView(withId(R.id.btnRetry)).perform(click());

    // Then: 데이터 로딩 성공
    onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
}
```

#### AN-1-18: 오프라인→온라인 복구 시 자동 갱신
```java
@Test
public void testOfflineToOnline_autoRefresh() {
    // Given: 오프라인 상태에서 앱 시작
    setNetworkState(false);
    ActivityScenario.launch(MachineListActivity.class);
    onView(withText("오프라인")).check(matches(isDisplayed()));

    // When: 네트워크 복구
    setNetworkState(true);

    // Then: 자동으로 데이터 갱신
    onView(withId(R.id.recyclerView))
        .check(matches(hasMinimumChildCount(1)));
}
```

#### AN-2-12: 화면 회전 시 MachineList 상태 유지
```java
@Test
public void testRotation_machineListStatePreserved() {
    // Given: MachineList에 데이터 표시 중
    ActivityScenario<MachineListActivity> scenario =
        ActivityScenario.launch(MachineListActivity.class);

    // When: 화면 회전
    scenario.onActivity(activity -> {
        activity.setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    });

    // Then: 데이터 유지, API 재호출 없음
    onView(withId(R.id.recyclerView))
        .check(matches(hasMinimumChildCount(1)));
}
```

#### AN-2-14: 앱 백그라운드 복귀 후 데이터 갱신
```java
@Test
public void testBackgroundToForeground_dataRefresh() {
    // Given: MachineList 표시 중
    ActivityScenario<MachineListActivity> scenario =
        ActivityScenario.launch(MachineListActivity.class);

    // When: 백그라운드 → 포그라운드
    scenario.moveToState(Lifecycle.State.CREATED);
    // 서버에 새 데이터 추가
    addNewMachineToServer();
    scenario.moveToState(Lifecycle.State.RESUMED);

    // Then: 데이터 갱신됨 (또는 갱신 알림 표시)
    // 구현에 따라 자동 갱신 또는 갱신 프롬프트
}
```

---

## Approval

**Test Design Approved By:**

- [ ] Product Manager: _______ Date: _______
- [ ] Tech Lead: _______ Date: _______
- [ ] QA Lead: _______ Date: _______

**Comments:**

---

## Appendix

### Knowledge Base References

- `risk-governance.md` - Risk classification framework
- `probability-impact.md` - Risk scoring methodology
- `test-levels-framework.md` - Test level selection
- `test-priorities-matrix.md` - P0-P3 prioritization

### Related Documents

- PRD: `docs/final/gym-service-overview.md`
- Epic: `_bmad-output/epics.md` (Epic 1)
- Architecture: `docs/final/implementation/02-backend.md`
- Android Spec: `docs/final/implementation/04-android.md`
- API Reference: `docs/final/implementation/05-api-reference.md`

---

**Generated by**: BMad TEA Agent - Test Architect Module
**Workflow**: `_bmad/bmm/testarch/test-design`
**Version**: 4.0 (BMad v6)
