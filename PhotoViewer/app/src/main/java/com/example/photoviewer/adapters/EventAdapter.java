package com.example.photoviewer.adapters;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photoviewer.BuildConfig;
import com.example.photoviewer.R;
import com.example.photoviewer.models.MachineEvent;
import com.google.android.material.chip.Chip;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EventAdapter - RecyclerView adapter for displaying machine events
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private static final String TAG = "EventAdapter";
    private static final String API_BASE_URL = BuildConfig.API_BASE_URL.replaceAll("/$", "");
    private static final String COLOR_START = "#28A745";
    private static final String COLOR_END = "#DC3545";

    private List<MachineEvent> events = new ArrayList<>();
    private OnEventClickListener clickListener;
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Interface for handling event item clicks
     */
    public interface OnEventClickListener {
        void onEventClick(MachineEvent event);
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.clickListener = listener;
    }

    public void setEvents(List<MachineEvent> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        MachineEvent event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for event items
     */
    class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final Chip chipEventType;
        private final TextView tvCapturedAt;
        private final TextView tvPersonCount;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            chipEventType = itemView.findViewById(R.id.chipEventType);
            tvCapturedAt = itemView.findViewById(R.id.tvCapturedAt);
            tvPersonCount = itemView.findViewById(R.id.tvPersonCount);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onEventClick(events.get(pos));
                }
            });
        }

        void bind(MachineEvent event) {
            String eventType = event.getEventType();
            String eventTypeDisplay = event.getEventTypeDisplay();
            String chipText = !eventTypeDisplay.isEmpty()
                    ? eventTypeDisplay
                    : (eventType != null ? eventType.toUpperCase(Locale.US) : "");
            chipEventType.setText(chipText);

            if ("start".equalsIgnoreCase(eventType)) {
                chipEventType.setChipBackgroundColor(
                        ColorStateList.valueOf(android.graphics.Color.parseColor(COLOR_START)));
                chipEventType.setTextColor(android.graphics.Color.WHITE);
            } else if ("end".equalsIgnoreCase(eventType)) {
                chipEventType.setChipBackgroundColor(
                        ColorStateList.valueOf(android.graphics.Color.parseColor(COLOR_END)));
                chipEventType.setTextColor(android.graphics.Color.WHITE);
            } else {
                chipEventType.setChipBackgroundColor(
                        ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0")));
                chipEventType.setTextColor(android.graphics.Color.BLACK);
            }

            tvCapturedAt.setText(formatCapturedAt(event.getCapturedAt()));
            tvPersonCount.setText("감지 인원: " + event.getPersonCount() + "명");

            loadThumbnail(event.getImageUrl(), ivThumbnail);
        }
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

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA);
        return formatter.format(parsedDate);
    }

    /**
     * Load thumbnail image from URL using HttpURLConnection
     */
    private void loadThumbnail(String imageUrl, ImageView imageView) {
        imageView.setImageResource(R.drawable.placeholder_image);

        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        imageView.setTag(imageUrl);

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
                        String finalUrl = imageUrl;
                        mainHandler.post(() -> {
                            if (finalUrl.equals(imageView.getTag())) {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
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
}
