---
epic: 2
title: Usage Event Monitoring & Detection
status: complete
storiesCompleted: [2.1, 2.2, 2.3, 2.4, 2.5, 2.6]
---

# Epic 2: Usage Event Monitoring & Detection

**Goal:** Users can view detailed usage history for any equipment, filter by event type and date, and see event details. Edge devices can detect and report usage events automatically.

**FRs Covered:** FR1, FR2, FR3, FR4, FR5, FR7, FR8, FR9, FR11, FR12, FR14 (Events), FR15 (Events), FR18, FR19

**Implementation Order:** Backend → Edge → Android

---

## Story 2.1: Backend Event Model & Posting API

**As an** Edge device,
**I want** to post usage events (start/end) with images to the server,
**So that** equipment usage is automatically logged in the system.

**Acceptance Criteria:**

**Given** the Django server is running with MachineEvent model
**When** I POST to `/api/machines/{machine_id}/events/` with multipart form data containing image, event_type, captured_at, person_count, detections JSON, change_info JSON
**Then** a new MachineEvent is created and I receive HTTP 201 with the event data

**Given** I POST an event with event_type="start"
**When** the request is processed
**Then** the event is stored with event_type="start" and person_count >= 1

**Given** I POST an event with event_type="end"
**When** the request is processed
**Then** the event is stored with event_type="end" and person_count = 0

**Given** the referenced machine_id does not exist
**When** I POST an event
**Then** I receive HTTP 404 Not Found

**Given** I have no valid auth token
**When** I POST an event
**Then** I receive HTTP 401 Unauthorized

**Given** MachineEvent model is created
**When** I access Django admin
**Then** I can view all events with image previews and filter by machine, event_type, date

---

## Story 2.2: Backend Event List & Detail API

**As a** gym manager,
**I want** to retrieve usage events via API with filtering options,
**So that** I can view equipment usage history filtered by type and date.

**Acceptance Criteria:**

**Given** I have a valid auth token
**When** I GET `/api/machines/{machine_id}/events/`
**Then** I receive HTTP 200 with a JSON array of events for that machine, ordered by captured_at descending

**Given** events exist for a machine
**When** I GET `/api/machines/{machine_id}/events/?event_type=start`
**Then** I receive only events with event_type="start"

**Given** events exist for a machine
**When** I GET `/api/machines/{machine_id}/events/?event_type=end`
**Then** I receive only events with event_type="end"

**Given** events exist for a machine
**When** I GET `/api/machines/{machine_id}/events/?date_from=2024-01-01&date_to=2024-01-31`
**Then** I receive only events within the specified date range

**Given** I have an event_id
**When** I GET `/api_root/events/{event_id}/`
**Then** I receive full event details including: id, machine, machine_name, event_type, event_type_display, image URL, captured_at, created_at, person_count, detections JSON, change_info JSON

**Given** many events exist
**When** I GET the event list
**Then** results are paginated with default page size

**Given** I request a non-existent event_id
**When** I GET `/api_root/events/{event_id}/`
**Then** I receive HTTP 404 Not Found

---

## Story 2.3: Edge Bidirectional Change Detection

**As an** Edge system,
**I want** to detect both usage start (0→1) and usage end (1→0) events for the target class,
**So that** complete usage sessions are tracked automatically.

**Acceptance Criteria:**

**Given** the ChangeDetection class is initialized with target_class="person"
**When** person count changes from 0 to 1 or more
**Then** a "start" event is triggered with change_info containing prev_count=0 and curr_count>=1

**Given** person count was previously 1 or more
**When** person count changes to 0
**Then** an "end" event is triggered with change_info containing prev_count>=1 and curr_count=0

**Given** person count changes from 1 to 2
**When** the frame is processed
**Then** no event is triggered (only 0→N and N→0 transitions matter)

**Given** person count remains at 0 across frames
**When** frames are processed
**Then** no event is triggered (no state change)

