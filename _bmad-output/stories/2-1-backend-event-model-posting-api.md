# Story 2.1: Backend Event Model & Posting API

Status: in-progress

## Story

As an **Edge device**,
I want **to post usage events (start/end) with images to the server**,
so that **equipment usage is automatically logged in the system**.

## Acceptance Criteria

1. **Given** the Django server is running with MachineEvent model **When** I POST to `/api/machines/{machine_id}/events/` with multipart form data containing image, event_type, captured_at, person_count, detections JSON, change_info JSON **Then** a new MachineEvent is created and I receive HTTP 201 with the event data

2. **Given** I POST an event with event_type="start" **When** the request is processed **Then** the event is stored with event_type="start" and person_count >= 1

3. **Given** I POST an event with event_type="end" **When** the request is processed **Then** the event is stored with event_type="end" and person_count = 0

4. **Given** the referenced machine_id does not exist **When** I POST an event **Then** I receive HTTP 404 Not Found

5. **Given** I have no valid auth token **When** I POST an event **Then** I receive HTTP 401 Unauthorized

6. **Given** MachineEvent model is created **When** I access Django admin **Then** I can view all events with image previews and filter by machine, event_type, date

## Tasks / Subtasks

- [x] Task 1: Create MachineEvent model (AC: #1, #6)
  - [x] 1.1: Add MachineEvent class to blog/models.py with fields: machine (FK), event_type (choices), image, captured_at, created_at, person_count, detections (JSONField), change_info (JSONField)
  - [x] 1.2: Add EVENT_TYPES choices: ('start', '사용 시작'), ('end', '사용 종료')
  - [x] 1.3: Add Meta class with ordering=['-captured_at'] and indexes on (machine, -captured_at) and (event_type, -captured_at)
  - [x] 1.4: Add __str__ method returning `{machine.name} - {event_type_display} ({captured_at})`
  - [ ] 1.5: Run makemigrations and migrate (requires venv activation)

- [x] Task 2: Register MachineEvent in Django Admin (AC: #6)
  - [x] 2.1: Add MachineEventAdmin class in blog/admin.py with list_display, list_filter, search_fields, date_hierarchy
  - [x] 2.2: Configure readonly_fields for created_at
  - [x] 2.3: Register MachineEvent with admin.site.register

- [x] Task 3: Create MachineEvent Serializers (AC: #1)
  - [x] 3.1: Add MachineEventSerializer to blog/serializers.py with all fields
  - [x] 3.2: Add machine_name (read_only) and event_type_display (read_only) computed fields
  - [x] 3.3: Add MachineEventCreateSerializer for POST requests with machine auto-set from URL

- [x] Task 4: Implement event posting endpoint (AC: #1, #2, #3, #4, #5)
  - [x] 4.1: Create MachineEventViewSet in blog/views.py with create action
  - [x] 4.2: Add custom create logic to set machine from URL parameter (machine_id)
  - [x] 4.3: Add URL pattern for `/api/machines/{machine_id}/events/` with POST method
  - [x] 4.4: Return HTTP 201 on success with serialized event data
  - [x] 4.5: Return HTTP 404 if machine_id not found
  - [x] 4.6: Ensure IsAuthenticated permission class is applied

- [x] Task 5: Write tests for MachineEvent model and API (AC: #1-#5)
  - [x] 5.1: Create blog/tests/test_events.py
  - [x] 5.2: Test MachineEvent model creation with all fields
  - [x] 5.3: Test successful event POST with valid token and existing machine (201)
  - [x] 5.4: Test event POST with event_type="start" stores correctly
  - [x] 5.5: Test event POST with event_type="end" stores correctly
  - [x] 5.6: Test event POST with non-existent machine_id (404)
  - [x] 5.7: Test event POST without auth token (401)

## Dev Notes

### Technical Requirements

**Framework & Versions:**
- Django 5.2.6
- Django REST Framework (already installed)
- rest_framework.authtoken (already configured)

**Model Specifications (from docs/final/implementation/02-backend.md):**
```python
class MachineEvent(models.Model):
    """운동기구 사용 이벤트"""
    EVENT_TYPES = [
        ('start', '사용 시작'),
        ('end', '사용 종료'),
    ]

    machine = models.ForeignKey(
        GymMachine,
        on_delete=models.CASCADE,
        related_name='events'
    )
    event_type = models.CharField(max_length=10, choices=EVENT_TYPES)
    image = models.ImageField(upload_to='events/%Y/%m/%d/')
    captured_at = models.DateTimeField(help_text="Edge에서 캡처한 시각")
    created_at = models.DateTimeField(auto_now_add=True)

    # YOLO 검출 결과
    person_count = models.IntegerField(default=0)
    detections = models.JSONField(
        default=dict,
        help_text="YOLO 검출 결과 JSON"
    )
    change_info = models.JSONField(
        default=dict,
        help_text="Change Detection 결과 JSON"
    )

    class Meta:
        ordering = ['-captured_at']
        indexes = [
            models.Index(fields=['machine', '-captured_at']),
            models.Index(fields=['event_type', '-captured_at']),
        ]

    def __str__(self):
        return f"{self.machine.name} - {self.get_event_type_display()} ({self.captured_at})"
```

**API Endpoint (from docs/final/implementation/05-api-reference.md):**
- Method: POST
- URL: `/api/machines/{machine_id}/events/`
- Content-Type: multipart/form-data
- Request Fields:
  - image (file): Captured image
  - event_type (string): "start" or "end"
  - captured_at (datetime): ISO format timestamp
  - person_count (integer): Number of persons detected
  - detections (JSON string): YOLO detection results
  - change_info (JSON string): Change detection metadata
- Success Response (201): Event data JSON
- Failure Responses:
  - 401: Unauthorized (no/invalid token)
  - 404: Machine not found

### Architecture Compliance

**Existing Infrastructure:**
- `blog/models.py`: Has Post, ApiUser, GymMachine - ADD MachineEvent here
- `blog/serializers.py`: Has PostSerializer, SecurityKeyLoginSerializer, GymMachineSerializer - ADD MachineEventSerializer
- `blog/views.py`: Has BlogImages, GymMachineViewSet - ADD MachineEventViewSet
- `mysite/urls.py`: Router has Post, machines - ADD events route AND custom machine events URL

**Code Patterns to Follow:**
- Use ModelViewSet for CRUD operations (existing pattern from GymMachineViewSet)
- Use `@permission_classes([IsAuthenticated])` for protected endpoints
- Use `get_object_or_404` for machine lookup from URL
- Follow existing serializer patterns with read_only computed fields

### Project Structure Notes

**Files to Create:**
- `blog/tests/test_events.py` (new test file for event tests)
- `blog/migrations/XXXX_machineevent.py` (auto-generated)

**Files to Modify:**
- `blog/models.py`: Add MachineEvent model
- `blog/admin.py`: Register MachineEvent
- `blog/serializers.py`: Add MachineEventSerializer, MachineEventCreateSerializer
- `blog/views.py`: Add MachineEventViewSet and machine_events view
- `mysite/urls.py`: Add events to router and machine-specific events URL

**Database:**
- SQLite3 at `PhotoBlogServer/db.sqlite3`
- Run migrations from `PhotoBlogServer/` directory

### Testing Standards

**Test Framework:** Django's built-in TestCase with DRF's APIClient
**Test Location:** `blog/tests/test_events.py`

**Required Test Cases:**
1. MachineEvent model creation with all required fields
2. Event POST success with valid token and machine → 201 + event data
3. Event POST with event_type="start" stores correctly
4. Event POST with event_type="end" stores correctly
5. Event POST with non-existent machine → 404
6. Event POST without auth token → 401

**Test Utilities to Create:**
- MachineEventFactory or create_test_event helper
- create_test_image helper (can reuse from test_posts.py)

**Commands:**
```bash
cd PhotoBlogServer
python manage.py test blog.tests.test_events
```

### References

- [Source: docs/final/implementation/02-backend.md#MachineEvent 모델]
- [Source: docs/final/implementation/05-api-reference.md#이벤트 API]
- [Source: _bmad-output/stories/epic-2-stories.md#Story 2.1]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

- Migration could not be run automatically (no virtual environment found in project directory)
- User needs to run: `cd PhotoBlogServer && python manage.py makemigrations blog --name machineevent && python manage.py migrate`

### Completion Notes List

- Created MachineEvent model with all required fields (machine FK, event_type choices, image, captured_at, created_at, person_count, detections JSON, change_info JSON)
- Added EVENT_TYPES choices: ('start', '사용 시작'), ('end', '사용 종료')
- Added Meta class with ordering and database indexes for efficient queries
- Added __str__ method with proper display format
- Registered MachineEvent in Django admin with image preview, list filters, date hierarchy
- Created MachineEventSerializer (full), MachineEventListSerializer (lightweight), MachineEventCreateSerializer (for POST)
- Added JSON string parsing support in MachineEventCreateSerializer for multipart form data
- Created MachineEventViewSet for /api_root/events/ endpoint
- Created machine_events view for /api/machines/{machine_id}/events/ endpoint (GET list + POST create)
- Registered events router in urls.py
- Created comprehensive test suite with 18 test cases covering all ACs

### File List

**Created:**
- `PhotoBlogServer/blog/tests/test_events.py` - 18 test cases for MachineEvent model and API

**Modified:**
- `PhotoBlogServer/blog/models.py` - Added MachineEvent model (lines 70-106)
- `PhotoBlogServer/blog/admin.py` - Added MachineEventAdmin with image preview
- `PhotoBlogServer/blog/serializers.py` - Added MachineEventSerializer, MachineEventListSerializer, MachineEventCreateSerializer
- `PhotoBlogServer/blog/views.py` - Added MachineEventViewSet and machine_events view
- `PhotoBlogServer/mysite/urls.py` - Added events router and machine-events URL

**Deleted:**
(None)

## Change Log

- 2025-12-19: Story created with comprehensive context from implementation specs
- 2025-12-19: Implementation completed - all 5 tasks done (migration pending venv activation)
