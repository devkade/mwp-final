---
epic: 3
title: Usage Analytics & Statistics
status: complete
storiesCompleted: [3.1, 3.2]
---

# Epic 3: Usage Analytics & Statistics

**Goal:** Users can analyze equipment usage patterns to make data-driven operational decisions (peak hours, most used equipment, daily trends).

**FRs Covered:** FR13, FR20

---

## Story 3.1: Backend Statistics API

**As a** gym manager,
**I want** to retrieve usage statistics for equipment via API,
**So that** I can analyze usage patterns and make operational decisions.

**Acceptance Criteria:**

**Given** I have a valid auth token
**When** I GET `/api_root/machines/{machine_id}/stats/`
**Then** I receive HTTP 200 with statistics JSON

**Given** events exist for a machine
**When** I GET the stats endpoint
**Then** I receive: machine_id, machine_name, total_starts, total_ends, daily_usage array

**Given** I request stats with date range
**When** I GET `/api_root/machines/{machine_id}/stats/?date_from=2024-01-01&date_to=2024-01-31`
**Then** statistics are calculated only for events within that date range

**Given** daily_usage is returned
**When** I view the array
**Then** each item contains: date (YYYY-MM-DD), count (number of start events)

**Given** no events exist for a machine
**When** I GET the stats endpoint
**Then** I receive: total_starts=0, total_ends=0, daily_usage=[]

**Given** I request stats for a non-existent machine
**When** I GET the stats endpoint
**Then** I receive HTTP 404 Not Found

---

## Story 3.2: Android Statistics Screen

**As a** gym manager,
**I want** to view usage statistics in the app with visual charts,
**So that** I can quickly understand usage patterns and make informed decisions.

**Acceptance Criteria:**

**Given** I am on the Equipment List or Event List screen
**When** I tap the statistics menu/button
**Then** I navigate to the Statistics screen

**Given** I am on the Statistics screen
**When** the screen loads
**Then** I see a date range selector defaulting to the last 7 days

**Given** I am on the Statistics screen
**When** I view the metrics section
**Then** I see cards showing: Total Workouts (total_starts), total usage sessions

**Given** I am on the Statistics screen
**When** I view the Usage by Equipment section
**Then** I see a horizontal bar chart comparing usage across equipment

**Given** I select a different date range
**When** the selection is confirmed
**Then** all statistics are refreshed for the new date range

**Given** I am on the Statistics screen
**When** I view the daily usage section
**Then** I see daily_usage data visualized (list or simple chart)

**Given** the API call fails
**When** the Statistics screen loads
**Then** I see an error message with retry option

**Given** no usage data exists for the selected period
**When** the screen loads
**Then** I see an empty state: "선택한 기간에 사용 기록이 없습니다" (No usage records for selected period)
