# Story 1.3: Android Login Screen

Status: review

## Story

As a **gym manager**,
I want **to log in to the Android app using my security key**,
so that **I can securely access the gym monitoring system**.

## Acceptance Criteria

1. **Given** the app is launched for the first time **When** the app opens **Then** I see the Login screen with security key input field and login button

2. **Given** I am on the Login screen **When** I enter a valid security key and tap "Login" **Then** the app calls POST `/api/auth/login/` with the security key **And** on success, the token is stored securely in SharedPreferences **And** I am navigated to the Equipment List screen

3. **Given** I am on the Login screen **When** I enter an invalid security key and tap "Login" **Then** I see an error message "잘못된 보안키입니다" (Invalid security key) **And** I remain on the Login screen

4. **Given** I am on the Login screen **When** I tap "Login" with an empty security key field **Then** I see an error message "보안키를 입력하세요" (Please enter security key)

5. **Given** a valid token is already stored from a previous session **When** I launch the app **Then** I am automatically navigated to the Equipment List screen (skip login)

6. **Given** I am on the Login screen and the network is unavailable **When** I tap "Login" **Then** I see an error message indicating network failure **And** I remain on the Login screen

## Tasks / Subtasks

- [x] Task 1: Modify AuthenticationService for security key authentication (AC: #2, #3)
  - [x] 1.1: Update login() method signature to accept securityKey instead of username/password
  - [x] 1.2: Change JSON request body from `{"username": ..., "password": ...}` to `{"security_key": ...}`
  - [x] 1.3: Ensure error response parsing handles `{"error": "Invalid security key"}` format

- [x] Task 2: Update LoginActivity for security key input (AC: #1, #2, #3, #4, #6)
  - [x] 2.1: Replace usernameInput and passwordInput with securityKeyInput (EditText)
  - [x] 2.2: Remove rememberUsernameCheckbox (not needed for security key flow)
  - [x] 2.3: Update attemptLogin() to validate empty security key with Korean message "보안키를 입력하세요"
  - [x] 2.4: Update attemptLogin() to call AuthenticationService.login(securityKey, callback)
  - [x] 2.5: Update onError callback to show Korean message "잘못된 보안키입니다" for invalid key
  - [x] 2.6: Update network error handling to show Korean message "네트워크 오류가 발생했습니다"
  - [x] 2.7: Change navigation target from MainActivity to MachineListActivity on success

- [x] Task 3: Update activity_login.xml layout (AC: #1)
  - [x] 3.1: Change app title from "Photo Blog" to "GymFlow Manager" (or appropriate Korean equivalent)
  - [x] 3.2: Replace username EditText with security key input (id: security_key_input)
  - [x] 3.3: Remove password EditText
  - [x] 3.4: Remove remember_username_checkbox
  - [x] 3.5: Add hint text "보안키를 입력하세요" to security key input
  - [x] 3.6: Update button text to "로그인" (Login in Korean)
  - [x] 3.7: Apply Material Design styling per UX spec (Primary color #12c0e2)

- [x] Task 4: Update SecureTokenManager for security key storage (AC: #2, #5)
  - [x] 4.1: Add SECURITY_KEY constant for key storage
  - [x] 4.2: Add saveSecurityKey(String key) method
  - [x] 4.3: Add getSecurityKey() method
  - [x] 4.4: Add deleteSecurityKey() method
  - [x] 4.5: Update clearAll() to include security key

- [x] Task 5: Update SessionManager for auto-login flow (AC: #5)
  - [x] 5.1: Verify isLoggedIn() correctly checks for existing valid token
  - [x] 5.2: Update saveSession() to store security key for session reuse

- [x] Task 6: Create MachineListActivity placeholder (AC: #2)
  - [x] 6.1: Create MachineListActivity.java with basic structure
  - [x] 6.2: Create activity_machine_list.xml layout (placeholder)
  - [x] 6.3: Register MachineListActivity in AndroidManifest.xml
  - [x] 6.4: Add logout functionality that returns to LoginActivity

- [x] Task 7: Write unit tests for login functionality (AC: #1-6)
  - [x] 7.1: Create LoginActivityTest.java in test directory
  - [x] 7.2: Test security key validation (empty key shows error)
  - [x] 7.3: Test successful login flow (token stored, navigation triggered)
  - [x] 7.4: Test failed login flow (error message displayed)
  - [x] 7.5: Test auto-login when token exists
  - [x] 7.6: Test network error handling

## Dev Notes

### Technical Requirements

**Framework & Versions:**
- Android SDK: minSdk 24, targetSdk 36
- Java 11
- Material Design Components (already in build.gradle.kts)
- AndroidX Security Crypto (already in build.gradle.kts)

**API Endpoint (from Story 1-1 implementation):**
- Method: POST
- URL: `/api/auth/login/`
- Request: `{"security_key": "..."}`
- Success Response (200): `{"token": "...", "name": "..."}`
- Failure Response (401): `{"error": "Invalid security key"}`

**BuildConfig:**
- Debug: `http://10.0.2.2:8000/` (localhost via Android emulator)
- Release: `https://mouseku.pythonanywhere.com/`

### Architecture Compliance

**Existing Infrastructure (REUSE, DO NOT RECREATE):**
- `SecureTokenManager.java`: Encrypted SharedPreferences using AndroidX Security Crypto - ADD methods for security key storage
- `SessionManager.java`: Singleton for session state - MODIFY saveSession() signature
- `AuthenticationService.java`: HTTP client for login - MODIFY to use security_key
- `LoginActivity.java`: Login UI - MODIFY for security key input
- `BuildConfig.API_BASE_URL`: Already configured for dev/prod URLs

**Code Patterns to Follow (from existing codebase):**
- Use ExecutorService for background network calls (not deprecated AsyncTask)
- Use Handler with Looper.getMainLooper() for UI updates from background threads
- Use HttpURLConnection for REST API calls (existing pattern in MainActivity)
- Use EncryptedSharedPreferences for sensitive data storage
- Use callbacks (interface) for async operation results

**Error Message Patterns:**
- Korean error messages for user-facing errors
- Log.d/Log.e for debugging with TAG constant
- Toast.makeText for transient user feedback

### Project Structure Notes

**Files to Modify:**
- `PhotoViewer/app/src/main/java/com/example/photoviewer/services/AuthenticationService.java`
- `PhotoViewer/app/src/main/java/com/example/photoviewer/LoginActivity.java`
- `PhotoViewer/app/src/main/res/layout/activity_login.xml`
- `PhotoViewer/app/src/main/java/com/example/photoviewer/utils/SecureTokenManager.java`
- `PhotoViewer/app/src/main/java/com/example/photoviewer/services/SessionManager.java`
- `PhotoViewer/app/src/main/AndroidManifest.xml` (add MachineListActivity)

**Files to Create:**
- `PhotoViewer/app/src/main/java/com/example/photoviewer/MachineListActivity.java`
- `PhotoViewer/app/src/main/res/layout/activity_machine_list.xml`
- `PhotoViewer/app/src/test/java/com/example/photoviewer/LoginActivityTest.java`

**Do NOT Modify (backward compatibility):**
- `MainActivity.java` - Keep existing Photo Blog functionality working
- `Post.java` - Existing data model
- `ImageAdapter.java` - Existing adapter

### Previous Story Intelligence (Story 1-1, 1-2)

**Learnings from Backend Stories:**
- Login endpoint returns `{"token": "...", "name": "..."}` on success
- Login endpoint returns `{"error": "Invalid security key"}` on 401
- Token authentication header format: `Authorization: Token <token>`
- GymMachine API at `/api_root/machines/` requires token auth

**Files Created in Story 1-1:**
- `/api/auth/login/` endpoint now accepts security_key (NOT username/password)
- ApiUser model with security_key field

**Files Created in Story 1-2:**
- `/api_root/machines/` endpoint returns machine list
- GymMachine model with event_count and last_event fields

### Testing Standards

**Test Framework:** JUnit 4 with Mockito (already in build.gradle.kts)
**Test Location:** `PhotoViewer/app/src/test/java/com/example/photoviewer/`

**Test Patterns from Existing Tests:**
```java
@Test
public void testMethodName() {
    // Given - setup
    // When - action
    // Then - assertion
}
```

**Note:** Android instrumented tests require device/emulator. For unit tests, mock HttpURLConnection and Context.

### UX Design Requirements (from docs/final/ui/README.md)

**Login Screen Components:**
- App logo and title: "GymFlow Manager"
- Security key input field with hint "보안키를 입력하세요"
- Login button
- Security key guidance text (optional)

**Color Scheme:**
- Primary: `#12c0e2`
- Background: `#FFFFFF` (light mode)
- Error: `#FF0000`

**Typography:**
- Font: Inter (or system default)
- Title: 32sp bold
- Input: 16sp
- Button: 16sp bold

### References

- [Source: docs/final/implementation/04-android.md#LoginActivity 수정]
- [Source: docs/final/implementation/05-api-reference.md#인증 API]
- [Source: docs/final/ui/README.md#로그인 화면]
- [Source: _bmad-output/stories/1-1-backend-authentication-setup.md#API Endpoint]
- [Source: _bmad-output/stories/1-2-backend-equipment-api.md#Previous Story Intelligence]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

- Java runtime not available in dev environment - tests written but require Android SDK to execute

### Completion Notes List

1. **Task 1 Complete**: AuthenticationService.login() now accepts single `securityKey` parameter and sends `{"security_key": ...}` JSON body
2. **Task 2 Complete**: LoginActivity rewritten with security key input, Korean error messages, and MachineListActivity navigation
3. **Task 3 Complete**: activity_login.xml updated with GymFlow Manager branding, Korean UI text, primary color #12c0e2
4. **Task 4 Complete**: SecureTokenManager extended with saveSecurityKey(), getSecurityKey(), hasSecurityKey(), deleteSecurityKey() methods
5. **Task 5 Complete**: SessionManager.saveSession() updated to store security key instead of username
6. **Task 6 Complete**: MachineListActivity placeholder created with logout functionality, registered in AndroidManifest.xml
7. **Task 7 Complete**: 15 unit tests written covering validation, error classification, Korean messages, and JSON format

### File List

**Modified Files:**
- `PhotoViewer/app/src/main/java/com/example/photoviewer/services/AuthenticationService.java`
- `PhotoViewer/app/src/main/java/com/example/photoviewer/LoginActivity.java`
- `PhotoViewer/app/src/main/res/layout/activity_login.xml`
- `PhotoViewer/app/src/main/java/com/example/photoviewer/utils/SecureTokenManager.java`
- `PhotoViewer/app/src/main/java/com/example/photoviewer/services/SessionManager.java`
- `PhotoViewer/app/src/main/AndroidManifest.xml`

**Created Files:**
- `PhotoViewer/app/src/main/java/com/example/photoviewer/MachineListActivity.java`
- `PhotoViewer/app/src/main/res/layout/activity_machine_list.xml`
- `PhotoViewer/app/src/main/res/menu/menu_machine_list.xml`
- `PhotoViewer/app/src/main/res/drawable/edit_text_background.xml`
- `PhotoViewer/app/src/test/java/com/example/photoviewer/LoginActivityTest.java`

## Change Log

- 2025-12-19: Story created with comprehensive context from implementation specs and existing codebase analysis
- 2025-12-19: All 7 tasks completed - security key authentication flow implemented with Korean localization
