# Story 2.6: Android Event Detail Screen

Status: ready-for-dev

## Story

As a **gym manager**,
I want **to view detailed information about a specific usage event**,
so that **I can see the captured image and detection metadata**.

## Acceptance Criteria

1. **Given** I tap on an event from the Event List **When** the Event Detail screen loads **Then** I see the full event information from GET `/api_root/events/{event_id}/`

2. **Given** I am on the Event Detail screen **When** I view the screen **Then** I see: equipment name in the app bar, event type chip (START/END with color)

3. **Given** I am on the Event Detail screen **When** I view the image section **Then** I see the captured image displayed at 4:3 aspect ratio

4. **Given** I am on the Event Detail screen **When** I tap on the image **Then** the image opens in full-screen view mode

5. **Given** I am on the Event Detail screen **When** I view the details section **Then** I see: Timestamp (captured_at formatted), Detected Persons count, Event Summary (start/end description)

6. **Given** I am on the Event Detail screen **When** I tap the back button **Then** I return to the Event List screen

7. **Given** the API call fails **When** the Event Detail screen loads **Then** I see an error message with retry option

## Tasks / Subtasks

- [ ] Task 1: Create EventDetailActivity (AC: #1, #2, #5, #6, #7)
  - [ ] 1.1: Create `EventDetailActivity.java`
  - [ ] 1.2: Get event_id from Intent extras
  - [ ] 1.3: Implement loadEventDetail() method to fetch from API
  - [ ] 1.4: Parse JSON response into MachineEvent object
  - [ ] 1.5: Populate UI with event data
  - [ ] 1.6: Handle back button navigation
  - [ ] 1.7: Implement error handling with retry option

- [ ] Task 2: Create activity_event_detail.xml layout (AC: #2, #3, #5)
  - [ ] 2.1: Add Toolbar/AppBar with equipment name
  - [ ] 2.2: Add ImageView for captured image (4:3 aspect ratio)
  - [ ] 2.3: Add Chip for event type (START green / END red)
  - [ ] 2.4: Add TextView for timestamp (formatted)
  - [ ] 2.5: Add TextView for detected persons count
  - [ ] 2.6: Add TextView for event summary/description
  - [ ] 2.7: Add error state layout with retry button
  - [ ] 2.8: Add loading progress indicator

- [ ] Task 3: Implement image loading (AC: #3)
  - [ ] 3.1: Load image from URL using existing image loading pattern
  - [ ] 3.2: Set ImageView aspect ratio to 4:3
  - [ ] 3.3: Show placeholder while loading
  - [ ] 3.4: Handle image load failure gracefully

- [ ] Task 4: Implement full-screen image view (AC: #4)
  - [ ] 4.1: Set click listener on main image
  - [ ] 4.2: Create full-screen image dialog or activity
  - [ ] 4.3: Support pinch-to-zoom (optional)
  - [ ] 4.4: Add close button for full-screen view

- [ ] Task 5: Format and display event details (AC: #5)
  - [ ] 5.1: Format captured_at to localized date/time string
  - [ ] 5.2: Display person count with label
  - [ ] 5.3: Generate event summary text based on event_type
  - [ ] 5.4: Apply appropriate styling to text elements

- [ ] Task 6: Implement error handling (AC: #7)
  - [ ] 6.1: Show error message on API failure
  - [ ] 6.2: Add "다시 시도" (Retry) button
  - [ ] 6.3: Hide content views, show error view on failure
  - [ ] 6.4: Retry button calls loadEventDetail() again

- [ ] Task 7: Navigation integration (AC: #1, #6)
  - [ ] 7.1: Modify EventListActivity to launch EventDetailActivity on item click
  - [ ] 7.2: Pass event_id in Intent
  - [ ] 7.3: Handle up navigation to return to Event List
  - [ ] 7.4: Register EventDetailActivity in AndroidManifest.xml

- [ ] Task 8: Register Activity in AndroidManifest (AC: #1)
  - [ ] 8.1: Add EventDetailActivity to AndroidManifest.xml
  - [ ] 8.2: Set parent activity to EventListActivity for up navigation

## Dev Notes

### Technical Requirements

**Target SDK:** 36 (Android 14)
**Min SDK:** 24 (Android 7.0)

**API Endpoint:**
- `GET /api_root/events/{event_id}/`
- Headers: `Authorization: Token {token}`

**Response Fields:**
```json
{
  "id": 1,
  "machine": 1,
  "machine_name": "런닝머신 #1",
  "event_type": "start",
  "event_type_display": "사용 시작",
  "image": "/media/events/2024/01/15/event_001.jpg",
  "captured_at": "2024-01-15T14:35:10",
  "created_at": "2024-01-15T14:35:12",
  "person_count": 1,
  "detections": {...},
  "change_info": {...}
}
```

**UI Colors (from UX Design):**
- Primary: #12c0e2
- Start (Success): #28A745
- End (Danger): #DC3545

**Date/Time Format:**
- Display: "2024년 1월 15일 오후 2:35" (Korean locale)

### Architecture Compliance

**Files to Create:**
- `app/src/main/java/com/example/photoviewer/EventDetailActivity.java`
- `app/src/main/res/layout/activity_event_detail.xml`
- `app/src/main/res/layout/dialog_fullscreen_image.xml` (optional)

**Files to Modify:**
- `app/src/main/java/com/example/photoviewer/EventListActivity.java` - Add navigation to EventDetailActivity
- `app/src/main/AndroidManifest.xml` - Register EventDetailActivity

**Code Patterns to Follow:**
- Use ExecutorService for background network calls (existing pattern)
- Use SecureTokenManager for auth token (existing pattern)
- Use runOnUiThread for UI updates (existing pattern)
- Use HttpURLConnection for API calls (existing pattern)

### Layout Specifications

**activity_event_detail.xml:**
```
┌─────────────────────────────────────────┐
│ ← 런닝머신 #1                           │  <- App Bar
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │                                     │ │
│ │           Captured Image            │ │  <- 4:3 ratio, clickable
│ │             640x480                 │ │
│ │                                     │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ [사용 시작]                              │  <- Chip (green)
│                                         │
│ 📅 발생 시각                             │
│    2024년 1월 15일 오후 2:35            │
│                                         │
│ 👥 감지 인원                             │
│    1명                                  │
│                                         │
│ 📝 이벤트 요약                           │
│    운동기구 사용이 시작되었습니다.        │
│                                         │
└─────────────────────────────────────────┘
```

**Error State:**
```
┌─────────────────────────────────────────┐
│                                         │
│          ⚠️ 오류 발생                    │
│                                         │
│   이벤트 정보를 불러올 수 없습니다.      │
│                                         │
│          [다시 시도]                     │
│                                         │
└─────────────────────────────────────────┘
```

### Event Summary Text

| event_type | Summary Text |
|------------|--------------|
| start | 운동기구 사용이 시작되었습니다. |
| end | 운동기구 사용이 종료되었습니다. |

### Testing Standards

**Test Type:** Manual UI testing + Unit tests
**Test Location:** `app/src/androidTest/java/com/example/photoviewer/`

**Test Scenarios:**
1. Event detail loads and displays correctly
2. Image displays at 4:3 aspect ratio
3. Tapping image opens full-screen view
4. Event type chip shows correct color
5. Timestamp formatted correctly
6. Back button returns to Event List
7. Error state shows on API failure
8. Retry button reloads event detail

**Commands:**
```bash
cd PhotoViewer
./gradlew connectedAndroidTest
```

### References

- [Source: docs/final/implementation/04-android.md#EventDetailActivity]
- [Source: docs/final/ui/event_details.html]
- [Source: _bmad-output/stories/epic-2-stories.md#Story 2.6]
- [Dependency: Story 2.2 (Backend Event Detail API)]
- [Dependency: Story 2.5 (EventListActivity navigation)]

## Dev Agent Record

### Agent Model Used

(To be filled after implementation)

### Debug Log References

(To be filled during implementation)

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
