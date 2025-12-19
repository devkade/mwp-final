# Story 1.2: Backend Equipment API

Status: review

## Story

As a **gym manager**,
I want **to retrieve a list of all gym equipment via REST API**,
so that **I can see what equipment is available in the facility**.

## Acceptance Criteria

1. **Given** the Django server is running and I have a valid auth token **When** I GET `/api_root/machines/` with header `Authorization: Token <token>` **Then** I receive HTTP 200 with a JSON array of GymMachine objects

2. **Given** GymMachine objects exist in the database **When** I GET `/api_root/machines/` **Then** each object includes: id, name, machine_type, location, description, thumbnail, is_active, event_count, last_event

3. **Given** a GymMachine has is_active=False **When** I GET `/api_root/machines/` **Then** that machine is NOT included in the response (only active machines returned)

4. **Given** I have no valid auth token **When** I GET `/api_root/machines/` without Authorization header **Then** I receive HTTP 401 Unauthorized

5. **Given** multiple machines exist **When** I GET `/api_root/machines/` **Then** machines are ordered by location, then by name

6. **Given** GymMachine model is created **When** I access Django admin at `/admin/` **Then** I can create, edit, and delete GymMachine entries with all fields (name, machine_type choices, location, description, thumbnail, is_active)

## Tasks / Subtasks

