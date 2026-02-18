package com.flipclock.lockscreen;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OVERLAY = 1001;
    private static final int REQUEST_CODE_LOCATION = 1002;
    private TextView statusText, fontSizeValue, locationText;
    private View statusIndicator;
    private Button btnCustomColor;
    private LinearLayout permissionCard;
    private SwitchCompat rotationSwitch, serviceToggle, volumeSwipeSwitch, weatherSwitch, islamicDateMinusOneSwitch;
    private RadioGroup madhabRadioGroup;
    private SeekBar brightnessLimitSeek;
    private TextView brightnessLimitValue;
    private Spinner fontSpinner;
    private SeekBar fontSizeSeek;
    private SharedPreferences prefs;
    private FusedLocationProviderClient fusedLocationClient;

    private String[] fontNames = {
        "Digital-7",
        "Seven Segment",
        "Orbitron Bold",
        "Monospace Bold"
    };
    private String[] fontFiles = {
        "digital-7.ttf",
        "SevenSegment.ttf",
        "Orbitron-Bold.ttf",
        "monospace"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("FlipClockPrefs", MODE_PRIVATE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        statusText = findViewById(R.id.statusText);
        statusIndicator = findViewById(R.id.statusIndicator);
        btnCustomColor = findViewById(R.id.btnCustomColor);
        permissionCard = findViewById(R.id.permissionCard);
        rotationSwitch = findViewById(R.id.rotationSwitch);
        serviceToggle = findViewById(R.id.serviceToggle);
        volumeSwipeSwitch = findViewById(R.id.volumeSwipeSwitch);
        brightnessLimitSeek = findViewById(R.id.brightnessLimitSeek);
        brightnessLimitValue = findViewById(R.id.brightnessLimitValue);
        fontSpinner = findViewById(R.id.fontSpinner);
        fontSizeSeek = findViewById(R.id.fontSizeSeek);
        fontSizeValue = findViewById(R.id.fontSizeValue);
        locationText = findViewById(R.id.locationText);
        weatherSwitch = findViewById(R.id.weatherSwitch);
        madhabRadioGroup = findViewById(R.id.madhabRadioGroup);
        islamicDateMinusOneSwitch = findViewById(R.id.islamicDateMinusOneSwitch);

        Button btnPreview = findViewById(R.id.btnPreview);
        Button btnGrantPermission = findViewById(R.id.btnGrantPermission);
        Button btnGrantNotification = findViewById(R.id.btnGrantNotification);
        Button btnGetLocation = findViewById(R.id.btnGetLocation);

        setupFontSpinner();
        setupFontSizeSeekBar();
        updateServiceStatus();
        updatePermissionVisibility();
        updateColorPreview();
        updateLocationDisplay();

        rotationSwitch.setChecked(prefs.getBoolean("rotate_180", false));
        serviceToggle.setChecked(isServiceRunning(LockScreenService.class));
        volumeSwipeSwitch.setChecked(prefs.getBoolean("volume_swipe_enabled", true));
        weatherSwitch.setChecked(prefs.getBoolean("weather_enabled", true));
        islamicDateMinusOneSwitch.setChecked(prefs.getBoolean("islamic_date_minus_one", false));

        // Setup madhab radio group (0 = Shafi, 1 = Hanafi)
        int savedMadhab = prefs.getInt("madhab", 0);
        if (savedMadhab == 1) {
            madhabRadioGroup.check(R.id.radioHanafi);
        } else {
            madhabRadioGroup.check(R.id.radioShafi);
        }

        // Setup brightness limit seekbar (default -5%)
        setupBrightnessLimitSeekBar();

        btnPreview.setOnClickListener(v -> previewLockScreen());
        btnGrantPermission.setOnClickListener(v -> requestOverlayPermission());
        btnGrantNotification.setOnClickListener(v -> requestNotificationAccess());
        btnCustomColor.setOnClickListener(v -> showColorPicker());
        btnGetLocation.setOnClickListener(v -> requestLocation());

        rotationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("rotate_180", isChecked).apply();
            Toast.makeText(this, "Rotation " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        volumeSwipeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("volume_swipe_enabled", isChecked).apply();
            Toast.makeText(this, "Volume swipe " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        weatherSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("weather_enabled", isChecked).apply();
            Toast.makeText(this, "Weather " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        madhabRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int madhab = (checkedId == R.id.radioHanafi) ? 1 : 0;
            prefs.edit().putInt("madhab", madhab).apply();
            String madhabName = (madhab == 1) ? "Hanafi" : "Shafi";
            Toast.makeText(this, "Asr calculation: " + madhabName, Toast.LENGTH_SHORT).show();
        });

        islamicDateMinusOneSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("islamic_date_minus_one", isChecked).apply();
            Toast.makeText(this, "Islamic date -1 day " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        serviceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableLockScreen();
            } else {
                disableLockScreen();
            }
        });

        // Auto-enable lock screen service on first run
        boolean firstRun = prefs.getBoolean("first_run", true);
        if (firstRun) {
            prefs.edit().putBoolean("first_run", false).apply();
            enableLockScreen();
        }
    }

    private void setupFontSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fontNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontSpinner.setAdapter(adapter);

        int savedFont = prefs.getInt("font_index", 0);
        fontSpinner.setSelection(savedFont);

        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("font_index", position).apply();
                prefs.edit().putString("font_file", fontFiles[position]).apply();
                Toast.makeText(MainActivity.this, "Font changed to " + fontNames[position], Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFontSizeSeekBar() {
        int savedSize = prefs.getInt("font_size", 200);
        int progress = (savedSize - 100) / 2;
        fontSizeSeek.setProgress(progress);
        fontSizeValue.setText(savedSize + "sp");

        fontSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = 100 + (progress * 2);
                fontSizeValue.setText(size + "sp");
                if (fromUser) {
                    prefs.edit().putInt("font_size", size).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int size = 100 + (seekBar.getProgress() * 2);
                Toast.makeText(MainActivity.this, "Font size: " + size + "sp", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBrightnessLimitSeekBar() {
        // Default is -5%, stored as positive 5 (seekbar progress)
        int savedLimit = prefs.getInt("brightness_limit", 5);
        brightnessLimitSeek.setProgress(savedLimit);
        brightnessLimitValue.setText("-" + savedLimit + "%");

        brightnessLimitSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brightnessLimitValue.setText("-" + progress + "%");
                if (fromUser) {
                    prefs.edit().putInt("brightness_limit", progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int limit = seekBar.getProgress();
                Toast.makeText(MainActivity.this, "Brightness limit: -" + limit + "%", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
        updatePermissionVisibility();
    }

    private void showColorPicker() {
        View dialogView = getLayoutInflater().inflate(R.layout.color_picker_dialog, null);

        final View colorDisplay = dialogView.findViewById(R.id.colorDisplay);
        SeekBar seekRed = dialogView.findViewById(R.id.seekRed);
        SeekBar seekGreen = dialogView.findViewById(R.id.seekGreen);
        SeekBar seekBlue = dialogView.findViewById(R.id.seekBlue);
        TextView textRed = dialogView.findViewById(R.id.textRed);
        TextView textGreen = dialogView.findViewById(R.id.textGreen);
        TextView textBlue = dialogView.findViewById(R.id.textBlue);
        TextView hexValue = dialogView.findViewById(R.id.hexValue);

        String currentColor = prefs.getString("clock_color", "#FFFFFF");
        int color = Color.parseColor(currentColor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        seekRed.setProgress(red);
        seekGreen.setProgress(green);
        seekBlue.setProgress(blue);
        textRed.setText("R: " + red);
        textGreen.setText("G: " + green);
        textBlue.setText("B: " + blue);
        hexValue.setText(currentColor);
        colorDisplay.setBackgroundColor(color);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int r = seekRed.getProgress();
                int g = seekGreen.getProgress();
                int b = seekBlue.getProgress();
                textRed.setText("R: " + r);
                textGreen.setText("G: " + g);
                textBlue.setText("B: " + b);
                String hex = String.format("#%02X%02X%02X", r, g, b);
                hexValue.setText(hex);
                colorDisplay.setBackgroundColor(Color.rgb(r, g, b));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekRed.setOnSeekBarChangeListener(listener);
        seekGreen.setOnSeekBarChangeListener(listener);
        seekBlue.setOnSeekBarChangeListener(listener);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Choose Clock Color")
                .setView(dialogView)
                .setPositiveButton("Apply", (d, which) -> {
                    int r = seekRed.getProgress();
                    int g = seekGreen.getProgress();
                    int b = seekBlue.getProgress();
                    String hexColor = String.format("#%02X%02X%02X", r, g, b);
                    setClockColor(hexColor);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Style the buttons to be visible (not white)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#4CAF50"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#F44336"));
    }

    private void setClockColor(String color) {
        prefs.edit().putString("clock_color", color).apply();
        updateColorPreview();
        Toast.makeText(this, "Clock color updated", Toast.LENGTH_SHORT).show();
    }

    private void updateColorPreview() {
        String color = prefs.getString("clock_color", "#FFFFFF");
        int colorInt = Color.parseColor(color);
        btnCustomColor.setBackgroundColor(colorInt);

        // Calculate luminance to determine if we should use white or black text
        int red = Color.red(colorInt);
        int green = Color.green(colorInt);
        int blue = Color.blue(colorInt);
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;

        // Use white text on dark colors, black text on light colors
        btnCustomColor.setTextColor(luminance > 0.5 ? Color.BLACK : Color.WHITE);
    }

    private void updatePermissionVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                permissionCard.setVisibility(View.GONE);
            } else {
                permissionCard.setVisibility(View.VISIBLE);
            }
        } else {
            permissionCard.setVisibility(View.GONE);
        }
    }

    private void requestNotificationAccess() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Please enable notification access for this app", Toast.LENGTH_LONG).show();
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY);
        }
    }

    private void requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void enableLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_LONG).show();
                return;
            }

            // Check for WRITE_SETTINGS permission (needed for brightness control)
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "Granting system settings permission for brightness control...", Toast.LENGTH_LONG).show();
                requestWriteSettingsPermission();
                // Continue anyway - brightness won't work until permission is granted
            }
        }

        Intent serviceIntent = new Intent(this, LockScreenService.class);

        // Use startForegroundService for Android 8.0+ (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "Lock screen enabled", Toast.LENGTH_SHORT).show();
        updateServiceStatus();
    }

    private void disableLockScreen() {
        // Prevent service from auto-restarting
        LockScreenService.setShouldRestart(false);

        Intent serviceIntent = new Intent(this, LockScreenService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Lock screen disabled", Toast.LENGTH_SHORT).show();
        updateServiceStatus();
    }

    private void previewLockScreen() {
        Intent intent = new Intent(this, LockScreenActivity.class);
        startActivity(intent);
    }

    private void updateServiceStatus() {
        boolean running = isServiceRunning(LockScreenService.class);

        if (running) {
            statusText.setText("Running");
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor("#4CAF50"));
            statusIndicator.setBackground(shape);
        } else {
            statusText.setText("Stopped");
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor("#F44336"));
            statusIndicator.setBackground(shape);
        }

        // Update toggle without triggering listener
        serviceToggle.setOnCheckedChangeListener(null);
        serviceToggle.setChecked(running);
        serviceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableLockScreen();
            } else {
                disableLockScreen();
            }
        });
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY) {
            updatePermissionVisibility();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permission granted! You can now enable the lock screen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void requestLocation() {
        // First check if location services are enabled
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            // GPS provider not available
        }

        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            // Network provider not available
        }

        if (!gpsEnabled && !networkEnabled) {
            // Location services are disabled, show dialog
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Location Services Disabled")
                    .setMessage("Please enable location services to get accurate prayer times.\n\nWould you like to open location settings?")
                    .setPositiveButton("Open Settings", (d, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                        Toast.makeText(this, "Please enable location and come back to the app", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create();
            dialog.show();

            // Style the buttons to be visible
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#4CAF50"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#F44336"));
            return;
        }

        // Location is enabled, check for permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            prefs.edit()
                                    .putFloat("latitude", (float) latitude)
                                    .putFloat("longitude", (float) longitude)
                                    .apply();

                            updateLocationDisplay();
                            Toast.makeText(MainActivity.this,
                                    "Location saved successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Could not get location. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateLocationDisplay() {
        float latitude = prefs.getFloat("latitude", 0);
        float longitude = prefs.getFloat("longitude", 0);

        if (latitude != 0 && longitude != 0) {
            locationText.setText(String.format("Lat: %.4f, Lon: %.4f", latitude, longitude));
        } else {
            locationText.setText("Location not set");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
