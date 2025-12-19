package com.example.photoviewer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.photoviewer.adapters.EventAdapter;
import com.example.photoviewer.models.MachineEvent;
import com.example.photoviewer.services.GymApiService;
import com.example.photoviewer.services.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * EventListActivity - Equipment Event List Screen
 */
public class EventListActivity extends AppCompatActivity {
    private static final String TAG = "EventListActivity";

    public static final String EXTRA_MACHINE_ID = "machine_id";
    public static final String EXTRA_MACHINE_NAME = "machine_name";
    public static final String EXTRA_EVENT_ID = "event_id";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private View emptyStateLayout;
    private TextView emptyTitle;
    private TextView emptySubtitle;
    private ProgressBar progressBar;
    private MaterialButton buttonDateRange;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<MachineEvent> events = new ArrayList<>();

    private int machineId = -1;
    private String filterEventType = null; // null, "start", "end"
    private String dateFrom = null; // YYYY-MM-DD
    private String dateTo = null;   // YYYY-MM-DD

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        if (!SessionManager.getInstance().isLoggedIn()) {
            redirectToLogin();
            return;
        }

        machineId = getIntent().getIntExtra(EXTRA_MACHINE_ID, -1);
        String machineName = getIntent().getStringExtra(EXTRA_MACHINE_NAME);

        if (machineId == -1) {
            Toast.makeText(this, "기구 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar(machineName);
        initializeViews();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFilterChips();
        setupDateRangeSelector();

        loadEvents();
    }

    private void setupToolbar(String machineName) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            if (machineName != null && !machineName.isEmpty()) {
                getSupportActionBar().setTitle(machineName);
            } else {
                getSupportActionBar().setTitle("이벤트");
            }
        }
    }

    private void initializeViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptySubtitle = findViewById(R.id.emptySubtitle);
        progressBar = findViewById(R.id.progressBar);
        buttonDateRange = findViewById(R.id.buttonDateRange);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter();
        adapter.setOnEventClickListener(event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra(EXTRA_EVENT_ID, event.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(android.R.color.holo_blue_bright, null)
        );
        swipeRefreshLayout.setOnRefreshListener(this::loadEvents);
    }

    private void setupFilterChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupFilter);
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                filterEventType = null;
            } else if (checkedId == R.id.chipStart) {
                filterEventType = "start";
            } else if (checkedId == R.id.chipEnd) {
                filterEventType = "end";
            }
            loadEvents();
        });
    }

    private void setupDateRangeSelector() {
        buttonDateRange.setOnClickListener(v -> showDateFromPicker());
    }

    private void showDateFromPicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    dateFrom = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    showDateToPicker(year, month, dayOfMonth);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showDateToPicker(int startYear, int startMonth, int startDay) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    dateTo = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    normalizeDateRange(startYear, startMonth, startDay, year, month, dayOfMonth);
                    updateDateRangeButton();
                    loadEvents();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void normalizeDateRange(int startYear, int startMonth, int startDay,
                                    int endYear, int endMonth, int endDay) {
        Calendar start = Calendar.getInstance();
        start.set(startYear, startMonth, startDay, 0, 0, 0);
        Calendar end = Calendar.getInstance();
        end.set(endYear, endMonth, endDay, 0, 0, 0);

        if (end.before(start)) {
            String temp = dateFrom;
            dateFrom = dateTo;
            dateTo = temp;
        }
    }

    private void updateDateRangeButton() {
        if (dateFrom != null && dateTo != null) {
            buttonDateRange.setText("기간: " + dateFrom + " ~ " + dateTo);
        } else {
            buttonDateRange.setText("기간 선택");
        }
    }

    private void loadEvents() {
        if (!swipeRefreshLayout.isRefreshing()) {
            showLoadingState();
        }

        GymApiService.getInstance().getMachineEvents(
                machineId,
                filterEventType,
                dateFrom,
                dateTo,
                new GymApiService.EventsCallback() {
                    @Override
                    public void onSuccess(List<MachineEvent> eventList) {
                        mainHandler.post(() -> {
                            events = eventList;
                            swipeRefreshLayout.setRefreshing(false);
                            if (events.isEmpty()) {
                                showEmptyState();
                            } else {
                                showEvents();
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        mainHandler.post(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            progressBar.setVisibility(View.GONE);
                            if (GymApiService.ERROR_UNAUTHORIZED.equals(errorMessage)) {
                                handleUnauthorized();
                            } else {
                                Toast.makeText(EventListActivity.this,
                                        "이벤트를 불러오지 못했습니다",
                                        Toast.LENGTH_SHORT).show();
                                if (events.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    showEvents();
                                }
                            }
                        });
                    }
                }
        );
    }

    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void showEvents() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        adapter.setEvents(events);
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        emptyTitle.setText("아직 이벤트가 없습니다");
        emptySubtitle.setText("사용 이력이 등록되면 여기에 표시됩니다");
    }

    private void handleUnauthorized() {
        SessionManager.getInstance().logout();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