- [x] Task 1: Create GymMachine model (AC: #2, #5, #6)
  - [x] 1.1: Add GymMachine class to blog/models.py with MACHINE_TYPES choices
  - [x] 1.2: Add fields: name, machine_type, location, description, thumbnail, is_active, created_at
  - [x] 1.3: Add Meta class with ordering = ['location', 'name']
  - [x] 1.4: Add __str__ method returning `{name} ({location})`
  - [x] 1.5: Run makemigrations and migrate

- [x] Task 2: Register GymMachine in Django Admin (AC: #6)
  - [x] 2.1: Add GymMachineAdmin class in blog/admin.py with list_display, search_fields, list_filter
  - [x] 2.2: Include machine_type filter and location filter

- [x] Task 3: Create GymMachineSerializer (AC: #2)
  - [x] 3.1: Add GymMachineSerializer to blog/serializers.py
  - [x] 3.2: Add event_count SerializerMethodField
  - [x] 3.3: Add last_event SerializerMethodField with event_type and captured_at
  - [x] 3.4: Define all fields in Meta class

- [x] Task 4: Create GymMachineViewSet (AC: #1, #3, #4, #5)
  - [x] 4.1: Add GymMachineViewSet to blog/views.py
  - [x] 4.2: Filter queryset to is_active=True only
  - [x] 4.3: Requires token authentication (added explicit IsAuthenticated permission)

- [x] Task 5: Register URL route (AC: #1)
  - [x] 5.1: Register 'machines' route in router in mysite/urls.py

- [x] Task 6: Write tests for equipment API (AC: #1, #2, #3, #4, #5)
  - [x] 6.1: Create blog/tests/test_machines.py
  - [x] 6.2: Test successful machine list with valid token
  - [x] 6.3: Test machine list fields (id, name, machine_type, location, description, thumbnail, is_active, event_count, last_event)
  - [x] 6.4: Test inactive machines excluded from response
  - [x] 6.5: Test 401 response without auth token
  - [x] 6.6: Test ordering by location then name

## Dev Notes

### Technical Requirements

**Framework & Versions:**
- Django 5.2.6
- Django REST Framework (already installed)
- rest_framework.authtoken (already configured)

**Model Specification (from docs/final/implementation/02-backend.md):**
```python
class GymMachine(models.Model):
    """운동기구 정보"""
    MACHINE_TYPES = [
        ('treadmill', '런닝머신'),
        ('bench_press', '벤치프레스'),
        ('squat_rack', '스쿼트랙'),
        ('lat_pulldown', '랫풀다운'),
        ('leg_press', '레그프레스'),
        ('cable_machine', '케이블머신'),
        ('dumbbell', '덤벨존'),
        ('other', '기타'),
    ]

    name = models.CharField(max_length=100)
    machine_type = models.CharField(max_length=20, choices=MACHINE_TYPES)
    location = models.CharField(max_length=100, help_text="예: 1층 A구역")
    description = models.TextField(blank=True)
    thumbnail = models.ImageField(
        upload_to='machines/',
        blank=True,
        null=True
    )
    is_active = models.BooleanField(default=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['location', 'name']

    def __str__(self):
        return f"{self.name} ({self.location})"
```

**Serializer Specification (from docs/final/implementation/02-backend.md):**
```python
class GymMachineSerializer(serializers.ModelSerializer):
    event_count = serializers.SerializerMethodField()
    last_event = serializers.SerializerMethodField()

    class Meta:
        model = GymMachine
        fields = [
            'id', 'name', 'machine_type', 'location',
            'description', 'thumbnail', 'is_active',
            'event_count', 'last_event'
        ]

    def get_event_count(self, obj):
        return obj.events.count()

    def get_last_event(self, obj):
        last = obj.events.first()
        if last:
            return {
                'event_type': last.event_type,
                'captured_at': last.captured_at.isoformat()
            }
        return None
```

**Note:** The `events` related_name comes from MachineEvent.machine FK (Epic 2). For now, the serializer methods will return 0 and None since MachineEvent doesn't exist yet. This is correct behavior.

**ViewSet Specification (from docs/final/implementation/02-backend.md):**
```python
class GymMachineViewSet(viewsets.ModelViewSet):
    """운동기구 ViewSet"""
    queryset = GymMachine.objects.filter(is_active=True)
    serializer_class = GymMachineSerializer
```

**API Endpoint (from docs/final/implementation/05-api-reference.md):**
- Method: GET
- URL: `/api_root/machines/`
- Headers: `Authorization: Token <token>`
- Response (200):
```json
[
  {
    "id": 1,
    "name": "런닝머신 #1",
    "machine_type": "treadmill",
    "location": "1층 A구역",
    "description": "ProForm 상업용 런닝머신",
    "thumbnail": "/media/machines/treadmill1.jpg",
    "is_active": true,
    "event_count": 0,
    "last_event": null
  }
]
```

### Architecture Compliance

**Existing Infrastructure (DO NOT modify unless required):**
- `mysite/urls.py`: Already has router at line 25-26 - ADD 'machines' route
- Token authentication already configured in settings.py REST_FRAMEWORK
- Default permission is `IsAuthenticated` from REST_FRAMEWORK settings

**Files to Create:**
- `blog/tests/test_machines.py` (new test file for machine API tests)

**Files to Modify:**
- `blog/models.py`: Add GymMachine model (after ApiUser)
- `blog/admin.py`: Register GymMachine with GymMachineAdmin
- `blog/serializers.py`: Add GymMachineSerializer
- `blog/views.py`: Add GymMachineViewSet
- `mysite/urls.py`: Register 'machines' route in router

**Code Patterns to Follow (from Story 1-1):**
- Use `viewsets.ModelViewSet` (same as BlogImages)
- Use `serializers.ModelSerializer` with explicit `fields` list
- Admin class pattern with list_display, search_fields, list_filter (same as ApiUserAdmin)
- Tests in `blog/tests/` directory using Django TestCase with DRF's APIClient

**Database:**
- SQLite3 at `PhotoBlogServer/db.sqlite3`
- Run migrations from `PhotoBlogServer/` directory

### Previous Story Intelligence (Story 1-1)

**Learnings Applied:**
- Tests should be in `blog/tests/` directory structure (not blog/tests.py which was removed)
- Import Token from `rest_framework.authtoken.models`
- Use `APIClient` from `rest_framework.test` for API tests
- Create test user with `User.objects.create_user()` and ApiUser linked to it
- Get token with `Token.objects.get_or_create(user=user)`

**Files Created in Story 1-1:**
- `blog/tests/__init__.py` - Package init
- `blog/tests/test_auth.py` - Authentication tests
- `blog/migrations/0003_apiuser.py` - ApiUser migration

### Testing Standards

**Test Framework:** Django's built-in TestCase with DRF's APIClient
**Test Location:** `blog/tests/test_machines.py`

**Required Test Cases:**
1. GymMachine model creation with all fields
2. Machine list success with valid token → 200 + JSON array
3. Machine list includes all required fields (event_count=0, last_event=null for now)
4. Inactive machines filtered out (is_active=False not in response)
5. No auth token → 401 Unauthorized
6. Ordering verification: location then name

**Test Setup Pattern (from test_auth.py):**
```python
from django.test import TestCase
from django.contrib.auth.models import User
from rest_framework.test import APIClient
from rest_framework.authtoken.models import Token
from blog.models import GymMachine, ApiUser

class GymMachineAPITestCase(TestCase):
    def setUp(self):
        self.client = APIClient()
        # Create user and get token for authenticated requests
        self.user = User.objects.create_user(username='testuser', password='testpass')
        self.api_user = ApiUser.objects.create(
            name='Test API User',
            security_key='test-key-123',
            user=self.user,
            is_active=True
        )
        self.token, _ = Token.objects.get_or_create(user=self.user)
```

**Commands:**
```bash
cd PhotoBlogServer
python manage.py test blog.tests.test_machines
```

### Project Structure Notes

**Alignment with Unified Project Structure:**
- Models in `blog/models.py` - following existing pattern
- Serializers in `blog/serializers.py` - following existing pattern
- Views in `blog/views.py` - following existing pattern
- URL routes in `mysite/urls.py` via router - following existing pattern
- Admin in `blog/admin.py` - following existing pattern

**Media Files:**
- Thumbnails stored in `media/machines/` directory
- MEDIA_ROOT already configured in settings.py

### References

- [Source: docs/final/implementation/02-backend.md#GymMachine 모델]
- [Source: docs/final/implementation/02-backend.md#Serializers]
- [Source: docs/final/implementation/02-backend.md#Views]
- [Source: docs/final/implementation/05-api-reference.md#운동기구 API]
- [Source: _bmad-output/stories/epic-1-stories.md#Story 1.2]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

- Fixed 401 test failure: Default REST_FRAMEWORK permission was `IsAuthenticatedOrReadOnly`, added explicit `permission_classes = [IsAuthenticated]` to GymMachineViewSet
- Moved blog/tests.py to blog/tests/test_posts.py to resolve test discovery conflict
- Fixed import in test_posts.py: changed `.models` to `blog.models`

### Completion Notes List

- Created GymMachine model with 8 MACHINE_TYPES choices (treadmill, bench_press, squat_rack, lat_pulldown, leg_press, cable_machine, dumbbell, other)
- Model includes all required fields with proper defaults and ordering
- Registered GymMachineAdmin with list_display, search_fields, and filters for machine_type, location, is_active
- Created GymMachineSerializer with computed event_count and last_event fields (return 0/null until MachineEvent is added in Epic 2)
- Created GymMachineViewSet with is_active=True filter and IsAuthenticated permission
- Registered 'machines' route in DefaultRouter
- Created comprehensive test suite with 12 tests covering all 6 acceptance criteria
- All 35 blog tests pass (including existing auth and post tests)

### File List

**Created:**
- `PhotoBlogServer/blog/tests/test_machines.py`
- `PhotoBlogServer/blog/migrations/0004_gymmachine.py`

**Modified:**
- `PhotoBlogServer/blog/models.py` - Added GymMachine model
- `PhotoBlogServer/blog/admin.py` - Added GymMachineAdmin and registered GymMachine
- `PhotoBlogServer/blog/serializers.py` - Added GymMachineSerializer
- `PhotoBlogServer/blog/views.py` - Added GymMachineViewSet with IsAuthenticated permission
- `PhotoBlogServer/mysite/urls.py` - Registered 'machines' route in router
- `PhotoBlogServer/blog/tests/test_posts.py` - Fixed import path (moved from blog/tests.py)

## Change Log

- 2025-12-19: Story created with comprehensive context from implementation specs
- 2025-12-19: Implementation completed - all 6 tasks done, 12 new tests passing, 35 total tests passing
