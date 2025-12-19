# Story 2.4: Edge Event Upload Integration

Status: ready-for-dev

## Story

As an **Edge system**,
I want **to authenticate with the server and upload detected events via REST API**,
so that **usage events are stored in the central system in real-time**.

## Acceptance Criteria

1. **Given** the Edge system starts with a configured security_key **When** the ChangeDetection class initializes **Then** it authenticates via POST `/api/auth/login/` and stores the returned token

2. **Given** authentication fails (invalid security_key) **When** the ChangeDetection class initializes **Then** an error is logged and the system raises an exception

3. **Given** a change event is detected **When** the event is processed **Then** the system POSTs to `/api/machines/{machine_id}/events/` with Authorization header, image file, event_type, captured_at, person_count, detections JSON, change_info JSON

4. **Given** the event upload succeeds (HTTP 201) **When** the response is received **Then** a success message is logged with event_type

5. **Given** the event upload fails (HTTP 4xx/5xx) **When** the response is received **Then** an error message is logged with status code

6. **Given** configuration is needed **When** the Edge system loads **Then** it reads host, security_key, machine_id, target_class from environment variables or config file

7. **Given** the server is configured **When** running detect.py **Then** the system connects to the configured host (default: https://mouseku.pythonanywhere.com)

## Tasks / Subtasks

- [ ] Task 1: Implement authentication in ChangeDetection (AC: #1, #2)
  - [ ] 1.1: Add HOST, SECURITY_KEY, MACHINE_ID to config initialization
  - [ ] 1.2: Implement `_authenticate()` method
  - [ ] 1.3: POST to `/api/auth/login/` with security_key
  - [ ] 1.4: Store returned token in self.token
  - [ ] 1.5: Log success message on successful auth
  - [ ] 1.6: Raise exception on auth failure (invalid key)
  - [ ] 1.7: Call `_authenticate()` in `__init__`

- [ ] Task 2: Implement event upload method (AC: #3, #4, #5)
  - [ ] 2.1: Implement `_send_event(event_type, image, person_count, detections, change_info)` method
  - [ ] 2.2: Construct multipart form data with image file
  - [ ] 2.3: Include event_type, captured_at (ISO format), person_count
  - [ ] 2.4: Include detections and change_info as JSON strings
  - [ ] 2.5: Set Authorization header with Token
  - [ ] 2.6: POST to `/api/machines/{machine_id}/events/`
  - [ ] 2.7: Log success message on HTTP 201
  - [ ] 2.8: Log error message on HTTP 4xx/5xx with status code

- [ ] Task 3: Integrate with detect_changes method (AC: #3)
  - [ ] 3.1: Call `_send_event()` when change detected
  - [ ] 3.2: Pass current image, person_count, detections, change_info
  - [ ] 3.3: Save image locally before upload (for retry/debugging)

- [ ] Task 4: Implement configuration loading (AC: #6, #7)
  - [ ] 4.1: Create `yolov5/config/gym_detection.yaml` template
  - [ ] 4.2: Support environment variables: GYM_HOST, GYM_SECURITY_KEY, GYM_MACHINE_ID
  - [ ] 4.3: Set default host to https://mouseku.pythonanywhere.com
  - [ ] 4.4: Document configuration options

- [ ] Task 5: Modify detect.py integration (AC: #6, #7)
  - [ ] 5.1: Import ChangeDetection class
  - [ ] 5.2: Initialize with config from env vars
  - [ ] 5.3: Call detect_changes in frame processing loop
  - [ ] 5.4: Handle initialization errors gracefully

- [ ] Task 6: Write integration tests (AC: #1-#5)
  - [ ] 6.1: Test successful authentication stores token
  - [ ] 6.2: Test failed authentication raises exception
  - [ ] 6.3: Test event upload sends correct data format
  - [ ] 6.4: Test success logging on 201 response
  - [ ] 6.5: Test error logging on 4xx/5xx response
  - [ ] 6.6: Mock server responses for isolated testing

## Dev Notes

### Technical Requirements

**Framework & Libraries:**
- Python 3.x
- requests library for HTTP calls
- json for serialization
- os for environment variables
- yaml (optional) for config file support

**API Endpoints:**
- Authentication: `POST {HOST}/api/auth/login/`
  - Request: `{"security_key": "..."}`
  - Response: `{"token": "...", "name": "..."}`
- Event Upload: `POST {HOST}/api/machines/{machine_id}/events/`
  - Headers: `Authorization: Token {token}`
  - Content-Type: multipart/form-data
  - Fields: image (file), event_type, captured_at, person_count, detections (JSON), change_info (JSON)

**Configuration Options:**
| Config Key | Env Variable | Default | Description |
|------------|--------------|---------|-------------|
| host | GYM_HOST | https://mouseku.pythonanywhere.com | API server URL |
| security_key | GYM_SECURITY_KEY | (required) | Authentication key |
| machine_id | GYM_MACHINE_ID | 1 | Target machine ID |
| target_class | GYM_TARGET_CLASS | person | Detection target |

**Implementation (from docs/final/implementation/03-edge-system.md):**
```python
def _authenticate(self):
    """보안키로 인증"""
    try:
        res = requests.post(
            f'{self.HOST}/api/auth/login/',
            json={'security_key': self.SECURITY_KEY}
        )
        res.raise_for_status()
        self.token = res.json()['token']
        print(f"[ChangeDetection] Authenticated successfully")
    except Exception as e:
        print(f"[ChangeDetection] Auth failed: {e}")
        raise

def _send_event(self, event_type, image, person_count, detections, change_info):
    """서버로 이벤트 전송"""
    headers = {'Authorization': f'Token {self.token}'}

    data = {
        'event_type': event_type,
        'captured_at': datetime.now().isoformat(),
        'person_count': person_count,
        'detections': json.dumps(detections),
        'change_info': json.dumps(change_info)
    }

    files = {'image': open(filepath, 'rb')}

    res = requests.post(
        f'{self.HOST}/api/machines/{self.MACHINE_ID}/events/',
        data=data,
        files=files,
        headers=headers
    )

    if res.status_code in [200, 201]:
        print(f"[ChangeDetection] Event sent: {event_type}")
    else:
        print(f"[ChangeDetection] Send failed: {res.status_code}")
```

### Architecture Compliance

**Files to Modify:**
- `yolov5/changedetection.py` - Add authentication and upload methods
- `yolov5/detect.py` - Integrate ChangeDetection initialization

**Files to Create:**
- `yolov5/config/gym_detection.yaml` - Configuration template

**Dependencies:**
- Story 2.3: ChangeDetection class and detect_changes method
- Story 2.1: Backend event posting API

### Testing Standards

**Test Framework:** Python unittest with unittest.mock
**Test Location:** `yolov5/tests/test_changedetection.py`

**Additional Test Cases:**
1. _authenticate() success stores token
2. _authenticate() failure raises exception
3. _send_event() constructs correct request
4. _send_event() logs success on 201
5. _send_event() logs error on 4xx
6. _send_event() logs error on 5xx
7. Config loading from environment variables
8. Config loading with defaults

**Mocking Strategy:**
- Mock requests.post for isolated testing
- Mock responses for auth success/failure
- Mock responses for upload success/failure

**Commands:**
```bash
cd yolov5
python -m pytest tests/test_changedetection.py -v
```

### References

- [Source: docs/final/implementation/03-edge-system.md#ChangeDetection 클래스 수정]
- [Source: docs/final/implementation/05-api-reference.md#인증 API]
- [Source: docs/final/implementation/05-api-reference.md#이벤트 API]
- [Source: _bmad-output/stories/epic-2-stories.md#Story 2.4]
- [Dependency: Story 2.1 (Backend Event API)]
- [Dependency: Story 2.3 (ChangeDetection base class)]

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
