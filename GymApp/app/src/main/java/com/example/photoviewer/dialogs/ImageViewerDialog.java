package com.example.photoviewer.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.photoviewer.BuildConfig;
import com.example.photoviewer.R;
import com.example.photoviewer.models.MachineEvent;
import com.example.photoviewer.services.SessionManager;

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
 * ImageViewerDialog - Fullscreen dialog for viewing event images
 */
public class ImageViewerDialog {
    private static final String TAG = "ImageViewerDialog";
    private static final String API_BASE_URL = BuildConfig.API_BASE_URL.replaceAll("/$", "");

    private final Context context;
    private Dialog dialog;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ImageViewerDialog(Context context) {
        this.context = context;
    }

    /**
     * Show the image viewer dialog with event details
     *
     * @param event The MachineEvent containing image URL and details
     */
    public void show(MachineEvent event) {
        if (event == null || event.getImageUrl() == null || event.getImageUrl().isEmpty()) {
            return;
        }

        dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_viewer);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivFullImage = dialog.findViewById(R.id.ivFullImage);
        ImageButton btnClose = dialog.findViewById(R.id.btnClose);
        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
        TextView tvEventType = dialog.findViewById(R.id.tvEventType);
        TextView tvDateTime = dialog.findViewById(R.id.tvDateTime);
        TextView tvPersonCount = dialog.findViewById(R.id.tvPersonCount);

        // Set event info
        String eventTypeDisplay = event.getEventTypeDisplay();
        if (eventTypeDisplay != null && !eventTypeDisplay.isEmpty()) {
            tvEventType.setText(eventTypeDisplay);
        } else {
            String eventType = event.getEventType();
            tvEventType.setText(eventType != null ? eventType.toUpperCase(Locale.US) : "EVENT");
        }

        // Format and set date/time
        String formattedDateTime = formatDateTime(event.getCapturedAt());
        tvDateTime.setText(formattedDateTime);

        // Show person count if available
        if (event.getPersonCount() > 0) {
            tvPersonCount.setVisibility(View.VISIBLE);
            tvPersonCount.setText("Detected: " + event.getPersonCount() + " person(s)");
        } else {
            tvPersonCount.setVisibility(View.GONE);
        }

        // Close button
        btnClose.setOnClickListener(v -> dismiss());

        // Click on image or background to close
        ivFullImage.setOnClickListener(v -> dismiss());

        // Load full image
        loadFullImage(event.getImageUrl(), ivFullImage, progressBar);

        dialog.show();
    }

    /**
     * Dismiss the dialog
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * Load full resolution image from URL
     */
    private void loadFullImage(String imageUrl, ImageView imageView, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);

        imageExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String fullUrl = imageUrl;
                if (!imageUrl.startsWith("http")) {
                    fullUrl = API_BASE_URL + imageUrl;
                }

                Log.d(TAG, "Loading full image from: " + fullUrl);

                URL url = new URL(fullUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                // Add authorization header for authenticated media access
                String token = SessionManager.getInstance().getToken();
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Token " + token);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to load image: " + responseCode + " from " + fullUrl);
                    mainHandler.post(() -> progressBar.setVisibility(View.GONE));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
                mainHandler.post(() -> progressBar.setVisibility(View.GONE));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Format date/time string for display
     */
    private String formatDateTime(String capturedAt) {
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

        SimpleDateFormat outputFmt = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US);
        return outputFmt.format(parsedDate);
    }
}
