# Story 3.1: Backend Statistics API

Status: done

## Story

As a **gym manager**,
I want **to retrieve usage statistics for equipment via API**,
so that **I can analyze usage patterns and make operational decisions**.

## Acceptance Criteria

1. **Given** I have a valid auth token **When** I GET `/api_root/machines/{machine_id}/stats/` **Then** I receive HTTP 200 with statistics JSON

2. **Given** events exist for a machine **When** I GET the stats endpoint **Then** I receive: machine_id, machine_name, total_starts, total_ends, daily_usage array

3. **Given** I request stats with date range **When** I GET `/api_root/machines/{machine_id}/stats/?date_from=2024-01-01&date_to=2024-01-31` **Then** statistics are calculated only for events within that date range

4. **Given** daily_usage is returned **When** I view the array **Then** each item contains: date (YYYY-MM-DD), count (number of start events)

5. **Given** no events exist for a machine **When** I GET the stats endpoint **Then** I receive: total_starts=0, total_ends=0, daily_usage=[]

6. **Given** I request stats for a non-existent machine **When** I GET the stats endpoint **Then** I receive HTTP 404 Not Found

## Tasks / Subtasks

- [x] Task 1: Add stats action to GymMachineViewSet (AC: #1, #2)
  - [x] 1.1: Add `@action(detail=True, methods=['get'])` decorator for stats method
  - [x] 1.2: Implement basic stats query returning machine_id, machine_name
  - [x] 1.3: Add total_starts count (filter event_type='start')
  - [x] 1.4: Add total_ends count (filter event_type='end')

- [x] Task 2: Implement daily_usage aggregation (AC: #4)
  - [x] 2.1: Use TruncDate to group events by date
  - [x] 2.2: Filter for start events only
  - [x] 2.3: Annotate with Count('id') for daily count
  - [x] 2.4: Order by date ascending

- [x] Task 3: Implement date range filtering (AC: #3)
  - [x] 3.1: Parse date_from query param and filter captured_at__date__gte
  - [x] 3.2: Parse date_to query param and filter captured_at__date__lte
  - [x] 3.3: Apply filters to all statistics calculations

- [x] Task 4: Handle edge cases (AC: #5, #6)
  - [x] 4.1: Return empty results when no events exist (total_starts=0, total_ends=0, daily_usage=[])
  - [x] 4.2: Return HTTP 404 for non-existent machine_id (handled by get_object())

- [x] Task 5: Write tests for statistics API (AC: #1-#6)
  - [x] 5.1: Create blog/tests/test_stats.py
  - [x] 5.2: Test stats endpoint returns 200 with valid token
  - [x] 5.3: Test response contains all required fields
  - [x] 5.4: Test date_from filter works correctly
  - [x] 5.5: Test date_to filter works correctly
  - [x] 5.6: Test combined date_from + date_to filter
  - [x] 5.7: Test empty results for machine with no events
  - [x] 5.8: Test 404 for non-existent machine

## Dev Notes

### Technical Requirements

**Framework & Versions:**
- Django 5.2.6
- Django REST Framework (already installed)
- rest_framework.authtoken (already configured)

**Stats Action Implementation (from docs/final/implementation/02-backend.md):**
```python
from django.db.models import Count
from django.db.models.functions import TruncDate

class GymMachineViewSet(viewsets.ModelViewSet):
    queryset = GymMachine.objects.filter(is_active=True)
    serializer_class = GymMachineSerializer

    @action(detail=True, methods=['get'])
    def stats(self, request, pk=None):
        """기구별 통계"""
        machine = self.get_object()
        date_from = request.query_params.get('date_from')
        date_to = request.query_params.get('date_to')

        events = machine.events.all()
        if date_from:
            events = events.filter(captured_at__date__gte=date_from)
        if date_to:
            events = events.filter(captured_at__date__lte=date_to)

        # 일별 사용 횟수
        daily_stats = events.filter(event_type='start').annotate(
            date=TruncDate('captured_at')
        ).values('date').annotate(
            count=Count('id')
        ).order_by('date')

        return Response({
            'machine_id': machine.id,
            'machine_name': machine.name,
            'total_starts': events.filter(event_type='start').count(),
            'total_ends': events.filter(event_type='end').count(),
            'daily_usage': list(daily_stats)
        })
```

**API Endpoint (from docs/final/implementation/05-api-reference.md):**
- Method: GET
- URL: `/api_root/machines/{id}/stats/`
- Query Parameters:
  - date_from (optional): Filter events >= date (YYYY-MM-DD)
  - date_to (optional): Filter events <= date (YYYY-MM-DD)
- Success Response (200):
```json
{
  "machine_id": 1,
  "machine_name": "런닝머신 #1",
  "total_starts": 152,
  "total_ends": 150,
  "daily_usage": [
    {"date": "2023-10-25", "count": 12},
    {"date": "2023-10-26", "count": 18},
    {"date": "2023-10-27", "count": 15}
  ]
}
```

### Architecture Compliance

**Existing Infrastructure:**
- `blog/models.py`: Has GymMachine, MachineEvent
- `blog/views.py`: Has GymMachineViewSet - ADD stats action here
- `mysite/urls.py`: Router has machines - stats action auto-registered

**Code Patterns to Follow:**
- Use `@action` decorator for custom ViewSet actions
- Use `get_object()` for automatic 404 handling
- Use query_params for filtering
- Follow existing Response format patterns

### Testing Standards

**Test Framework:** Django's built-in TestCase with DRF's APIClient
**Test Location:** `blog/tests/test_stats.py`

**Required Test Cases:**
1. Stats endpoint returns 200 OK with valid token
2. Response contains machine_id, machine_name, total_starts, total_ends, daily_usage
3. date_from filter excludes earlier events
4. date_to filter excludes later events
5. Combined date filters work correctly
6. Empty machine returns zeros and empty array
7. Non-existent machine returns 404
8. Unauthenticated request returns 401

**Commands:**
```bash
cd PhotoBlogServer
python manage.py test blog.tests.test_stats
```

### Performance Considerations

**From test-design-epic-3.md:**
- R-301: 대량 이벤트 집계 시 API 응답 지연 (Score: 6)
- Mitigation: DB indexes already exist on (machine, -captured_at) and (event_type, -captured_at)
- Target: < 500ms for 1000 events, < 2s for 10000 events

### References

- [Source: docs/final/implementation/02-backend.md#GymMachineViewSet.stats]
- [Source: docs/final/implementation/05-api-reference.md#기구 통계]
- [Source: _bmad-output/stories/epic-3-stories.md#Story 3.1]
- [Dependency: Epic 2 MachineEvent model]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

N/A - Implementation completed without issues

### Completion Notes List

- Added stats action to GymMachineViewSet with @action decorator
- Implemented date range filtering with date_from/date_to query params
- Implemented daily_usage aggregation using TruncDate and Count
- All 13 test cases pass covering all acceptance criteria
- Endpoint auto-registered at `/api_root/machines/{id}/stats/` via router

### File List

**Created:**
- `PhotoBlogServer/blog/tests/test_stats.py` - 13 test cases for stats API

**Modified:**
- `PhotoBlogServer/blog/views.py` - Added imports and stats action to GymMachineViewSet

**Deleted:**
(None)

## Change Log

- 2025-12-19: Story created with comprehensive context from implementation specs
- 2025-12-19: Story implemented - stats action added with full test coverage
