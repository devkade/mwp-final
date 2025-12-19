---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
workflowStatus: complete
inputDocuments:
  - docs/final/gym-service-overview.md
  - docs/final/implementation/README.md
  - docs/final/implementation/01-codebase-analysis.md
  - docs/final/implementation/02-backend.md
  - docs/final/implementation/03-edge-system.md
  - docs/final/implementation/04-android.md
  - docs/final/implementation/05-api-reference.md
  - docs/final/implementation/06-implementation-plan.md
  - docs/final/ui/README.md
---

# mwp-finalterm-blog - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for mwp-finalterm-blog, decomposing the requirements from the PRD, UX Design if it exists, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

**Edge System:**
- FR1: Edge device SHALL perform object detection using YOLOv5 pretrained model
- FR2: Edge device SHALL detect MS COCO 80 classes
- FR3: Edge device SHALL perform Change Detection on person class (0→1 = start event, 1→0 = end event)
- FR4: Edge device SHALL send events to server via REST API only when state changes occur
- FR5: Edge device SHALL capture and send image, detections JSON, change_info JSON, captured_at timestamp, security_key, and machine_id

**Service System (Django Backend):**
- FR6: Server SHALL provide security key based authentication via POST /api/auth/login/
- FR7: Server SHALL store and manage Image Blog events with associated metadata
- FR8: Server SHALL provide RESTful API for event posting from Edge (POST /api/machines/{id}/events/)
- FR9: Server SHALL provide RESTful API for image list/retrieval for Android client
- FR10: Server SHALL provide machine list API (GET /api_root/machines/)
- FR11: Server SHALL provide machine event list API with filtering by event_type, date_from, date_to, and pagination
- FR12: Server SHALL provide event detail API (GET /api_root/events/{id}/)
- FR13: Server SHALL provide machine statistics API (GET /api_root/machines/{id}/stats/)

**Client System (Android):**
- FR14: Android app SHALL display image list view using RecyclerView
- FR15: Android app SHALL use REST API for fetching image lists and details
- FR16: Android app SHALL provide security key login screen
- FR17: Android app SHALL provide equipment/machine list screen
- FR18: Android app SHALL provide event list screen per equipment with filtering
- FR19: Android app SHALL provide event detail screen showing full image and metadata
- FR20: Android app SHALL provide usage statistics screen
- FR21: Android app SHALL store successful security key locally for session reuse

### NonFunctional Requirements

- NFR1: Django server SHALL be deployed on PythonAnywhere
- NFR2: All API endpoints (except login) SHALL require Token authentication
- NFR3: Security key authentication SHALL return a Token for subsequent API calls
- NFR4: Android app SHALL support minimum SDK 24 (Android 7.0)
- NFR5: Existing Photo Blog API (/api_root/Post/, /api-token-auth/) SHALL be maintained for backward compatibility
- NFR6: Edge SHALL resize images to 640x480 before uploading to reduce bandwidth
- NFR7: Server SHALL support Korean locale (ko) and Asia/Seoul timezone
- NFR8: Android app SHALL handle network errors gracefully with user feedback

### Additional Requirements

**From Architecture - Starter Template:**
- Use existing Photo Blog codebase (PhotoBlogServer, PhotoViewer, yolov5) as foundation
- Reuse Django REST Framework Token authentication pattern
- Reuse existing REST API router structure
- Reuse Android RecyclerView and Adapter patterns
- Extend existing YOLOv5 ChangeDetection to support bidirectional detection (0→1 AND 1→0)

**From Architecture - New Models Required:**
- ApiUser model: security_key based API user linked to Django User
- GymMachine model: equipment metadata (name, type, location, description, thumbnail)
- MachineEvent model: usage event log (machine FK, event_type, image, captured_at, person_count, detections JSON, change_info JSON)

**From Architecture - Database:**
- Index on (machine, -captured_at) for efficient event queries
- Index on (event_type, -captured_at) for filtered queries

**From UX Design:**
- 5 main screens: Login, Equipment List, Usage History, Event Details, Usage Statistics
- Material Design components (MaterialCardView, Chips, SwipeRefreshLayout)
- Color scheme: Primary #12c0e2, Success #28A745 (START), Danger #DC3545 (END)
- Inter font family with weights 400, 500, 700
- Material Symbols Outlined icons
- Event type filtering via Chip group (All, Start, End)
- Date range filtering capability
- Status indicators on equipment list (In Use, Available)

