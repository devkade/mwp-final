---
epic: 1
title: Equipment Discovery & System Access
status: complete
storiesCompleted: [1.1, 1.2, 1.3, 1.4]
---

# Epic 1: Equipment Discovery & System Access

**Goal:** Users can authenticate with a security key and view all available gym equipment with their status and location. Tapping equipment shows an empty state placeholder preparing users for Epic 2.

**FRs Covered:** FR6, FR10, FR14 (Equipment pattern), FR15 (API client pattern), FR16, FR17, FR21

---

## Story 1.1: Backend Authentication Setup

**As a** system administrator,
**I want** to create API users with security keys and authenticate them via REST API,
**So that** Edge devices and Android apps can securely access the system.

**Acceptance Criteria:**

**Given** the Django server is running
**When** I create an ApiUser with a unique security_key in the admin
**Then** the ApiUser is linked to a Django User and stored in the database

**Given** an active ApiUser exists with security_key "test-key-123"
**When** I POST to `/api/auth/login/` with `{"security_key": "test-key-123"}`
**Then** I receive HTTP 200 with `{"token": "<token>", "name": "<user_name>"}`

**Given** no ApiUser exists with security_key "invalid-key"
**When** I POST to `/api/auth/login/` with `{"security_key": "invalid-key"}`
**Then** I receive HTTP 401 with `{"error": "Invalid security key"}`

**Given** an ApiUser exists but is_active is False
**When** I POST to `/api/auth/login/` with that security_key
**Then** I receive HTTP 401 with `{"error": "Invalid security key"}`

---

## Story 1.2: Backend Equipment API

**As a** gym manager,
**I want** to retrieve a list of all gym equipment via REST API,
**So that** I can see what equipment is available in the facility.

**Acceptance Criteria:**

**Given** the Django server is running and I have a valid auth token
**When** I GET `/api_root/machines/` with header `Authorization: Token <token>`
**Then** I receive HTTP 200 with a JSON array of GymMachine objects

**Given** GymMachine objects exist in the database
**When** I GET `/api_root/machines/`
**Then** each object includes: id, name, machine_type, location, description, thumbnail, is_active, event_count, last_event

**Given** a GymMachine has is_active=False
**When** I GET `/api_root/machines/`
**Then** that machine is NOT included in the response (only active machines returned)

**Given** I have no valid auth token
**When** I GET `/api_root/machines/` without Authorization header
**Then** I receive HTTP 401 Unauthorized

**Given** multiple machines exist
**When** I GET `/api_root/machines/`
**Then** machines are ordered by location, then by name

**Given** GymMachine model is created
**When** I access Django admin at `/admin/`
**Then** I can create, edit, and delete GymMachine entries with all fields (name, machine_type choices, location, description, thumbnail, is_active)

---

## Story 1.3: Android Login Screen

**As a** gym manager,
**I want** to log in to the Android app using my security key,
**So that** I can securely access the gym monitoring system.

**Acceptance Criteria:**

**Given** the app is launched for the first time
**When** the app opens
**Then** I see the Login screen with security key input field and login button

**Given** I am on the Login screen
**When** I enter a valid security key and tap "Login"
**Then** the app calls POST `/api/auth/login/` with the security key
**And** on success, the token is stored securely in SharedPreferences
**And** I am navigated to the Equipment List screen

**Given** I am on the Login screen
**When** I enter an invalid security key and tap "Login"
**Then** I see an error message "잘못된 보안키입니다" (Invalid security key)
**And** I remain on the Login screen

**Given** I am on the Login screen
**When** I tap "Login" with an empty security key field
**Then** I see an error message "보안키를 입력하세요" (Please enter security key)

**Given** a valid token is already stored from a previous session
**When** I launch the app
**Then** I am automatically navigated to the Equipment List screen (skip login)

**Given** I am on the Login screen and the network is unavailable
**When** I tap "Login"
**Then** I see an error message indicating network failure
**And** I remain on the Login screen

---

## Story 1.4: Android Equipment List Screen

**As a** gym manager,
**I want** to view a list of all gym equipment in the app,
**So that** I can browse available equipment and select one to view its usage history.

**Acceptance Criteria:**

**Given** I am logged in and on the Equipment List screen
**When** the screen loads
**Then** I see a RecyclerView displaying all active gym machines from the API

**Given** equipment data is loaded
**When** I view the list
**Then** each item shows: thumbnail image, machine name, location, and event count

**Given** I am on the Equipment List screen
**When** I tap on an equipment item
**Then** I see an empty state screen with message "아직 이벤트가 없습니다" (No events yet)
**And** a subtitle "감지가 시작되면 사용 기록이 표시됩니다" (Usage history will appear when detection starts)

**Given** I am on the Equipment List screen
**When** I pull down to refresh
**Then** the equipment list is refreshed from the API

**Given** the API call fails due to network error
**When** the screen loads
**Then** I see an error message with a retry button

**Given** no equipment exists in the database
**When** the screen loads
**Then** I see an empty state message "등록된 운동기구가 없습니다" (No equipment registered)

**Given** I am on the Equipment List screen
**When** my auth token expires or becomes invalid
**Then** I am redirected to the Login screen
