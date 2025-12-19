# Story 2.5: Android Event List Screen

Status: ready-for-dev

## Story

As a **gym manager**,
I want **to view usage events for a specific equipment with filtering options**,
so that **I can monitor when equipment was used and filter by event type**.

## Acceptance Criteria

1. **Given** I tap on an equipment item from the Equipment List **When** events exist for that equipment **Then** I see a RecyclerView displaying events ordered by captured_at descending

2. **Given** I am on the Event List screen **When** I view an event item **Then** I see: thumbnail image, event type chip (START in green / END in red), captured_at timestamp, person count

3. **Given** I am on the Event List screen **When** I tap the "전체" (All) filter chip **Then** all events are displayed

4. **Given** I am on the Event List screen **When** I tap the "사용 시작" (Start) filter chip **Then** only start events are displayed (API called with ?event_type=start)

5. **Given** I am on the Event List screen **When** I tap the "사용 종료" (End) filter chip **Then** only end events are displayed (API called with ?event_type=end)

6. **Given** I am on the Event List screen **When** I tap the date range selector **Then** I can select date_from and date_to to filter events

7. **Given** I am on the Event List screen **When** I tap on an event item **Then** I navigate to the Event Detail screen

8. **Given** I am on the Event List screen **When** I pull down to refresh **Then** the event list is refreshed from the API

9. **Given** no events exist for the equipment **When** the screen loads **Then** I see the empty state: "아직 이벤트가 없습니다" with subtitle

## Tasks / Subtasks

