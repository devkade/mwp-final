package com.example.photoviewer.workers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.photoviewer.BuildConfig;
import com.example.photoviewer.services.SessionManager;
import com.example.photoviewer.utils.NotificationHelper;
import com.example.photoviewer.utils.SyncPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Background worker that periodically checks for new posts
 * Runs every 15 minutes when app is in background
 */
public class BackgroundSyncWorker extends Worker {
    private static final String TAG = "BackgroundSyncWorker";

    public BackgroundSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "BackgroundSyncWorker started");

        // Check if user is logged in
        if (!SessionManager.getInstance().isLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping sync");
            return Result.success();
        }

        try {
            // Initialize helpers
            Context context = getApplicationContext();
            SyncPreferences syncPrefs = new SyncPreferences(context);
            NotificationHelper notificationHelper = new NotificationHelper(context);

            // Fetch posts from server
            String siteUrl = BuildConfig.API_BASE_URL;
            URL url = new URL(siteUrl + "api_root/Post/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Token " + SessionManager.getInstance().getToken());
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                is.close();
                conn.disconnect();

                // Parse JSON and check for new posts
                JSONArray aryJson = new JSONArray(result.toString());
                int lastSeenId = syncPrefs.getLastSeenPostId();
                int maxId = 0;
                int newPostCount = 0;
                String firstNewObjectName = null;

                for (int i = 0; i < aryJson.length(); i++) {
                    JSONObject post_json = aryJson.getJSONObject(i);
                    int id = post_json.optInt("id", -1);
                    if (id > maxId) {
                        maxId = id;
                    }
                    if (id > lastSeenId) {
                        newPostCount++;
                        if (firstNewObjectName == null) {
                            firstNewObjectName = post_json.optString("title", "");
                        }
                    }
                }

                Log.d(TAG, "Sync complete: lastSeenId=" + lastSeenId +
                      ", maxId=" + maxId + ", newPostCount=" + newPostCount);

                // Show notification if new posts found
                if (newPostCount > 0) {
                    Log.d(TAG, "New posts detected, showing notification");
                    notificationHelper.showNewDetectionNotification(newPostCount, firstNewObjectName);
                    syncPrefs.setLastSeenPostId(maxId);
                }

                return Result.success();
            } else {
                Log.e(TAG, "Sync failed with HTTP code: " + responseCode);
                return Result.retry();
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error in background sync: " + e.getMessage());
            e.printStackTrace();
            return Result.retry();
        }
    }
}