**Given** an event is triggered
**When** the change_info is generated
**Then** it includes: event_type, target_class, prev_count, curr_count, timestamp (ISO format)

**Given** an event is triggered
**When** the image is captured
**Then** the current frame is resized to 640x480 and saved locally to `runs/detect/events/`

---

## Story 2.4: Edge Event Upload Integration

**As an** Edge system,
**I want** to authenticate with the server and upload detected events via REST API,
**So that** usage events are stored in the central system in real-time.

**Acceptance Criteria:**

**Given** the Edge system starts with a configured security_key
**When** the ChangeDetection class initializes
**Then** it authenticates via POST `/api/auth/login/` and stores the returned token

**Given** authentication fails (invalid security_key)
**When** the ChangeDetection class initializes
**Then** an error is logged and the system raises an exception

**Given** a change event is detected
**When** the event is processed
**Then** the system POSTs to `/api/machines/{machine_id}/events/` with Authorization header, image file, event_type, captured_at, person_count, detections JSON, change_info JSON

**Given** the event upload succeeds (HTTP 201)
**When** the response is received
**Then** a success message is logged with event_type

**Given** the event upload fails (HTTP 4xx/5xx)
**When** the response is received
**Then** an error message is logged with status code

**Given** configuration is needed
**When** the Edge system loads
**Then** it reads host, security_key, machine_id, target_class from environment variables or config file

**Given** the server is configured
**When** running detect.py
**Then** the system connects to the configured host (default: https://mouseku.pythonanywhere.com)

---

## Story 2.5: Android Event List Screen

**As a** gym manager,
**I want** to view usage events for a specific equipment with filtering options,
**So that** I can monitor when equipment was used and filter by event type.

**Acceptance Criteria:**

**Given** I tap on an equipment item from the Equipment List
**When** events exist for that equipment
**Then** I see a RecyclerView displaying events ordered by captured_at descending

**Given** I am on the Event List screen
**When** I view an event item
**Then** I see: thumbnail image, event type chip (START in green / END in red), captured_at timestamp, person count

**Given** I am on the Event List screen
**When** I tap the "전체" (All) filter chip
**Then** all events are displayed

**Given** I am on the Event List screen
**When** I tap the "사용 시작" (Start) filter chip
**Then** only start events are displayed (API called with ?event_type=start)

**Given** I am on the Event List screen
**When** I tap the "사용 종료" (End) filter chip
**Then** only end events are displayed (API called with ?event_type=end)

**Given** I am on the Event List screen
**When** I tap the date range selector
**Then** I can select date_from and date_to to filter events

**Given** I am on the Event List screen
**When** I tap on an event item
**Then** I navigate to the Event Detail screen

**Given** I am on the Event List screen
**When** I pull down to refresh
**Then** the event list is refreshed from the API

**Given** no events exist for the equipment
**When** the screen loads
**Then** I see the empty state: "아직 이벤트가 없습니다" with subtitle

---

## Story 2.6: Android Event Detail Screen

**As a** gym manager,
**I want** to view detailed information about a specific usage event,
**So that** I can see the captured image and detection metadata.

**Acceptance Criteria:**

**Given** I tap on an event from the Event List
**When** the Event Detail screen loads
**Then** I see the full event information from GET `/api_root/events/{event_id}/`

**Given** I am on the Event Detail screen
**When** I view the screen
**Then** I see: equipment name in the app bar, event type chip (START/END with color)

**Given** I am on the Event Detail screen
**When** I view the image section
**Then** I see the captured image displayed at 4:3 aspect ratio

**Given** I am on the Event Detail screen
**When** I tap on the image
**Then** the image opens in full-screen view mode

**Given** I am on the Event Detail screen
**When** I view the details section
**Then** I see: Timestamp (captured_at formatted), Detected Persons count, Event Summary (start/end description)

**Given** I am on the Event Detail screen
**When** I tap the back button
**Then** I return to the Event List screen

**Given** the API call fails
**When** the Event Detail screen loads
**Then** I see an error message with retry option