- [x] Task 1: Create MachineEvent model class (AC: #1, #2)
  - [x] 1.1: Create `models/MachineEvent.java` with fields: id, machineId, machineName, eventType, eventTypeDisplay, imageUrl, capturedAt, personCount
  - [x] 1.2: Add getters and setters
  - [x] 1.3: Add JSON parsing constructor or use Gson

- [x] Task 2: Create EventAdapter for RecyclerView (AC: #1, #2)
  - [x] 2.1: Create `adapters/EventAdapter.java` extending RecyclerView.Adapter
  - [x] 2.2: Create ViewHolder with thumbnail, event type chip, timestamp, person count
  - [x] 2.3: Implement onBindViewHolder to populate event data
  - [x] 2.4: Set chip color: green (#28A745) for START, red (#DC3545) for END
  - [x] 2.5: Format capturedAt timestamp to readable format
  - [x] 2.6: Implement click listener for item selection

- [x] Task 3: Create item_event.xml layout (AC: #2)
  - [x] 3.1: Create MaterialCardView container
  - [x] 3.2: Add ImageView for thumbnail (100dp x 75dp, 4:3 ratio)
  - [x] 3.3: Add Chip for event type with color styling
  - [x] 3.4: Add TextView for capturedAt timestamp
  - [x] 3.5: Add TextView for person count

- [x] Task 4: Create EventListActivity (AC: #1, #3, #4, #5, #7, #8, #9)
  - [x] 4.1: Create `EventListActivity.java`
  - [x] 4.2: Get machine_id and machine_name from Intent extras
  - [x] 4.3: Set title to machine name
  - [x] 4.4: Initialize RecyclerView with LinearLayoutManager
  - [x] 4.5: Initialize EventAdapter with click listener
  - [x] 4.6: Implement loadEvents() method to fetch from API
  - [x] 4.7: Handle empty state visibility

- [x] Task 5: Create activity_event_list.xml layout (AC: #3, #4, #5, #8, #9)
  - [x] 5.1: Add ChipGroup with filter chips: 전체, 사용 시작, 사용 종료
  - [x] 5.2: Add SwipeRefreshLayout wrapping RecyclerView
  - [x] 5.3: Add RecyclerView for event list
  - [x] 5.4: Add empty state layout (hidden by default)
  - [x] 5.5: Style chips with appropriate colors

- [x] Task 6: Implement filter chip functionality (AC: #3, #4, #5)
  - [x] 6.1: Set up ChipGroup.OnCheckedChangeListener
  - [x] 6.2: Track filterEventType state (null, "start", "end")
  - [x] 6.3: Call loadEvents() when filter changes
  - [x] 6.4: Append ?event_type={filter} to API URL when filter active

- [x] Task 7: Implement date range filtering (AC: #6)
  - [x] 7.1: Add date range selector UI (button or date picker)
  - [x] 7.2: Show DatePickerDialog for date_from selection
  - [x] 7.3: Show DatePickerDialog for date_to selection
  - [x] 7.4: Append date_from and date_to query params to API URL
  - [x] 7.5: Display selected date range in UI

- [x] Task 8: Implement pull-to-refresh (AC: #8)
  - [x] 8.1: Set SwipeRefreshLayout.OnRefreshListener
  - [x] 8.2: Call loadEvents() on refresh
  - [x] 8.3: Stop refresh animation when load complete

- [x] Task 9: Navigate from Equipment List (AC: #1, #7)
  - [x] 9.1: Modify MachineListActivity item click to launch EventListActivity
  - [x] 9.2: Pass machine_id and machine_name in Intent
  - [x] 9.3: Handle EventListActivity item click to launch EventDetailActivity
  - [x] 9.4: Pass event_id in Intent

- [x] Task 10: Register Activity in AndroidManifest (AC: #1)
  - [x] 10.1: Add EventListActivity to AndroidManifest.xml
  - [x] 10.2: Set parent activity for up navigation

## Dev Notes

### Technical Requirements

**Target SDK:** 36 (Android 14)
**Min SDK:** 24 (Android 7.0)

**Dependencies (already in build.gradle):**
- RecyclerView: `androidx.recyclerview:recyclerview`
- SwipeRefreshLayout: `androidx.swiperefreshlayout:swiperefreshlayout`
- Material Design: `com.google.android.material:material`

**API Endpoint:**
- `GET /api/machines/{machine_id}/events/`
- Query params: `event_type`, `date_from`, `date_to`
- Headers: `Authorization: Token {token}`

**UI Colors (from UX Design):**
- Primary: #12c0e2
- Start (Success): #28A745
- End (Danger): #DC3545

**Date Format:**
- API: YYYY-MM-DD (ISO 8601)
- Display: Localized format (Korean)

### Architecture Compliance

**Files to Create:**
- `app/src/main/java/com/example/photoviewer/models/MachineEvent.java`
- `app/src/main/java/com/example/photoviewer/adapters/EventAdapter.java`
- `app/src/main/java/com/example/photoviewer/EventListActivity.java`
- `app/src/main/res/layout/activity_event_list.xml`
- `app/src/main/res/layout/item_event.xml`

**Files to Modify:**
- `app/src/main/java/com/example/photoviewer/MachineListActivity.java` - Add navigation to EventListActivity
- `app/src/main/AndroidManifest.xml` - Register EventListActivity

**Code Patterns to Follow:**
- Use ExecutorService for background network calls (existing pattern)
- Use SecureTokenManager for auth token (existing pattern)
- Use runOnUiThread for UI updates (existing pattern)
- Use BuildConfig.API_BASE_URL for server URL (existing pattern)

### Layout Specifications

**item_event.xml:**
```
┌─────────────────────────────────────────┐
│ ┌──────────┐  [START]                   │
│ │  Image   │  2024-01-15 14:35:10       │
│ │ 100x75   │  감지 인원: 1명            │
│ └──────────┘                            │
└─────────────────────────────────────────┘
```

**activity_event_list.xml:**
```
┌─────────────────────────────────────────┐
│ [전체] [사용 시작] [사용 종료]           │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │         Event Item 1                │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │         Event Item 2                │ │
│ └─────────────────────────────────────┘ │
│               ...                       │
└─────────────────────────────────────────┘
```

### Testing Standards

**Test Type:** Manual UI testing + Unit tests
**Test Location:** `app/src/androidTest/java/com/example/photoviewer/`

**Test Scenarios:**
1. Event list displays correctly with events
2. Empty state shows when no events
3. Filter chips change displayed events
4. Pull-to-refresh reloads data
5. Tapping event navigates to detail
6. Date range filter works correctly

**Commands:**
```bash
cd PhotoViewer
./gradlew connectedAndroidTest
```

### References

- [Source: docs/final/implementation/04-android.md#EventListActivity]
- [Source: docs/final/ui/usage_history.html]
- [Source: _bmad-output/stories/epic-2-stories.md#Story 2.5]
- [Dependency: Story 2.2 (Backend Event List API)]
- [Dependency: Story 1.4 (MachineListActivity navigation)]

## Dev Agent Record

### Agent Model Used

(To be filled after implementation)

### Implementation Plan

- Implement MachineEvent model with JSON parsing for event list fields and accessors.

### Debug Log References

- 2025-12-19: `./gradlew test --tests com.example.photoviewer.MachineEventTest` failed (Java Runtime not found).

### Completion Notes List

(To be filled after implementation)

### File List

**Created:**
(To be filled after implementation)

**Modified:**
(To be filled after implementation)

**Deleted:**
(None expected)

## Change Log

- 2025-12-19: Story created with comprehensive context from implementation specs