### FR Coverage Map

| FR | Epic | Description |
|----|------|-------------|
| FR1 | Epic 2 | Edge YOLOv5 object detection |
| FR2 | Epic 2 | Edge MS COCO 80 classes |
| FR3 | Epic 2 | Edge Change Detection (start/end) |
| FR4 | Epic 2 | Edge REST API event posting |
| FR5 | Epic 2 | Edge event data (image, detections, change_info) |
| FR6 | Epic 1 | Security key authentication API |
| FR7 | Epic 2 | Image Blog event storage |
| FR8 | Epic 2 | Event posting API (Edge → Server) |
| FR9 | Epic 2 | Image list/retrieval API |
| FR10 | Epic 1 | Machine list API |
| FR11 | Epic 2 | Event list API with filtering |
| FR12 | Epic 2 | Event detail API |
| FR13 | Epic 3 | Machine statistics API |
| FR14 | Epic 1 (Equipment), Epic 2 (Events) | Android RecyclerView - Epic 1 establishes pattern for Equipment, Epic 2 extends for Events |
| FR15 | Epic 1 (Equipment), Epic 2 (Events) | Android REST API client - Epic 1 establishes pattern, Epic 2 extends |
| FR16 | Epic 1 | Android security key login screen |
| FR17 | Epic 1 | Android equipment list screen |
| FR18 | Epic 2 | Android event list screen |
| FR19 | Epic 2 | Android event detail screen |
| FR20 | Epic 3 | Android statistics screen |
| FR21 | Epic 1 | Android local key storage |

## Epic List

### Epic 1: Equipment Discovery & System Access

**Goal:** Users can authenticate with a security key and view all available gym equipment with their status and location. Tapping equipment shows an empty state placeholder preparing users for Epic 2.

**FRs Covered:** FR6, FR10, FR14 (Equipment pattern), FR15 (API client pattern), FR16, FR17, FR21

**User Outcome:** User can log in, browse equipment list, and understand the system structure.

**Implementation Scope:**
- **Backend:** ApiUser model, GymMachine model, login API (`POST /api/auth/login/`), machines list API (`GET /api_root/machines/`)
- **Android:** Login screen, Equipment List screen, Empty state for event list ("No events yet - detection will start soon")

**UX Note:** Include empty state placeholder when user taps equipment, showing friendly message that events will appear once detection begins.

---

### Epic 2: Usage Event Monitoring & Detection

**Goal:** Users can view detailed usage history for any equipment, filter by event type and date, and see event details. Edge devices can detect and report usage events automatically.

**FRs Covered:** FR1, FR2, FR3, FR4, FR5, FR7, FR8, FR9, FR11, FR12, FR14 (Events extension), FR15 (Events extension), FR18, FR19

**User Outcome:** Complete usage monitoring system - Edge reports events in real-time, users view and filter them.

**Implementation Order (Critical):**
1. **Backend First:** MachineEvent model, event posting API, event list/detail API with filtering
2. **Edge Second:** Bidirectional ChangeDetection (0→1 start, 1→0 end), API integration
3. **Android Last:** Usage History screen (extends RecyclerView pattern from Epic 1), Event Detail screen

**Technical Note:** Android implementation extends the RecyclerView and API client patterns established in Epic 1 for consistency.

---

### Epic 3: Usage Analytics & Statistics

**Goal:** Users can analyze equipment usage patterns to make data-driven operational decisions (peak hours, most used equipment, daily trends).

**FRs Covered:** FR13, FR20

**User Outcome:** Analytics capability enabling gym managers to optimize equipment placement and staffing.

**Implementation Scope:**
- **Backend:** Statistics endpoint with date range filtering and aggregation (`GET /api_root/machines/{id}/stats/`)
- **Android:** Usage Statistics screen with metrics cards, usage charts, heatmap visualization

---

## Story Files

| Epic | Stories File |
|------|--------------|
| Epic 1 | [stories/epic-1-stories.md](stories/epic-1-stories.md) |
| Epic 2 | [stories/epic-2-stories.md](stories/epic-2-stories.md) |
| Epic 3 | [stories/epic-3-stories.md](stories/epic-3-stories.md) |
