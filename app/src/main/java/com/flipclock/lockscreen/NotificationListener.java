package com.flipclock.lockscreen;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_NOTIFICATION_PACKAGES = "notification_packages";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        updateNotificationList();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        updateNotificationList();
    }

    private void updateNotificationList() {
        try {
            StatusBarNotification[] notifications = getActiveNotifications();
            Set<String> packages = new HashSet<>();

            for (StatusBarNotification sbn : notifications) {
                if (!sbn.getPackageName().equals(getPackageName())) {
                    packages.add(sbn.getPackageName());
                }
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putStringSet(KEY_NOTIFICATION_PACKAGES, packages).apply();

            // Notify lock screen to update
            Intent intent = new Intent("com.flipclock.lockscreen.NOTIFICATION_UPDATED");
            sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error updating notifications", e);
        }
    }

    public static Set<String> getNotificationPackages(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_NOTIFICATION_PACKAGES, new HashSet<>());
    }
}
