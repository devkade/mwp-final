package com.example.photoviewer;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.photoviewer.models.MachineEvent;
import com.example.photoviewer.services.GymApiService;
import com.example.photoviewer.services.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EventDetailActivity - Displays detailed information about a specific usage event
 */
public class EventDetailActivity extends AppCompatActivity {
    private static final String TAG = "EventDetailActivity";
    private static final String API_BASE_URL = BuildConfig.API_BASE_URL.replaceAll("/$", "");
    private static final String COLOR_START = "#28A745";
    private static final String COLOR_END = "#DC3545";

    private ScrollView contentScrollView;
    private ProgressBar progressBar;
    private View errorStateLayout;
    private MaterialButton buttonRetry;

    private ImageView ivEventImage;
    private Chip chipEventType;
    private TextView tvCapturedAt;
    private TextView tvPersonCount;
    private TextView tvEventSummary;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

    private int eventId = -1;
    private MachineEvent currentEvent;
    private Bitmap loadedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        if (!SessionManager.getInstance().isLoggedIn()) {
            redirectToLogin();
            return;
        }

        eventId = getIntent().getIntExtra(EventListActivity.EXTRA_EVENT_ID, -1);

        if (eventId == -1) {
            Toast.makeText(this, "이벤트 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        setupRetryButton();
        setupImageClickListener();

        loadEventDetail();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("이벤트 상세");
        }
    }

    private void initializeViews() {
        contentScrollView = findViewById(R.id.contentScrollView);
        progressBar = findViewById(R.id.progressBar);
        errorStateLayout = findViewById(R.id.errorStateLayout);
        buttonRetry = findViewById(R.id.buttonRetry);

        ivEventImage = findViewById(R.id.ivEventImage);
        chipEventType = findViewById(R.id.chipEventType);
        tvCapturedAt = findViewById(R.id.tvCapturedAt);
        tvPersonCount = findViewById(R.id.tvPersonCount);
        tvEventSummary = findViewById(R.id.tvEventSummary);

        // Set 4:3 aspect ratio for image
        ivEventImage.post(() -> {
            int width = ivEventImage.getWidth();
            ViewGroup.LayoutParams params = ivEventImage.getLayoutParams();
            params.height = (width * 3) / 4;
            ivEventImage.setLayoutParams(params);
        });
    }

    private void setupRetryButton() {
        buttonRetry.setOnClickListener(v -> loadEventDetail());
    }

    private void setupImageClickListener() {
        ivEventImage.setOnClickListener(v -> {
            if (loadedBitmap != null) {
                showFullscreenImage();
            }
        });
    }

    private void loadEventDetail() {
        showLoadingState();

        GymApiService.getInstance().getEventDetail(eventId, new GymApiService.EventDetailCallback() {
            @Override
            public void onSuccess(MachineEvent event) {
                mainHandler.post(() -> {
                    currentEvent = event;
                    populateUI(event);
                    showContentState();

                    // Update toolbar title with machine name
                    if (getSupportActionBar() != null && event.getMachineName() != null
                            && !event.getMachineName().isEmpty()) {
                        getSupportActionBar().setTitle(event.getMachineName());
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                mainHandler.post(() -> {
                    if (GymApiService.ERROR_UNAUTHORIZED.equals(errorMessage)) {
                        handleUnauthorized();
                    } else {
                        showErrorState();
                    }
                });
            }
        });
    }

    private void populateUI(MachineEvent event) {
        // Event type chip
        String eventType = event.getEventType();
        String eventTypeDisplay = event.getEventTypeDisplay();
        String chipText = !eventTypeDisplay.isEmpty()
                ? eventTypeDisplay
                : (eventType != null ? eventType.toUpperCase(Locale.US) : "");
        chipEventType.setText(chipText);

        if ("start".equalsIgnoreCase(eventType)) {
            chipEventType.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(COLOR_START)));
            chipEventType.setTextColor(Color.WHITE);
        } else if ("end".equalsIgnoreCase(eventType)) {
            chipEventType.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(COLOR_END)));
            chipEventType.setTextColor(Color.WHITE);
        } else {
            chipEventType.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            chipEventType.setTextColor(Color.BLACK);
        }

        // Timestamp
        tvCapturedAt.setText(formatCapturedAt(event.getCapturedAt()));

        // Person count
        tvPersonCount.setText(event.getPersonCount() + "명");

        // Event summary
        tvEventSummary.setText(getEventSummaryText(eventType));

        // Load image
        loadEventImage(event.getImageUrl());
    }

    private String formatCapturedAt(String capturedAt) {
        if (capturedAt == null || capturedAt.isEmpty()) {
            return "";
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        };

        Date parsedDate = null;
        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.US);
                if (pattern.endsWith("'Z'")) {
                    parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                parsedDate = parser.parse(capturedAt);
                if (parsedDate != null) {
                    break;
                }
            } catch (ParseException ignored) {
            }
        }

        if (parsedDate == null) {
            return capturedAt;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy년 MM월 dd일 a h:mm", Locale.KOREA);
        return formatter.format(parsedDate);
    }

    private String getEventSummaryText(String eventType) {
        if ("start".equalsIgnoreCase(eventType)) {
            return "운동기구 사용이 시작되었습니다.";
        } else if ("end".equalsIgnoreCase(eventType)) {
            return "운동기구 사용이 종료되었습니다.";
        }
        return "";
    }

    private void loadEventImage(String imageUrl) {
        ivEventImage.setImageResource(R.drawable.placeholder_image);

        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        imageExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String fullUrl = imageUrl;
                if (!imageUrl.startsWith("http")) {
                    fullUrl = API_BASE_URL + imageUrl;
                }

                URL url = new URL(fullUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    if (bitmap != null) {
                        loadedBitmap = bitmap;
                        mainHandler.post(() -> ivEventImage.setImageBitmap(bitmap));
                    }
                } else {
                    Log.e(TAG, "Failed to load event image: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading event image: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void showFullscreenImage() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_fullscreen_image);

        ImageView ivFullscreen = dialog.findViewById(R.id.ivFullscreenImage);
        ImageButton buttonClose = dialog.findViewById(R.id.buttonClose);

        if (loadedBitmap != null) {
            ivFullscreen.setImageBitmap(loadedBitmap);
        }

        buttonClose.setOnClickListener(v -> dialog.dismiss());
        ivFullscreen.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        contentScrollView.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.GONE);
    }

    private void showContentState() {
        progressBar.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.VISIBLE);
        errorStateLayout.setVisibility(View.GONE);
    }

    private void showErrorState() {
        progressBar.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        errorStateLayout.setVisibility(View.VISIBLE);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageExecutor.shutdown();
        if (loadedBitmap != null && !loadedBitmap.isRecycled()) {
            loadedBitmap.recycle();
            loadedBitmap = null;
        }
    }
}
