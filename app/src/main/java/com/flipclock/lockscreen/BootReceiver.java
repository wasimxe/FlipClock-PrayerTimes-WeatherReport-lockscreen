package com.flipclock.lockscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            android.util.Log.d("BootReceiver", "Boot completed, starting lock screen service");

            // Always start the lock screen service on boot
            SharedPreferences prefs = context.getSharedPreferences("FlipClockPrefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("service_enabled", true).apply();

            Intent serviceIntent = new Intent(context, LockScreenService.class);

            // Use startForegroundService for Android 8.0+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                android.util.Log.d("BootReceiver", "Started foreground service (Android 8.0+)");
            } else {
                context.startService(serviceIntent);
                android.util.Log.d("BootReceiver", "Started service (Android < 8.0)");
            }
        }
    }
}
