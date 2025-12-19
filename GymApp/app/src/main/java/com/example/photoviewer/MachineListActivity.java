package com.example.photoviewer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.photoviewer.adapters.MachineAdapter;
import com.example.photoviewer.models.GymMachine;
import com.example.photoviewer.services.GymApiService;
import com.example.photoviewer.services.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * MachineListActivity - Equipment List Screen
 *
 * Displays a list of gym equipment from the API.
 * Supports pull-to-refresh, error handling, and navigation to equipment details.
 */
public class MachineListActivity extends AppCompatActivity {
    private static final String TAG = "MachineListActivity";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProgressBar progressBar;
    private LinearLayout errorStateLayout;
    private TextView errorMessageText;
    private Button retryButton;

    private List<GymMachine> machines = new ArrayList<>();
    private MachineAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machine_list);

        // Check if user is logged in
        if (!SessionManager.getInstance().isLoggedIn()) {
            Log.d(TAG, "User not logged in, redirecting to LoginActivity");
            redirectToLogin();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupSwipeRefresh();

        // Load machines on startup
        loadMachines();
    }

    private void initializeViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);
        errorStateLayout = findViewById(R.id.errorStateLayout);
        errorMessageText = findViewById(R.id.errorMessageText);
        retryButton = findViewById(R.id.retryButton);

        // Set toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("운동기구 목록");
        }

        // Setup retry button click listener
        retryButton.setOnClickListener(v -> loadMachines());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MachineAdapter();
        adapter.setOnMachineClickListener(this::onMachineClicked);
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
            getResources().getColor(android.R.color.holo_blue_bright, null)
        );
        swipeRefreshLayout.setOnRefreshListener(this::loadMachines);
    }

    /**
     * Load machines from API
     */
    private void loadMachines() {
        Log.d(TAG, "Loading machines...");

        // Show loading state
        showLoadingState();

        GymApiService.getInstance().getMachines(new GymApiService.MachinesCallback() {
            @Override
            public void onSuccess(List<GymMachine> machineList) {
                mainHandler.post(() -> {
                    Log.d(TAG, "Loaded " + machineList.size() + " machines");
                    machines = machineList;
                    swipeRefreshLayout.setRefreshing(false);

                    if (machines.isEmpty()) {
                        showEmptyState();
                    } else {
                        showMachines();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    Log.e(TAG, "Error loading machines: " + errorMessage);
                    swipeRefreshLayout.setRefreshing(false);

                    if (GymApiService.ERROR_UNAUTHORIZED.equals(errorMessage)) {
                        // Token expired/invalid - redirect to login
                        handleUnauthorized();
                    } else {
                        // Network or server error - show error state
                        showErrorState(errorMessage);
                    }
                });
            }
        });
    }

    /**
     * Handle machine item click - show options dialog
     */
    private void onMachineClicked(GymMachine machine) {
        Log.d(TAG, "Machine clicked: " + machine.getName());
        showMachineOptionsDialog(machine);
    }

    /**
     * Show dialog with navigation options: Event List or Statistics
     */
    private void showMachineOptionsDialog(GymMachine machine) {
        String[] options = {"이벤트 목록", "사용 통계"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(machine.getName())
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Navigate to Event List
                    Intent intent = new Intent(this, EventListActivity.class);
                    intent.putExtra(EventListActivity.EXTRA_MACHINE_ID, machine.getId());
                    intent.putExtra(EventListActivity.EXTRA_MACHINE_NAME, machine.getName());
                    startActivity(intent);
                } else if (which == 1) {
                    // Navigate to Statistics
                    Intent intent = new Intent(this, StatsActivity.class);
                    intent.putExtra(StatsActivity.EXTRA_MACHINE_ID, machine.getId());
                    intent.putExtra(StatsActivity.EXTRA_MACHINE_NAME, machine.getName());
                    startActivity(intent);
                }
            })
            .show();
    }

    /**
     * Show loading state - hide all other views, show progress bar
     */
    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.GONE);
    }

    /**
     * Show machines list - hide all other views, show RecyclerView with data
     */
    private void showMachines() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.GONE);

        adapter.setMachines(machines);
    }

    /**
     * Show empty state - no machines registered
     */
    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText("등록된 운동기구가 없습니다");
        errorStateLayout.setVisibility(View.GONE);
    }

    /**
     * Show error state with appropriate message
     */
    private void showErrorState(String errorType) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.VISIBLE);

        // Set appropriate error message based on error type
        String message;
        if (GymApiService.ERROR_SERVER.equals(errorType)) {
            message = "서버 오류가 발생했습니다";
        } else {
            message = "네트워크 오류가 발생했습니다";
        }
        errorMessageText.setText(message);
    }

    /**
     * Handle 401 Unauthorized - clear session and redirect to login
     */
    private void handleUnauthorized() {
        Log.d(TAG, "Handling unauthorized - redirecting to login");
        SessionManager.getInstance().logout();
        redirectToLogin();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_machine_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        Log.d(TAG, "Logging out user");
        SessionManager.getInstance().logout();
        redirectToLogin();
        Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
