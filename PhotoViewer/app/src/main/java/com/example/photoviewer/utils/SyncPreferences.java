package com.example.photoviewer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Helper class for managing sync-related preferences
 * Tracks the last seen post ID to detect new posts
 */
public class SyncPreferences {
    private static final String TAG = "SyncPreferences";
    private static final String PREF_NAME = "PhotoViewerSyncPrefs";
    private static final String KEY_LAST_SEEN_POST_ID = "lastSeenPostId";
    private static final String KEY_LAST_SYNC_TIMESTAMP = "lastSyncTimestamp";

    private final SharedPreferences prefs;

    public SyncPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get the ID of the last seen post
     * @return Last seen post ID, or 0 if never set
     */
    public int getLastSeenPostId() {
        int lastId = prefs.getInt(KEY_LAST_SEEN_POST_ID, 0);
        Log.d(TAG, "getLastSeenPostId: " + lastId);
        return lastId;
    }

    /**
     * Update the last seen post ID
     * @param postId The highest post ID currently visible
     */
    public void setLastSeenPostId(int postId) {
        Log.d(TAG, "setLastSeenPostId: " + postId);
        prefs.edit()
            .putInt(KEY_LAST_SEEN_POST_ID, postId)
            .putLong(KEY_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
            .apply();
    }

    /**
     * Get the timestamp of the last sync
     * @return Timestamp in milliseconds, or 0 if never synced
     */
    public long getLastSyncTimestamp() {
        return prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0);
    }

    /**
     * Clear all sync preferences (e.g., on logout)
     */
    public void clear() {
        Log.d(TAG, "Clearing sync preferences");
        prefs.edit().clear().apply();
    }
}
