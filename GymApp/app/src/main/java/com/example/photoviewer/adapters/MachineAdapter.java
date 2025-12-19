package com.example.photoviewer.adapters;

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
import com.example.photoviewer.models.GymMachine;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MachineAdapter - RecyclerView adapter for displaying gym machines
 *
 * Displays machine info including thumbnail, name, location, and event count.
 * Handles thumbnail loading in background thread.
 */
public class MachineAdapter extends RecyclerView.Adapter<MachineAdapter.MachineViewHolder> {
    private static final String TAG = "MachineAdapter";
    private static final String API_BASE_URL = BuildConfig.API_BASE_URL.replaceAll("/$", "");

    private List<GymMachine> machines = new ArrayList<>();
    private OnMachineClickListener clickListener;
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Interface for handling machine item clicks
     */
    public interface OnMachineClickListener {
        void onMachineClick(GymMachine machine);
    }

    public MachineAdapter() {
    }

    public void setOnMachineClickListener(OnMachineClickListener listener) {
        this.clickListener = listener;
    }

    public void setMachines(List<GymMachine> machines) {
        this.machines = machines != null ? machines : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MachineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_machine, parent, false);
        return new MachineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MachineViewHolder holder, int position) {
        GymMachine machine = machines.get(position);
        holder.bind(machine);
    }

    @Override
    public int getItemCount() {
        return machines.size();
    }

    /**
     * ViewHolder for machine items
     */
    class MachineViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvName;
        private final TextView tvDescription;
        private final TextView tvLocation;
        private final TextView tvStatus;
        private final View statusDot;

        MachineViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvName = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            statusDot = itemView.findViewById(R.id.statusDot);

            // Set click listener on the whole item
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onMachineClick(machines.get(pos));
                }
            });
        }

        void bind(GymMachine machine) {
            tvName.setText(machine.getName());
            tvLocation.setText(machine.getLocation());

            // Show event count in description
            int eventCount = machine.getEventCount();
            tvDescription.setText("이벤트 " + eventCount + "건");

            // Show status
            tvStatus.setText("Active");

            // Load thumbnail image in background
            loadThumbnail(machine.getThumbnailUrl(), ivThumbnail);
        }
    }

    /**
     * Load thumbnail image from URL using HttpURLConnection
     * Runs in background thread, updates UI on main thread
     */
    private void loadThumbnail(String thumbnailUrl, ImageView imageView) {
        // Set placeholder first
        imageView.setImageResource(R.drawable.placeholder_image);

        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            return;
        }

        // Store reference to check if view was recycled
        imageView.setTag(thumbnailUrl);

        imageExecutor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                // Build full URL if relative
                String fullUrl = thumbnailUrl;
                if (!thumbnailUrl.startsWith("http")) {
                    fullUrl = API_BASE_URL + thumbnailUrl;
                }

                Log.d(TAG, "Loading thumbnail: " + fullUrl);

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
                        mainHandler.post(() -> {
                            // Check if view is still showing same image URL
                            if (thumbnailUrl.equals(imageView.getTag())) {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "Failed to load thumbnail: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading thumbnail: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}
