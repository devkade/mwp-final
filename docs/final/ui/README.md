# UI 스크린 디자인

헬스장 운동기구 감지 시스템의 Android 앱 UI 디자인 파일입니다.

---

## 파일 목록

| 화면 | 스크린샷 | HTML 프로토타입 |
|------|----------|-----------------|
| 로그인 | [login.png](login.png) | [login.html](login.html) |
| 운동기구 목록 | [equipment_list.png](equipment_list.png) | [equipment_list.html](equipment_list.html) |
| 사용 기록 | [usage_history.png](usage_history.png) | [usage_history.html](usage_history.html) |
| 이벤트 상세 | [event_details.png](event_details.png) | [event_details.html](event_details.html) |
| 사용 통계 | [usage_statistics.png](usage_statistics.png) | [usage_statistics.html](usage_statistics.html) |

---

## 화면별 설명

### 1. 로그인 화면 (Login)

**파일**: `login.png`, `login.html`

**구성 요소**:
- 앱 로고 및 타이틀 (GymFlow Manager)
- 보안키(Security Key) 입력 필드
- 로그인 버튼
- 보안키 안내 문구

**동작**:
- 보안키 입력 후 로그인 버튼 클릭
- 서버 인증 성공 시 운동기구 목록 화면으로 이동
- 토큰을 로컬에 저장하여 재사용

---

### 2. 운동기구 목록 화면 (Equipment List)

**파일**: `equipment_list.png`, `equipment_list.html`

**구성 요소**:
- 상단 앱바 (메뉴, 필터 버튼)
- 검색 바 (기구명/ID 검색)
- 필터 칩 (All, Cardio Area, Weight Room, Functional)
- 기구 목록 (RecyclerView)
  - 썸네일 이미지
  - 기구명, 설명
  - 위치 정보
  - 상태 표시 (In Use, Available, Maintenance)
- FAB (기구 추가 버튼)

**동작**:
- 기구 항목 탭 시 사용 기록 화면으로 이동
- 필터 칩으로 구역별 필터링
- 검색으로 기구 찾기

---

### 3. 운동기구 사용 기록 화면 (Usage History)

**파일**: `usage_history.png`, `usage_history.html`

**구성 요소**:
- 상단 앱바 (뒤로가기, 기구명 표시)
- 날짜 범위 선택 버튼
- 이벤트 타입 필터 칩 (전체, 사용 시작, 사용 종료)
- 이벤트 목록 (RecyclerView)
  - 썸네일 이미지
  - 이벤트 타입 아이콘 (재생/정지)
  - 이벤트명 (사용 시작/종료)
  - 캡처 시간

**동작**:
- 날짜 범위로 기간 필터링
- 이벤트 타입 칩으로 필터링
- 이벤트 항목 탭 시 상세 화면으로 이동

---

### 4. 이벤트 상세 화면 (Event Details)

**파일**: `event_details.png`, `event_details.html`

**구성 요소**:
- 상단 앱바 (뒤로가기)
- 기구명 및 이벤트 타입 칩 (START/END)
- 캡처 이미지 (4:3 비율)
- 상세 정보 목록
  - Timestamp (캡처 시각)
  - Detected Persons (감지된 인원 수)
  - Event Summary (이벤트 요약)
- Report Issue 버튼

**동작**:
- 이미지 탭 시 전체화면 보기
- Report Issue로 문제 신고

---

### 5. 사용 통계 화면 (Usage Statistics)

**파일**: `usage_statistics.png`, `usage_statistics.html`

**구성 요소**:
- 상단 앱바 (뒤로가기, 더보기 메뉴)
- 날짜 범위 선택기
- 주요 지표 카드
  - Total Workouts (총 운동 횟수)
  - Busiest Hour (피크 시간)
  - Most Used Equipment (가장 많이 사용된 기구)
- Usage by Equipment 섹션
  - 가로 막대 차트 (기구별 사용량)
- Busiest Times 섹션
  - Heatmap/List 토글
  - 요일×시간대 히트맵

**동작**:
- 날짜 범위 변경 시 통계 갱신
- Heatmap/List 뷰 전환

---

## 디자인 시스템

### 색상

| 색상명 | 값 | 용도 |
|--------|-----|------|
| Primary | `#12c0e2` | 주요 액션, 강조 |
| Background Light | `#f6f8f8` | 라이트 모드 배경 |
| Background Dark | `#101f22` | 다크 모드 배경 |
| Success | `#28A745` | 사용 시작 (START) |
| Danger | `#DC3545` | 사용 종료 (END) |

### 폰트

- **Font Family**: Inter
- **Weights**: 400 (Regular), 500 (Medium), 700 (Bold)

### 아이콘

- **Material Symbols Outlined** 사용
- 주요 아이콘: `arrow_back`, `menu`, `tune`, `search`, `calendar_month`, `schedule`, `person`, `play_arrow`, `stop`

---

## 관련 문서

- **구현 스펙**: [../implementation/README.md](../implementation/README.md)
- **설계 문서**: [../gym-service-overview.md](../gym-service-overview.md)
