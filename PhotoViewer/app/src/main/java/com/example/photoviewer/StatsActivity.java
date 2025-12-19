package com.example.photoviewer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;

import com.example.photoviewer.models.MachineStats;
import com.example.photoviewer.services.GymApiService;
import com.example.photoviewer.services.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * StatsActivity - Statistics Screen for a specific machine
 *
 * Displays usage statistics with metric cards and daily usage bar chart.
 * Supports date range selection with MaterialDatePicker.
 */
public class StatsActivity extends AppCompatActivity {
    private static final String TAG = "StatsActivity";

    public static final String EXTRA_MACHINE_ID = "machine_id";
    public static final String EXTRA_MACHINE_NAME = "machine_name";

    private ScrollView contentScrollView;
    private ProgressBar progressBar;
    private View emptyStateLayout;
    private View errorStateLayout;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;
    private MaterialButton btnDateRange;
    private TextView tvTotalStarts;
    private TextView tvTotalEnds;
    private BarChart chartDailyUsage;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int machineId = -1;
    private String machineName = "";
    private String dateFrom;
    private String dateTo;

    private static final SimpleDateFormat API_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT =
            new SimpleDateFormat("MM/dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Check authentication
        if (!SessionManager.getInstance().isLoggedIn()) {
            redirectToLogin();
            return;
        }

        // Get intent extras
        machineId = getIntent().getIntExtra(EXTRA_MACHINE_ID, -1);
        machineName = getIntent().getStringExtra(EXTRA_MACHINE_NAME);

        if (machineId == -1) {
            finish();
            return;
        }

        // Set default date range (last 7 days)
        setDefaultDateRange();

        setupToolbar();
        initializeViews();
        setupChart();
        setupDateRangePicker();
        setupRetryButton();

        loadStats();
    }

    private void setDefaultDateRange() {
        Calendar calendar = Calendar.getInstance();
        dateTo = API_DATE_FORMAT.format(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, -6); // 7 days including today
        dateFrom = API_DATE_FORMAT.format(calendar.getTime());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            String title = (machineName != null && !machineName.isEmpty())
                    ? machineName + " 통계"
                    : "통계";
            getSupportActionBar().setTitle(title);
        }
    }

    private void initializeViews() {
        contentScrollView = findViewById(R.id.contentScrollView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        errorStateLayout = findViewById(R.id.errorStateLayout);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetry = findViewById(R.id.btnRetry);
        btnDateRange = findViewById(R.id.btnDateRange);
        tvTotalStarts = findViewById(R.id.tvTotalStarts);
        tvTotalEnds = findViewById(R.id.tvTotalEnds);
        chartDailyUsage = findViewById(R.id.chartDailyUsage);

        updateDateRangeButtonText();
    }

    private void setupChart() {
        // Configure BarChart
        chartDailyUsage.setDrawGridBackground(false);
        chartDailyUsage.setDrawBarShadow(false);
        chartDailyUsage.setHighlightFullBarEnabled(false);
        chartDailyUsage.getDescription().setEnabled(false);
        chartDailyUsage.getLegend().setEnabled(false);
        chartDailyUsage.setTouchEnabled(true);
        chartDailyUsage.setDragEnabled(false);
        chartDailyUsage.setScaleEnabled(false);
        chartDailyUsage.setPinchZoom(false);

        // Configure X-axis
        XAxis xAxis = chartDailyUsage.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#666666"));

        // Configure Y-axis (left)
        YAxis leftAxis = chartDailyUsage.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(Color.parseColor("#666666"));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));

        // Disable right Y-axis
        chartDailyUsage.getAxisRight().setEnabled(false);
    }

    private void setupDateRangePicker() {
        btnDateRange.setOnClickListener(v -> showDateRangePicker());
    }

    private void showDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("기간 선택");

        // Set default selection
        try {
            Date fromDate = API_DATE_FORMAT.parse(dateFrom);
            Date toDate = API_DATE_FORMAT.parse(dateTo);
            if (fromDate != null && toDate != null) {
                builder.setSelection(new Pair<>(fromDate.getTime(), toDate.getTime()));
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dates for picker: " + e.getMessage());
        }

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null && selection.first != null && selection.second != null) {
                dateFrom = API_DATE_FORMAT.format(new Date(selection.first));
                dateTo = API_DATE_FORMAT.format(new Date(selection.second));
                updateDateRangeButtonText();
                loadStats();
            }
        });

        picker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void updateDateRangeButtonText() {
        if (dateFrom != null && dateTo != null) {
            btnDateRange.setText(dateFrom + " ~ " + dateTo);
        } else {
            btnDateRange.setText("기간 선택");
        }
    }

    private void setupRetryButton() {
        btnRetry.setOnClickListener(v -> loadStats());
    }

    private void loadStats() {
        showLoadingState();

        GymApiService.getInstance().getMachineStats(
                machineId,
                dateFrom,
                dateTo,
                new GymApiService.StatsCallback() {
                    @Override
                    public void onSuccess(MachineStats stats) {
                        mainHandler.post(() -> {
                            if (stats.isEmpty()) {
                                showEmptyState();
                            } else {
                                populateUI(stats);
                                showContentState();
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        mainHandler.post(() -> {
                            if (GymApiService.ERROR_UNAUTHORIZED.equals(errorMessage)) {
                                handleUnauthorized();
                            } else {
                                showErrorState(errorMessage);
                            }
                        });
                    }
                }
        );
    }

    private void populateUI(MachineStats stats) {
        // Update metric cards
        tvTotalStarts.setText(stats.getTotalStarts() + "회");
        tvTotalEnds.setText(stats.getTotalEnds() + "회");

        // Update chart
        updateChart(stats.getDailyUsage());
    }

    private void updateChart(List<MachineStats.DailyUsage> dailyUsage) {
        if (dailyUsage == null || dailyUsage.isEmpty()) {
            chartDailyUsage.clear();
            chartDailyUsage.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < dailyUsage.size(); i++) {
            MachineStats.DailyUsage day = dailyUsage.get(i);
            entries.add(new BarEntry(i, day.getCount()));

            // Format date as MM/DD for X-axis label
            try {
                Date date = API_DATE_FORMAT.parse(day.getDate());
                if (date != null) {
                    labels.add(DISPLAY_DATE_FORMAT.format(date));
                } else {
                    labels.add(day.getDate());
                }
            } catch (ParseException e) {
                labels.add(day.getDate());
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "일별 사용량");
        dataSet.setColor(Color.parseColor("#12c0e2"));
        dataSet.setValueTextColor(Color.parseColor("#333333"));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);

        chartDailyUsage.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartDailyUsage.getXAxis().setLabelCount(labels.size());
        chartDailyUsage.setData(barData);
        chartDailyUsage.invalidate();
    }

    // State management methods

    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        contentScrollView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.GONE);
    }

    private void showContentState() {
        progressBar.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        errorStateLayout.setVisibility(View.GONE);
    }

    private void showErrorState(String errorType) {
        progressBar.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.VISIBLE);

        String message;
        if (GymApiService.ERROR_SERVER.equals(errorType)) {
            message = "서버 오류가 발생했습니다";
        } else {
            message = "네트워크 오류가 발생했습니다";
        }
        tvErrorMessage.setText(message);
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
