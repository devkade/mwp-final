package com.example.photoviewer.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.photoviewer.MainActivity;
import com.example.photoviewer.R;

/**
 * Helper class for showing detection notifications
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "detection_notifications";
    private static final String CHANNEL_NAME = "Detection Notifications";
    private static final String CHANNEL_DESC = "Notifications for new object detections";
    private static final int NOTIFICATION_ID = 1001;

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();
    }

    /**
     * Create notification channel (required for Android O+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    /**
     * Show notification for new detections
     * @param count Number of new detections
     * @param firstObjectName Name of the first detected object (or null)
     */
    public void showNewDetectionNotification(int count, String firstObjectName) {
        if (count <= 0) {
            Log.w(TAG, "showNewDetectionNotification called with count <= 0");
            return;
        }

        String title = "새로운 객체 검출됨!";
        String text;

        if (count == 1 && firstObjectName != null && !firstObjectName.isEmpty()) {
            text = firstObjectName + " 검출됨";
        } else if (count == 1) {
            text = "1개의 새로운 검출";
        } else if (firstObjectName != null && !firstObjectName.isEmpty()) {
            text = firstObjectName + " 외 " + (count - 1) + "개의 새로운 검출";
        } else {
            text = count + "개의 새로운 검출";
        }

        Log.d(TAG, "Showing notification: " + title + " - " + text);

        // Intent to open MainActivity when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using default launcher icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss when tapped
            .setVibrate(new long[]{0, 250, 250, 250}); // Vibration pattern

        // Show notification
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        Log.d(TAG, "Notification shown successfully");
    }

    /**
     * Cancel all notifications
     */
    public void cancelAll() {
        notificationManager.cancelAll();
        Log.d(TAG, "All notifications cancelled");
    }
}
