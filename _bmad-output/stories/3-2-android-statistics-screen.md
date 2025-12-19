# Story 3.2: Android Statistics Screen

Status: pending

## Story

As a **gym manager**,
I want **to view usage statistics in the app with visual charts**,
so that **I can quickly understand usage patterns and make informed decisions**.

## Acceptance Criteria

1. **Given** I am on the Equipment List or Event List screen **When** I tap the statistics menu/button **Then** I navigate to the Statistics screen

2. **Given** I am on the Statistics screen **When** the screen loads **Then** I see a date range selector defaulting to the last 7 days

3. **Given** I am on the Statistics screen **When** I view the metrics section **Then** I see cards showing: Total Workouts (total_starts), total usage sessions

4. **Given** I am on the Statistics screen **When** I view the Usage by Equipment section **Then** I see a horizontal bar chart comparing usage across equipment

5. **Given** I select a different date range **When** the selection is confirmed **Then** all statistics are refreshed for the new date range

6. **Given** I am on the Statistics screen **When** I view the daily usage section **Then** I see daily_usage data visualized (list or simple chart)

7. **Given** the API call fails **When** the Statistics screen loads **Then** I see an error message with retry option

8. **Given** no usage data exists for the selected period **When** the screen loads **Then** I see an empty state: "선택한 기간에 사용 기록이 없습니다" (No usage records for selected period)

## Tasks / Subtasks

