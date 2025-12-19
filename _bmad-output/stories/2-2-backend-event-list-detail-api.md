# Story 2.2: Backend Event List & Detail API

Status: ready-for-dev

## Story

As a **gym manager**,
I want **to retrieve usage events via API with filtering options**,
so that **I can view equipment usage history filtered by type and date**.

## Acceptance Criteria

1. **Given** I have a valid auth token **When** I GET `/api/machines/{machine_id}/events/` **Then** I receive HTTP 200 with a JSON array of events for that machine, ordered by captured_at descending

2. **Given** events exist for a machine **When** I GET `/api/machines/{machine_id}/events/?event_type=start` **Then** I receive only events with event_type="start"

3. **Given** events exist for a machine **When** I GET `/api/machines/{machine_id}/events/?event_type=end` **Then** I receive only events with event_type="end"

4. **Given** events exist for a machine **When** I GET `/api/machines/{machine_id}/events/?date_from=2024-01-01&date_to=2024-01-31` **Then** I receive only events within the specified date range

5. **Given** I have an event_id **When** I GET `/api_root/events/{event_id}/` **Then** I receive full event details including: id, machine, machine_name, event_type, event_type_display, image URL, captured_at, created_at, person_count, detections JSON, change_info JSON

6. **Given** many events exist **When** I GET the event list **Then** results are paginated with default page size

7. **Given** I request a non-existent event_id **When** I GET `/api_root/events/{event_id}/` **Then** I receive HTTP 404 Not Found

## Tasks / Subtasks

- [ ] Task 1: Verify event list endpoint exists (from Story 2.1) (AC: #1)
  - [ ] 1.1: Confirm GET `/api/machines/{machine_id}/events/` returns event list
  - [ ] 1.2: Confirm events are ordered by captured_at descending

- [ ] Task 2: Implement event_type filtering (AC: #2, #3)
  - [ ] 2.1: Verify query param `?event_type=start` filters correctly
  - [ ] 2.2: Verify query param `?event_type=end` filters correctly

- [ ] Task 3: Implement date range filtering (AC: #4)
  - [ ] 3.1: Verify `?date_from=YYYY-MM-DD` filters events >= date
  - [ ] 3.2: Verify `?date_to=YYYY-MM-DD` filters events <= date
  - [ ] 3.3: Verify combined date_from and date_to work together

- [ ] Task 4: Verify event detail endpoint (AC: #5, #7)
  - [ ] 4.1: Confirm GET `/api_root/events/{id}/` returns full event details
  - [ ] 4.2: Confirm all required fields are present in response
  - [ ] 4.3: Confirm non-existent event_id returns 404

- [ ] Task 5: Add pagination support (AC: #6)
  - [ ] 5.1: Configure DRF pagination in settings.py
  - [ ] 5.2: Set default page size (e.g., 20)
  - [ ] 5.3: Test pagination with many events

- [ ] Task 6: Write tests for event list/detail API (AC: #1-#7)
  - [ ] 6.1: Add tests to blog/tests/test_events.py
  - [ ] 6.2: Test event list returns correct events for machine
  - [ ] 6.3: Test event_type filter (start/end)
  - [ ] 6.4: Test date range filter
  - [ ] 6.5: Test event detail returns all fields
  - [ ] 6.6: Test 404 for non-existent event
  - [ ] 6.7: Test pagination response format

## Dev Notes

### Technical Requirements

**Note:** Most functionality was implemented in Story 2.1. This story verifies and adds pagination.

**Pagination Configuration (settings.py):**
```python
REST_FRAMEWORK = {
    # ... existing settings ...
    'DEFAULT_PAGINATION_CLASS': 'rest_framework.pagination.PageNumberPagination',
    'PAGE_SIZE': 20,
}
```

**Query Parameters:**
- `event_type`: Filter by event type ("start" or "end")
- `date_from`: Filter events on or after date (YYYY-MM-DD format)
- `date_to`: Filter events on or before date (YYYY-MM-DD format)
- `page`: Page number for pagination

**Response Format (Paginated):**
```json
{
  "count": 100,
  "next": "http://.../events/?page=2",
  "previous": null,
  "results": [...]
}
```

### Architecture Compliance

**Files to Modify:**
- `mysite/settings.py`: Add pagination configuration to REST_FRAMEWORK
- `blog/tests/test_events.py`: Add additional tests for filtering and pagination

**Existing Implementation (from Story 2.1):**
- Event list endpoint: `/api/machines/{machine_id}/events/`
- Event detail endpoint: `/api_root/events/{id}/`
- event_type filtering: Implemented
- date_from/date_to filtering: Implemented

### Testing Standards

**Test Framework:** Django's built-in TestCase with DRF's APIClient
**Test Location:** `blog/tests/test_events.py`

**Additional Test Cases:**
1. Event list returns events for specific machine only
2. Event list ordered by captured_at descending
3. event_type=start filter works
4. event_type=end filter works
5. date_from filter works
6. date_to filter works
7. Combined date filters work
8. Event detail returns all required fields
9. Non-existent event returns 404
10. Pagination response format correct

**Commands:**
```bash
cd PhotoBlogServer
python manage.py test blog.tests.test_events
```

### References

- [Source: docs/final/implementation/05-api-reference.md#이벤트 API]
- [Source: _bmad-output/stories/epic-2-stories.md#Story 2.2]
- [Dependency: Story 2.1 (MachineEvent model and base endpoints)]

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
