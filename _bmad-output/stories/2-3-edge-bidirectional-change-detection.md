# Story 2.3: Edge Bidirectional Change Detection

Status: done

## Story

As an **Edge system**,
I want **to detect both usage start (0→1) and usage end (1→0) events for the target class**,
so that **complete usage sessions are tracked automatically**.

## Acceptance Criteria

1. **Given** the ChangeDetection class is initialized with target_class="person" **When** person count changes from 0 to 1 or more **Then** a "start" event is triggered with change_info containing prev_count=0 and curr_count>=1

2. **Given** person count was previously 1 or more **When** person count changes to 0 **Then** an "end" event is triggered with change_info containing prev_count>=1 and curr_count=0

3. **Given** person count changes from 1 to 2 **When** the frame is processed **Then** no event is triggered (only 0→N and N→0 transitions matter)

4. **Given** person count remains at 0 across frames **When** frames are processed **Then** no event is triggered (no state change)

5. **Given** an event is triggered **When** the change_info is generated **Then** it includes: event_type, target_class, prev_count, curr_count, timestamp (ISO format)

6. **Given** an event is triggered **When** the image is captured **Then** the current frame is resized to 640x480 and saved locally to `runs/detect/events/`

## Tasks / Subtasks

- [x] Task 1: Create ChangeDetection class structure (AC: #1, #2)
  - [x] 1.1: Create `yolov5/changedetection.py` file
  - [x] 1.2: Implement `__init__` method with names list and config dict
  - [x] 1.3: Initialize `result_prev` array to track previous frame counts
  - [x] 1.4: Initialize TARGET_CLASS from config (default: "person")

- [x] Task 2: Implement bidirectional change detection logic (AC: #1, #2, #3, #4)
  - [x] 2.1: Implement `detect_changes(names, detected_current, image, detections_raw)` method
  - [x] 2.2: Get target class index from names list
  - [x] 2.3: Compare prev_count vs curr_count for target class
  - [x] 2.4: Trigger "start" event when prev=0 and curr>=1
  - [x] 2.5: Trigger "end" event when prev>=1 and curr=0
  - [x] 2.6: Update result_prev after each detection
  - [x] 2.7: Return None when no state change (1→2, 0→0, etc.)

- [x] Task 3: Generate change_info JSON (AC: #5)
  - [x] 3.1: Create change_info dict with event_type, target_class, prev_count, curr_count
  - [x] 3.2: Add ISO format timestamp using datetime.now().isoformat()
  - [x] 3.3: Return change_info from detect_changes when event triggered

- [x] Task 4: Implement image capture and save (AC: #6)
  - [x] 4.1: Create `runs/detect/events/` directory if not exists
  - [x] 4.2: Resize image to 640x480 using cv2.resize with INTER_AREA
  - [x] 4.3: Generate filename with timestamp and event_type
  - [x] 4.4: Save image as JPEG to the events directory

- [x] Task 5: Write unit tests for ChangeDetection (AC: #1-#6)
  - [x] 5.1: Create `yolov5/tests/test_changedetection.py`
  - [x] 5.2: Test start event triggered on 0→1 transition
  - [x] 5.3: Test end event triggered on 1→0 transition
  - [x] 5.4: Test no event on 1→2 transition
  - [x] 5.5: Test no event on 0→0 (no change)
  - [x] 5.6: Test change_info contains all required fields
  - [x] 5.7: Test image saved to correct directory with correct size

## Dev Notes

### Technical Requirements

**Framework & Libraries:**
- Python 3.x
- OpenCV (cv2) for image processing
- datetime for timestamps
- pathlib for directory management

**Class Specification (from docs/final/implementation/03-edge-system.md):**
```python
class ChangeDetection:
    """양방향 Change Detection (0→1 사용시작, 1→0 사용종료)"""

    def __init__(self, names, config=None):
        self.names = names
        self.result_prev = [0 for _ in range(len(names))]
        self.config = config or {}
        self.TARGET_CLASS = self.config.get('target_class', 'person')

    def detect_changes(self, names, detected_current, image, detections_raw):
        """
        Args:
            names: 클래스 이름 리스트
            detected_current: 현재 프레임 검출 결과 [0,1,0,1,...]
            image: 현재 프레임 이미지
            detections_raw: YOLO 원본 검출 결과

        Returns:
            dict: change_info (변화 없으면 None)
        """
        pass
```

**change_info Structure:**
```json
{
  "event_type": "start",
  "target_class": "person",
  "prev_count": 0,
  "curr_count": 1,
  "timestamp": "2024-01-15T14:35:10.123456"
}
```

**Image Save Path:**
- Directory: `runs/detect/events/`
- Filename format: `{YYYYMMDD_HHMMSS}_{event_type}.jpg`
- Size: 640x480 pixels
- Format: JPEG

### Architecture Compliance

**Files to Create:**
- `yolov5/changedetection.py` - Main ChangeDetection class
- `yolov5/tests/__init__.py` - Test package init
- `yolov5/tests/test_changedetection.py` - Unit tests

**Event Detection Logic:**
1. Get target class index (default: "person" which is class 0 in COCO)
2. Compare previous count with current count
3. Only trigger on boundary transitions (0→N or N→0)
4. Ignore within-range changes (1→2, 2→1, etc.)

### Testing Standards

**Test Framework:** Python unittest or pytest
**Test Location:** `yolov5/tests/test_changedetection.py`

**Required Test Cases:**
1. ChangeDetection initialization with default config
2. ChangeDetection initialization with custom target_class
3. Start event: 0→1 transition triggers event
4. Start event: 0→3 transition triggers event
5. End event: 1→0 transition triggers event
6. End event: 3→0 transition triggers event
7. No event: 1→2 transition (no boundary)
8. No event: 2→1 transition (no boundary)
9. No event: 0→0 (no change)
10. change_info has all required fields
11. Image saved to correct path
12. Image dimensions are 640x480

**Commands:**
```bash
cd yolov5
python -m pytest tests/test_changedetection.py -v
```

### References

- [Source: docs/final/implementation/03-edge-system.md#ChangeDetection 클래스 수정]
- [Source: _bmad-output/stories/epic-2-stories.md#Story 2.3]

## Dev Agent Record

### Agent Model Used

Claude (via Claude Code CLI)

### Debug Log References

- Commit `1e53fd1`: feat(edge): implement bidirectional change detection (Story 2.3)
- Commit `444d0e2`: Merge branch 'feat/story2.3' - Edge bidirectional change detection

### Completion Notes List

- Implemented ChangeDetection class with bidirectional detection (0→1+ start, 1+→0 end)
- 18 unit test cases covering all acceptance criteria
- Image save functionality with 640x480 resize and timestamp-based filenames
- All tests passing

### File List

**Created:**
- `yolov5/changedetection.py` (108 lines)
- `yolov5/tests/__init__.py`
- `yolov5/tests/test_changedetection.py` (347 lines)

**Modified:**
(None)

**Deleted:**
(None)

## Change Log

- 2025-12-19: Story created with comprehensive context from implementation specs
- 2025-12-19: Implementation completed and merged to main