- [ ] Task 1: Create StatsActivity layout (AC: #2, #3, #4, #6)
  - [ ] 1.1: Create activity_stats.xml with toolbar and title
  - [ ] 1.2: Add date range selector button with default "최근 7일" text
  - [ ] 1.3: Add total_starts card (총 사용 시작)
  - [ ] 1.4: Add total_ends card (총 사용 종료)
  - [ ] 1.5: Add daily_usage bar chart area using MPAndroidChart
  - [ ] 1.6: Add empty state layout (hidden by default)
  - [ ] 1.7: Add error state layout with retry button (hidden by default)

- [ ] Task 2: Create StatsActivity.java (AC: #1)
  - [ ] 2.1: Create StatsActivity class extending AppCompatActivity
  - [ ] 2.2: Accept machine_id and machine_name from Intent extras
  - [ ] 2.3: Set toolbar title to "{machine_name} 통계"
  - [ ] 2.4: Initialize views and chart

- [ ] Task 3: Implement statistics API call (AC: #2, #3, #6)
  - [ ] 3.1: Add getMachineStats() method to GymApiService
  - [ ] 3.2: Calculate default date range (last 7 days)
  - [ ] 3.3: Call API on screen load with default date range
  - [ ] 3.4: Parse response and populate metric cards
  - [ ] 3.5: Render daily_usage data in bar chart

- [ ] Task 4: Implement date range selection (AC: #5)
  - [ ] 4.1: Add click listener to date range button
  - [ ] 4.2: Show MaterialDatePicker range picker dialog
  - [ ] 4.3: On confirm, update date range text and refresh data
  - [ ] 4.4: On cancel, maintain previous date range

- [ ] Task 5: Handle empty state (AC: #8)
  - [ ] 5.1: Check if total_starts == 0 and daily_usage is empty
  - [ ] 5.2: Show empty state layout with "선택한 기간에 사용 기록이 없습니다"
  - [ ] 5.3: Hide chart and metric cards

- [ ] Task 6: Handle error state (AC: #7)
  - [ ] 6.1: Catch API errors in Retrofit callback
  - [ ] 6.2: Show error state layout with error message
  - [ ] 6.3: Add retry button click listener to re-fetch data
  - [ ] 6.4: Hide chart and metric cards on error

- [ ] Task 7: Add navigation from Equipment List (AC: #1)
  - [ ] 7.1: Add action menu or button to MachineListActivity item
  - [ ] 7.2: Show dialog with options: "이벤트 목록" / "사용 통계"
  - [ ] 7.3: On "사용 통계" selection, launch StatsActivity with machine_id

- [ ] Task 8: Configure MPAndroidChart (AC: #4, #6)
  - [ ] 8.1: Add MPAndroidChart dependency to build.gradle
  - [ ] 8.2: Configure BarChart with X-axis date labels (DD format)
  - [ ] 8.3: Configure Y-axis with count values
  - [ ] 8.4: Set chart colors and styling
  - [ ] 8.5: Enable touch interaction for data points

- [ ] Task 9: Write tests for StatsActivity (AC: #1-#8)
  - [ ] 9.1: Test screen launches with correct machine name in title
  - [ ] 9.2: Test metric cards display correct values
  - [ ] 9.3: Test date range picker opens and applies selection
  - [ ] 9.4: Test empty state shows when no data
  - [ ] 9.5: Test error state shows on API failure
  - [ ] 9.6: Test retry button refreshes data

## Dev Notes

### Technical Requirements

**Android Versions:**
- Min SDK: 24 (Android 7.0)
- Target SDK: 34
- Compile SDK: 34

**Dependencies to Add:**
```gradle
// build.gradle (app level)
dependencies {
    // MPAndroidChart for bar chart visualization
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'

    // Material Components for DateRangePicker
    implementation 'com.google.android.material:material:1.11.0'
}
```

**Layout Structure (activity_stats.xml):**
```xml
<LinearLayout>
    <!-- Toolbar with machine name -->
    <androidx.appcompat.widget.Toolbar />

    <!-- Date Range Selector -->
    <Button
        android:id="@+id/btnDateRange"
        android:text="최근 7일: 2024-01-01 ~ 01-07" />

    <!-- Metric Cards Row -->
    <LinearLayout orientation="horizontal">
        <CardView id="@+id/cardTotalStarts">
            <TextView text="총 시작" />
            <TextView id="@+id/tvTotalStarts" text="152회" />
        </CardView>
        <CardView id="@+id/cardTotalEnds">
            <TextView text="총 종료" />
            <TextView id="@+id/tvTotalEnds" text="150회" />
        </CardView>
    </LinearLayout>

    <!-- Daily Usage Chart -->
    <com.github.mikephil.charting.charts.BarChart
        android:id="@+id/chartDailyUsage" />

    <!-- Empty State (hidden by default) -->
    <LinearLayout id="@+id/layoutEmptyState" visibility="gone">
        <TextView text="선택한 기간에 사용 기록이 없습니다" />
    </LinearLayout>

    <!-- Error State (hidden by default) -->
    <LinearLayout id="@+id/layoutErrorState" visibility="gone">
        <TextView id="@+id/tvErrorMessage" />
        <Button id="@+id/btnRetry" text="다시 시도" />
    </LinearLayout>
</LinearLayout>
```

**API Service Addition:**
```java
// GymApiService.java
public interface GymApiService {
    @GET("api_root/machines/{id}/stats/")
    Call<MachineStats> getMachineStats(
        @Path("id") int machineId,
        @Query("date_from") String dateFrom,
        @Query("date_to") String dateTo
    );
}

// MachineStats.java (new model)
public class MachineStats {
    private int machine_id;
    private String machine_name;
    private int total_starts;
    private int total_ends;
    private List<DailyUsage> daily_usage;

    public static class DailyUsage {
        private String date;  // "2024-01-15"
        private int count;
    }
}
```

### Architecture Compliance

**Files to Create:**
- `app/src/main/java/com/example/photoviewer/StatsActivity.java`
- `app/src/main/java/com/example/photoviewer/models/MachineStats.java`
- `app/src/main/res/layout/activity_stats.xml`

**Files to Modify:**
- `app/build.gradle`: Add MPAndroidChart dependency
- `app/src/main/java/com/example/photoviewer/services/GymApiService.java`: Add getMachineStats method
- `app/src/main/java/com/example/photoviewer/MachineListActivity.java`: Add navigation to StatsActivity
- `app/src/main/AndroidManifest.xml`: Register StatsActivity

**Code Patterns to Follow:**
- Use Retrofit for API calls (existing pattern)
- Use Material DateRangePicker for date selection
- Follow existing Activity patterns (LoginActivity, MachineListActivity)
- Use CardView for metric display (existing pattern)

### UI/UX Requirements

**From test-design-epic-3.md:**
- Date range default: Last 7 days
- Metric cards: Display total_starts and total_ends prominently
- Chart: Bar chart with date labels on X-axis (DD format: 01, 02, 03...)
- Weekly change indicator: "▲ +12 (이번 주)" for positive change
- Empty state: Korean text "선택한 기간에 사용 기록이 없습니다"
- Error state: Retry button for API failures

**Screen Rotation:**
- Save and restore state on rotation (ViewModel recommended)
- Chart should re-render on orientation change

### Testing Standards

**Test Framework:** Android JUnit / Espresso
**Test Location:** `app/src/androidTest/java/com/example/photoviewer/`

**Required Test Cases (from test-design-epic-3.md):**
- AN-3-01: Statistics 화면 진입
- AN-3-02: 총 사용 시작 횟수 카드 표시
- AN-3-03: 총 사용 종료 횟수 카드 표시
- AN-3-04: 일별 사용량 차트 표시
- AN-3-06: 날짜 범위 선택 다이얼로그
- AN-3-07: 날짜 범위 적용 후 데이터 갱신
- AN-3-09: 데이터 없는 기간 Empty State
- AN-3-10: 로딩 중 ProgressBar 표시
- AN-3-11: 동작 선택 메뉴 표시
- AN-3-13: DateRangePicker 취소 시 기존 범위 유지
- AN-3-16: 기구 이름 타이틀 표시

### Risk Mitigations

**R-303: 차트 라이브러리 호환성 (Score: 4)**
- Fix MPAndroidChart version to v3.1.0
- Test on API 24, 28, 34 emulators

**R-304: 빈 통계 데이터 시 UI 표시 오류 (Score: 4)**
- Explicit empty state layout
- Check for null/empty daily_usage before chart rendering

**R-305: 날짜 범위 선택 UI 혼란 (Score: 2)**
- Use Material DateRangePicker (familiar UX)
- Show selected range clearly in button text

### References

- [Source: docs/final/implementation/04-android.md#StatsActivity]
- [Source: docs/final/implementation/05-api-reference.md#기구 통계]
- [Source: _bmad-output/stories/epic-3-stories.md#Story 3.2]
- [Source: _bmad-output/test/test-design-epic-3.md#Android UI Tests]
- [Dependency: Story 3.1 (Backend Statistics API)]
- [Dependency: Epic 1 MachineListActivity]

## Dev Agent Record

### Agent Model Used

(To be filled on implementation)

### Debug Log References

(To be filled on implementation)

### Completion Notes List

(To be filled on implementation)

### File List

**Created:**
(To be filled on implementation)

**Modified:**
(To be filled on implementation)

**Deleted:**
(None expected)

## Change Log

- 2025-12-19: Story created with comprehensive context from implementation specs
