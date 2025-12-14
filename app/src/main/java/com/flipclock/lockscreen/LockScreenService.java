package com.flipclock.lockscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

public class LockScreenService extends Service {

    private static final String CHANNEL_ID = "LockScreenServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static boolean shouldRestart = true;
    private ScreenReceiver screenReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create foreground notification to keep service running
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // Register screen on/off receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldRestart = true; // Always allow restart when service starts
        // START_STICKY ensures service restarts if killed by system
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }

        // Restart service if destroyed (unless explicitly stopped by user)
        if (shouldRestart) {
            Intent restartIntent = new Intent(getApplicationContext(), LockScreenService.class);

            // Use startForegroundService for Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        }
    }

    public static void setShouldRestart(boolean restart) {
        shouldRestart = restart;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Lock Screen Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps lock screen active");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setContentTitle("Flip Clock Lock Screen")
            .setContentText("Lock screen is active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    // Screen turned off, show lock screen immediately
                    Intent lockIntent = new Intent(context, LockScreenActivity.class);
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(lockIntent);
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    // Screen turned on, also show lock screen
                    Intent lockIntent = new Intent(context, LockScreenActivity.class);
                    lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(lockIntent);
                } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                    // User unlocked the device
                }
            }
        }
    }
}
