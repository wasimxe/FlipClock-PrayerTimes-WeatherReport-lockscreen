package com.flipclock.lockscreen;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.batoulapps.adhan.Prayer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LockScreenActivity extends Activity {

    private TextView hourTens, hourOnes, minuteTens, minuteOnes, dateText, colonText;
    private LinearLayout notificationContainer;
    private RelativeLayout mainLayout;
    private Handler handler;
    private Runnable timeUpdater;
    private Runnable dateUpdater;
    private Runnable prayerUpdater;
    private Runnable leftInfoUpdater;
    private int lastMinute = -1;
    private SharedPreferences prefs;
    private boolean showIslamicDate = false;
    private boolean isCharging = true;
    private BroadcastReceiver batteryReceiver;
    private GestureDetector gestureDetector;

    // Prayer times UI elements
    private RelativeLayout prayerTimesOverlay;
    private TextView nextPrayerLabel, nextPrayerName, nextPrayerCountdown;
    private TextView leftInfoLabel, leftInfoValue;
    private TextView prayerTimeFajr, prayerTimeDhuhr, prayerTimeAsr, prayerTimeMaghrib, prayerTimeIsha;
    private TextView prayerNameFajr, prayerNameDhuhr, prayerNameAsr, prayerNameMaghrib, prayerNameIsha;
    private PrayerTimeCalculator prayerCalculator;
    private int leftInfoIndex = 0;

    // Weather UI elements
    private ScrollView weatherOverlay;
    private WeatherFetcher weatherFetcher;
    private WeatherData currentWeatherData;
    private Runnable weatherUpdater;

    // Brightness and Volume indicator UI elements
    private LinearLayout brightnessIndicator;
    private LinearLayout volumeIndicator;
    private android.widget.ProgressBar brightnessProgressBar;
    private android.widget.ProgressBar volumeProgressBar;
    private TextView brightnessPercentage;
    private TextView volumePercentage;
    private Runnable hideBrightnessIndicator;
    private Runnable hideVolumeIndicator;

    // Dark overlay for extra dimming beyond 0% brightness
    private View darkOverlay;
    private float currentOverlayAlpha = 0f;

    // View state: 0=Clock, 1=Prayer, 2=Weather
    private int currentViewState = 0;

    // Urdu day names
    private String[] urduDays = {"اتوار", "پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ"};
    // Urdu month names (Gregorian)
    private String[] urduMonths = {"جنوری", "فروری", "مارچ", "اپریل", "مئی", "جون",
                                    "جولائی", "اگست", "ستمبر", "اکتوبر", "نومبر", "دسمبر"};
    // Urdu Islamic month names
    private String[] urduIslamicMonths = {"محرم", "صفر", "ربیع الاول", "ربیع الثانی",
                                          "جمادی الاول", "جمادی الثانی", "رجب", "شعبان",
                                          "رمضان", "شوال", "ذوالقعدہ", "ذوالحجہ"};

    // Helper method to convert English digits to Urdu digits
    private String toUrduDigits(int number) {
        String[] urduDigits = {"۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹"};
        String numStr = String.valueOf(number);
        StringBuilder urduNum = new StringBuilder();
        for (char c : numStr.toCharArray()) {
            urduNum.append(urduDigits[c - '0']);
        }
        return urduNum.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        );

        setContentView(R.layout.activity_lockscreen);

        prefs = getSharedPreferences("FlipClockPrefs", MODE_PRIVATE);

        mainLayout = findViewById(R.id.lockScreenLayout);
        hourTens = findViewById(R.id.hourTens);
        hourOnes = findViewById(R.id.hourOnes);
        minuteTens = findViewById(R.id.minuteTens);
        minuteOnes = findViewById(R.id.minuteOnes);
        dateText = findViewById(R.id.dateText);
        notificationContainer = findViewById(R.id.notificationContainer);

        // Find colon
        LinearLayout clockContainer = (LinearLayout) hourTens.getParent();
        colonText = (TextView) clockContainer.getChildAt(2);

        // Initialize prayer times UI elements
        prayerTimesOverlay = findViewById(R.id.prayerTimesOverlay);
        nextPrayerLabel = findViewById(R.id.nextPrayerLabel);
        nextPrayerName = findViewById(R.id.nextPrayerName);
        nextPrayerCountdown = findViewById(R.id.nextPrayerCountdown);
        leftInfoLabel = findViewById(R.id.leftInfoLabel);
        leftInfoValue = findViewById(R.id.leftInfoValue);
        prayerTimeFajr = findViewById(R.id.prayerTimeFajr);
        prayerTimeDhuhr = findViewById(R.id.prayerTimeDhuhr);
        prayerTimeAsr = findViewById(R.id.prayerTimeAsr);
        prayerTimeMaghrib = findViewById(R.id.prayerTimeMaghrib);
        prayerTimeIsha = findViewById(R.id.prayerTimeIsha);
        prayerNameFajr = findViewById(R.id.prayerNameFajr);
        prayerNameDhuhr = findViewById(R.id.prayerNameDhuhr);
        prayerNameAsr = findViewById(R.id.prayerNameAsr);
        prayerNameMaghrib = findViewById(R.id.prayerNameMaghrib);
        prayerNameIsha = findViewById(R.id.prayerNameIsha);

        // Hide "اگلی نماز" label - show only prayer name and countdown
        // Find the parent container and hide the first TextView (اگلی نماز label)
        LinearLayout nextPrayerContainer = findViewById(R.id.nextPrayerContainer);
        if (nextPrayerContainer != null && nextPrayerContainer.getChildCount() > 0) {
            // Hide the first child which is the "اگلی نماز" TextView
            View firstChild = nextPrayerContainer.getChildAt(0);
            if (firstChild instanceof TextView) {
                firstChild.setVisibility(View.GONE);
            }
        }

        // Initialize weather overlay
        weatherOverlay = findViewById(R.id.weatherOverlay);
        weatherFetcher = new WeatherFetcher();

        // Initialize brightness and volume indicators
        brightnessIndicator = findViewById(R.id.brightnessIndicator);
        volumeIndicator = findViewById(R.id.volumeIndicator);
        brightnessProgressBar = findViewById(R.id.brightnessProgressBar);
        volumeProgressBar = findViewById(R.id.volumeProgressBar);
        brightnessPercentage = findViewById(R.id.brightnessPercentage);
        volumePercentage = findViewById(R.id.volumePercentage);
        darkOverlay = findViewById(R.id.darkOverlay);

        // Initialize prayer calculator and weather
        float latitude = prefs.getFloat("latitude", 0);
        float longitude = prefs.getFloat("longitude", 0);
        if (latitude != 0 && longitude != 0) {
            prayerCalculator = new PrayerTimeCalculator(latitude, longitude);
            updatePrayerTimesDisplay();

            // Load cached weather data first
            currentWeatherData = WeatherData.loadFromPreferences(this);
            if (currentWeatherData != null) {
                updateWeatherDisplay();
                android.util.Log.d("LockScreen", "Loaded cached weather data");
            }

            // Fetch initial weather
            fetchWeatherData(latitude, longitude);
        }

        // Setup battery monitor FIRST to check charging state before applying colors
        setupBatteryMonitor();

        applyRotation();
        applyFontSize();
        applyFont();
        applyClockColor();  // This now uses correct isCharging value
        applyUrduFont();
        // loadNotifications(); // Disabled - notification icons removed per user request

        // Setup tap gesture handling
        setupTapGestures();

        handler = new Handler();
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                updateClock();
                // Battery optimization: check every 15 seconds instead of every second
                // Clock only updates display when minute changes anyway
                handler.postDelayed(this, 15000);
            }
        };
        handler.post(timeUpdater);

        // Date alternating updater (every 15 seconds)
        dateUpdater = new Runnable() {
            @Override
            public void run() {
                showIslamicDate = !showIslamicDate;
                updateDate();
                handler.postDelayed(this, 15000);
            }
        };
        handler.post(dateUpdater);

        // Prayer times updater (every 1 second for countdown)
        if (prayerCalculator != null) {
            prayerUpdater = new Runnable() {
                @Override
                public void run() {
                    updateNextPrayerCountdown();
                    handler.postDelayed(this, 1000);
                }
            };
            handler.post(prayerUpdater);

            // Left side info cycler (every 10 seconds)
            leftInfoUpdater = new Runnable() {
                @Override
                public void run() {
                    updateLeftInfo();
                    handler.postDelayed(this, 10000);
                }
            };
            handler.post(leftInfoUpdater);
        }

        // Weather updater (check every hour if 24 hours have passed)
        if (latitude != 0 && longitude != 0) {
            final float lat = latitude;
            final float lon = longitude;
            weatherUpdater = new Runnable() {
                @Override
                public void run() {
                    fetchWeatherData(lat, lon); // Will only fetch if 24 hours have passed
                    handler.postDelayed(this, 3600000); // Check again in 1 hour
                }
            };
            handler.postDelayed(weatherUpdater, 3600000); // Start checking after 1 hour
        }
    }

    private void applyRotation() {
        boolean rotate = prefs.getBoolean("rotate_180", false);
        if (rotate) {
            mainLayout.setRotation(180);
        } else {
            mainLayout.setRotation(0);
        }
    }

    private void applyFontSize() {
        int fontSize = prefs.getInt("font_size", 200);

        hourTens.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        hourOnes.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        minuteTens.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        minuteOnes.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

        if (colonText != null) {
            int colonSize = (int) (fontSize * 0.7);
            colonText.setTextSize(TypedValue.COMPLEX_UNIT_SP, colonSize);
        }

        int dateSize = (int) (fontSize * 0.14);
        dateText.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize);
    }

    private void applyFont() {
        String fontFile = prefs.getString("font_file", "DSEG7Classic-Bold.ttf");

        try {
            Typeface customFont;
            if (fontFile.equals("monospace")) {
                customFont = Typeface.MONOSPACE;
            } else {
                customFont = Typeface.createFromAsset(getAssets(), "fonts/" + fontFile);
            }

            hourTens.setTypeface(customFont, Typeface.BOLD);
            hourOnes.setTypeface(customFont, Typeface.BOLD);
            minuteTens.setTypeface(customFont, Typeface.BOLD);
            minuteOnes.setTypeface(customFont, Typeface.BOLD);
            if (colonText != null) {
                colonText.setTypeface(customFont, Typeface.BOLD);
            }
        } catch (Exception e) {
            // Fallback to monospace bold
            hourTens.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            hourOnes.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            minuteTens.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            minuteOnes.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            if (colonText != null) {
                colonText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            }
        }
    }

    private void applyClockColor() {
        int color;
        if (isCharging) {
            // Use user selected color when charging
            String colorStr = prefs.getString("clock_color", "#FFFFFF");
            color = Color.parseColor(colorStr);
        } else {
            // Use red color when not charging
            color = Color.RED;
        }

        hourTens.setTextColor(color);
        hourOnes.setTextColor(color);
        minuteTens.setTextColor(color);
        minuteOnes.setTextColor(color);
        dateText.setTextColor(color);
        if (colonText != null) {
            colonText.setTextColor(color);
        }

        // Apply color to prayer times UI elements
        // Note: Prayer names have their own fixed colors for visual distinction
        if (leftInfoLabel != null) leftInfoLabel.setTextColor(Color.parseColor("#87CEEB"));
        if (leftInfoValue != null) leftInfoValue.setTextColor(Color.WHITE);
        if (nextPrayerLabel != null) nextPrayerLabel.setTextColor(color);
        if (nextPrayerName != null) nextPrayerName.setTextColor(Color.parseColor("#FFD700"));
        if (nextPrayerCountdown != null) nextPrayerCountdown.setTextColor(Color.WHITE);
        if (prayerTimeFajr != null) prayerTimeFajr.setTextColor(Color.WHITE);
        if (prayerTimeDhuhr != null) prayerTimeDhuhr.setTextColor(Color.WHITE);
        if (prayerTimeAsr != null) prayerTimeAsr.setTextColor(Color.WHITE);
        if (prayerTimeMaghrib != null) prayerTimeMaghrib.setTextColor(Color.WHITE);
        if (prayerTimeIsha != null) prayerTimeIsha.setTextColor(Color.WHITE);

        // Prayer name labels use fixed colors for visual distinction
        if (prayerNameFajr != null) prayerNameFajr.setTextColor(Color.parseColor("#FFB6C1"));
        if (prayerNameDhuhr != null) prayerNameDhuhr.setTextColor(Color.parseColor("#FFD700"));
        if (prayerNameAsr != null) prayerNameAsr.setTextColor(Color.parseColor("#FFA500"));
        if (prayerNameMaghrib != null) prayerNameMaghrib.setTextColor(Color.parseColor("#FF6B6B"));
        if (prayerNameIsha != null) prayerNameIsha.setTextColor(Color.parseColor("#9370DB"));
    }

    private void applyUrduFont() {
        try {
            Typeface urduFont = Typeface.createFromAsset(getAssets(), "fonts/Gulzar-Regular.ttf");
            dateText.setTypeface(urduFont);

            // Apply to prayer times UI elements
            if (nextPrayerLabel != null) nextPrayerLabel.setTypeface(urduFont);
            if (nextPrayerName != null) nextPrayerName.setTypeface(urduFont);
            if (leftInfoLabel != null) leftInfoLabel.setTypeface(urduFont);
            if (leftInfoValue != null) leftInfoValue.setTypeface(urduFont);
            if (prayerTimeFajr != null) prayerTimeFajr.setTypeface(urduFont);
            if (prayerTimeDhuhr != null) prayerTimeDhuhr.setTypeface(urduFont);
            if (prayerTimeAsr != null) prayerTimeAsr.setTypeface(urduFont);
            if (prayerTimeMaghrib != null) prayerTimeMaghrib.setTypeface(urduFont);
            if (prayerTimeIsha != null) prayerTimeIsha.setTypeface(urduFont);

            // Apply to prayer name labels
            if (prayerNameFajr != null) prayerNameFajr.setTypeface(urduFont);
            if (prayerNameDhuhr != null) prayerNameDhuhr.setTypeface(urduFont);
            if (prayerNameAsr != null) prayerNameAsr.setTypeface(urduFont);
            if (prayerNameMaghrib != null) prayerNameMaghrib.setTypeface(urduFont);
            if (prayerNameIsha != null) prayerNameIsha.setTypeface(urduFont);
        } catch (Exception e) {
            // Fallback to default font
        }
    }

    private void loadNotifications() {
        try {
            notificationContainer.removeAllViews();

            java.util.Set<String> notificationPackages = NotificationListener.getNotificationPackages(this);
            android.content.pm.PackageManager pm = getPackageManager();

            int count = 0;
            for (String packageName : notificationPackages) {
                if (count >= 8) break; // Limit to 8 icons

                try {
                    android.graphics.drawable.Drawable appIcon = pm.getApplicationIcon(packageName);
                    ImageView icon = new ImageView(this);
                    icon.setImageDrawable(appIcon);

                    int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36,
                            getResources().getDisplayMetrics());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    params.setMargins(12, 0, 12, 0);
                    icon.setLayoutParams(params);
                    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    notificationContainer.addView(icon);
                    count++;
                } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                    // Package not found, skip
                }
            }
        } catch (Exception e) {
            // Notification access not granted or error occurred
        }
    }

    private void updateClock() {
        Calendar calendar = Calendar.getInstance();
        int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Convert to 12-hour format
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;

        if (minute != lastMinute) {
            lastMinute = minute;

            int hourTensDigit = hour12 / 10;
            int hourOnesDigit = hour12 % 10;
            int minuteTensDigit = minute / 10;
            int minuteOnesDigit = minute % 10;

            updateDigit(hourTens, hourTensDigit);
            updateDigit(hourOnes, hourOnesDigit);
            updateDigit(minuteTens, minuteTensDigit);
            updateDigit(minuteOnes, minuteOnesDigit);
        }
    }

    private void updateDate() {
        Calendar calendar = Calendar.getInstance();

        if (showIslamicDate) {
            // Display Islamic (Hijri) calendar in Urdu
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.icu.util.IslamicCalendar islamicCal = new android.icu.util.IslamicCalendar();
                islamicCal.setTimeInMillis(calendar.getTimeInMillis());

                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                int islamicDay = islamicCal.get(android.icu.util.IslamicCalendar.DAY_OF_MONTH);
                int islamicMonth = islamicCal.get(android.icu.util.IslamicCalendar.MONTH);
                int islamicYear = islamicCal.get(android.icu.util.IslamicCalendar.YEAR);

                String urduDate = urduDays[dayOfWeek] + "، " + islamicDay + " " +
                                  urduIslamicMonths[islamicMonth] + " " + islamicYear;
                dateText.setText(urduDate);
            } else {
                // Fallback to Gregorian for older devices
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                String urduDate = urduDays[dayOfWeek] + "، " + day + " " +
                                  urduMonths[month] + " " + year;
                dateText.setText(urduDate);
            }
        } else {
            // Display Gregorian calendar in Urdu
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH);
            int year = calendar.get(Calendar.YEAR);

            String urduDate = urduDays[dayOfWeek] + "، " + day + " " +
                              urduMonths[month] + " " + year;
            dateText.setText(urduDate);
        }
    }

    private void updateDigit(TextView textView, int digit) {
        textView.setText(String.valueOf(digit));
    }

    private void setupBatteryMonitor() {
        // Check initial charging state
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
        }

        // Register battery receiver for charging state changes
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                  status == BatteryManager.BATTERY_STATUS_FULL;

                if (charging != isCharging) {
                    isCharging = charging;
                    applyClockColor();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(batteryReceiver, filter);
    }

    private void setupTapGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 50;  // Minimum distance to start adjustment
            private float scrollStartX = 0;
            private float scrollStartY = 0;
            private float initialBrightness = -1;
            private int initialVolume = -1;
            private boolean isScrollingHorizontal = false;
            private boolean isScrollingVertical = false;

            // IMPORTANT: onDown must return true for gestures to work!
            @Override
            public boolean onDown(MotionEvent e) {
                // Reset scroll tracking
                scrollStartX = e.getX();
                scrollStartY = e.getY();
                initialBrightness = -1;
                initialVolume = -1;
                isScrollingHorizontal = false;
                isScrollingVertical = false;
                return true;  // Must return true to enable gesture detection
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                cycleViews();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                unlockScreen();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || e2 == null) return false;

                try {
                    float totalDiffX = e2.getX() - scrollStartX;
                    float totalDiffY = e2.getY() - scrollStartY;

                    // Determine scroll direction on first significant movement
                    if (!isScrollingHorizontal && !isScrollingVertical) {
                        if (Math.abs(totalDiffX) > SWIPE_THRESHOLD || Math.abs(totalDiffY) > SWIPE_THRESHOLD) {
                            if (Math.abs(totalDiffX) > Math.abs(totalDiffY)) {
                                isScrollingHorizontal = true;
                                // Capture initial brightness
                                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                                initialBrightness = layoutParams.screenBrightness;
                                if (initialBrightness < 0) {
                                    try {
                                        int sysBrightness = android.provider.Settings.System.getInt(
                                            getContentResolver(),
                                            android.provider.Settings.System.SCREEN_BRIGHTNESS,
                                            128
                                        );
                                        initialBrightness = sysBrightness / 255.0f;
                                    } catch (Exception ex) {
                                        initialBrightness = 0.5f;
                                    }
                                }
                            } else {
                                isScrollingVertical = true;
                                // Capture initial volume
                                android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                                if (audioManager != null) {
                                    initialVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_RING);
                                }
                            }
                        }
                    }

                    // Check if rotation is enabled
                    boolean isRotated = prefs.getBoolean("rotate_180", false);

                    // Apply continuous adjustment based on scroll direction
                    if (isScrollingHorizontal) {
                        float swipeDistance = totalDiffX;
                        if (isRotated) {
                            swipeDistance = -totalDiffX;
                        }
                        adjustBrightnessLive(swipeDistance, initialBrightness);
                        return true;
                    } else if (isScrollingVertical) {
                        // Check if volume swipe is enabled
                        boolean volumeSwipeEnabled = prefs.getBoolean("volume_swipe_enabled", true);
                        if (volumeSwipeEnabled) {
                            float swipeDistance = -totalDiffY; // Negative because down is positive Y
                            if (isRotated) {
                                swipeDistance = totalDiffY;
                            }
                            adjustVolumeLive(swipeDistance, initialVolume);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("LockScreen", "Error handling scroll: " + e.getMessage(), e);
                }
                return false;
            }
        });
    }

    private void cycleViews() {
        // Cancel any auto-hide timer
        handler.removeCallbacks(autoHideOverlay);

        // Cycle: Clock (0) → Prayer (1) → Weather (2) → Clock (0)
        currentViewState = (currentViewState + 1) % 3;

        switch (currentViewState) {
            case 0: // Clock
                prayerTimesOverlay.setVisibility(View.GONE);
                weatherOverlay.setVisibility(View.GONE);
                break;

            case 1: // Prayer
                if (prayerCalculator != null) {
                    prayerTimesOverlay.setVisibility(View.VISIBLE);
                    weatherOverlay.setVisibility(View.GONE);
                    handler.postDelayed(autoHideOverlay, 60000);
                } else {
                    currentViewState = 0; // Skip if prayer not available
                }
                break;

            case 2: // Weather
                prayerTimesOverlay.setVisibility(View.GONE);
                weatherOverlay.setVisibility(View.VISIBLE);
                handler.postDelayed(autoHideOverlay, 60000);
                // Show weather view even if data is still loading
                break;
        }
    }

    private Runnable autoHideOverlay = new Runnable() {
        @Override
        public void run() {
            // Auto-hide and return to clock
            prayerTimesOverlay.setVisibility(View.GONE);
            weatherOverlay.setVisibility(View.GONE);
            currentViewState = 0;
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Intercept ALL touch events before they reach any child views
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }
        // Pass the event to child views for normal behavior
        return super.dispatchTouchEvent(event);
    }

    private void unlockScreen() {
        // Simply finish the activity - the system will show the password/pattern screen if device is locked
        // This is more reliable than requestDismissKeyguard which can get stuck
        finish();
    }

    private void updatePrayerTimesDisplay() {
        if (prayerCalculator == null) return;

        try {
            prayerTimeFajr.setText(prayerCalculator.getPrayerTime(Prayer.FAJR));
            prayerTimeDhuhr.setText(prayerCalculator.getPrayerTime(Prayer.DHUHR));
            prayerTimeAsr.setText(prayerCalculator.getPrayerTime(Prayer.ASR));
            prayerTimeMaghrib.setText(prayerCalculator.getPrayerTime(Prayer.MAGHRIB));
            prayerTimeIsha.setText(prayerCalculator.getPrayerTime(Prayer.ISHA));

            // Apply Urdu font to prayer times
            applyUrduFont();
        } catch (Exception e) {
            // Error calculating prayer times
        }
    }

    private void updateNextPrayerCountdown() {
        if (prayerCalculator == null) {
            android.util.Log.e("LockScreen", "updateNextPrayerCountdown: prayerCalculator is NULL");
            return;
        }

        try {
            Prayer nextPrayer = prayerCalculator.getNextPrayer();
            android.util.Log.d("LockScreen", "updateNextPrayerCountdown: nextPrayer = " + nextPrayer);

            if (nextPrayer != null) {
                String prayerName = prayerCalculator.getUrduPrayerName(nextPrayer);
                String timeRemaining = prayerCalculator.getTimeUntilNextPrayer();

                android.util.Log.d("LockScreen", "updateNextPrayerCountdown: prayerName = " + prayerName + ", timeRemaining = " + timeRemaining);

                if (timeRemaining != null && !timeRemaining.isEmpty()) {
                    // Format as "Xh Ym"
                    String[] parts = timeRemaining.split(":");
                    android.util.Log.d("LockScreen", "updateNextPrayerCountdown: parts.length = " + parts.length);

                    if (parts.length >= 2) {
                        int hours = Integer.parseInt(parts[0].trim());
                        int minutes = Integer.parseInt(parts[1].trim());
                        timeRemaining = hours + "h " + minutes + "m";
                        android.util.Log.d("LockScreen", "updateNextPrayerCountdown: formatted timeRemaining = " + timeRemaining);
                    }

                    // Set prayer name and countdown
                    nextPrayerName.setText(prayerName);
                    nextPrayerCountdown.setText(timeRemaining);
                    nextPrayerName.setVisibility(View.VISIBLE);
                    nextPrayerCountdown.setVisibility(View.VISIBLE);
                    android.util.Log.d("LockScreen", "updateNextPrayerCountdown: UI updated successfully");
                } else {
                    // Fallback if time calculation fails
                    android.util.Log.w("LockScreen", "updateNextPrayerCountdown: timeRemaining is null or empty, using fallback");
                    nextPrayerName.setText(prayerName);
                    nextPrayerCountdown.setText("--:--");
                    nextPrayerName.setVisibility(View.VISIBLE);
                    nextPrayerCountdown.setVisibility(View.VISIBLE);
                }
            } else {
                android.util.Log.e("LockScreen", "updateNextPrayerCountdown: nextPrayer is NULL");
            }
        } catch (Exception e) {
            android.util.Log.e("LockScreen", "updateNextPrayerCountdown: Exception - " + e.getMessage(), e);
            // Error updating countdown - still try to show something
            try {
                Prayer nextPrayer = prayerCalculator.getNextPrayer();
                if (nextPrayer != null) {
                    nextPrayerName.setText(prayerCalculator.getUrduPrayerName(nextPrayer));
                    nextPrayerCountdown.setText("--:--");
                    nextPrayerName.setVisibility(View.VISIBLE);
                    nextPrayerCountdown.setVisibility(View.VISIBLE);
                }
            } catch (Exception ex) {
                android.util.Log.e("LockScreen", "updateNextPrayerCountdown: Fallback exception - " + ex.getMessage(), ex);
            }
        }
    }

    private void updateLeftInfo() {
        if (prayerCalculator == null) return;

        try {
            String label = "";
            String value = "";

            switch (leftInfoIndex) {
                case 0: // Sunrise
                    label = "طلوع آفتاب";
                    value = prayerCalculator.getSunriseTime();
                    break;
                case 1: // Day Length
                    label = "دن کی لمبائی";
                    value = prayerCalculator.getDayLength();
                    break;
                case 2: // Night Length
                    label = "رات کی لمبائی";
                    value = prayerCalculator.getNightLength();
                    break;
                case 3: // Middle of Night
                    label = "نصف رات";
                    value = prayerCalculator.getMiddleOfNight();
                    break;
                case 4: // Zawal (Solar Noon)
                    label = "زوال";
                    value = prayerCalculator.getZawalTime();
                    break;
            }

            leftInfoLabel.setText(label);
            leftInfoValue.setText(value);

            // Cycle to next info
            leftInfoIndex = (leftInfoIndex + 1) % 5;
        } catch (Exception e) {
            // Error updating left info
        }
    }

    private void fetchWeatherData(double latitude, double longitude) {
        // Check if we already fetched today
        if (currentWeatherData != null && currentWeatherData.lastFetchTimestamp > 0) {
            long timeSinceLastFetch = System.currentTimeMillis() - currentWeatherData.lastFetchTimestamp;
            long hoursSinceFetch = timeSinceLastFetch / (1000 * 60 * 60);

            // If less than 24 hours, don't fetch again
            if (hoursSinceFetch < 24) {
                android.util.Log.d("LockScreen", "Weather data is still fresh (fetched " + hoursSinceFetch + " hours ago), skipping fetch");
                return;
            }
        }

        android.util.Log.d("LockScreen", "Starting weather fetch from multiple APIs");

        weatherFetcher.fetchWeather(latitude, longitude, new WeatherFetcher.WeatherCallback() {
            @Override
            public void onWeatherFetched(WeatherData data) {
                // Hide loading indicator
                hideWeatherLoading();

                // Only update if we successfully fetched new data
                currentWeatherData = data;
                // Save to cache for persistence
                data.saveToPreferences(LockScreenActivity.this);
                updateWeatherDisplay();
                android.util.Log.d("LockScreen", "Weather data fetched and saved successfully");
            }

            @Override
            public void onError(String error) {
                // Hide loading indicator
                hideWeatherLoading();

                // Weather fetch failed, keep using old cached data
                android.util.Log.e("LockScreen", "All weather APIs failed: " + error);
                // Display is not updated, so old data remains visible
            }

            @Override
            public void onProgress(String apiName, int apiIndex, int tryNumber) {
                // Show loading progress
                showWeatherLoading(apiName, apiIndex, tryNumber);
                android.util.Log.d("LockScreen", "Progress: " + apiName + " - API " + apiIndex + "/3, Try " + tryNumber + "/2");
            }
        });
    }

    private void showWeatherLoading(String apiName, int apiIndex, int tryNumber) {
        LinearLayout loadingContainer = findViewById(R.id.weatherLoadingContainer);
        TextView loadingText = findViewById(R.id.weatherLoadingText);
        TextView loadingProgress = findViewById(R.id.weatherLoadingProgress);

        if (loadingContainer != null) loadingContainer.setVisibility(View.VISIBLE);
        if (loadingText != null) loadingText.setText("موسم لوڈ ہو رہا ہے...");
        if (loadingProgress != null) {
            loadingProgress.setText(apiName + " - Try " + tryNumber + "/2 (API " + apiIndex + "/3)");
        }
    }

    private void hideWeatherLoading() {
        LinearLayout loadingContainer = findViewById(R.id.weatherLoadingContainer);
        if (loadingContainer != null) loadingContainer.setVisibility(View.GONE);
    }

    private void updateWeatherDisplay() {
        if (currentWeatherData == null) return;

        try {
            // Current weather
            TextView cityName = findViewById(R.id.weatherCityName);
            TextView icon = findViewById(R.id.weatherIcon);
            TextView temp = findViewById(R.id.weatherTemp);
            TextView condition = findViewById(R.id.weatherCondition);
            TextView healthAlert = findViewById(R.id.weatherHealthAlert);
            TextView lastUpdated = findViewById(R.id.weatherLastUpdated);
            TextView nextUpdate = findViewById(R.id.weatherNextUpdate);

            if (cityName != null) cityName.setText(currentWeatherData.cityName);
            if (icon != null) icon.setText(getWeatherIconFromCondition(currentWeatherData.condition));
            if (temp != null) temp.setText(currentWeatherData.temperature);
            if (condition != null) condition.setText(currentWeatherData.conditionUrdu);
            if (healthAlert != null) healthAlert.setText(currentWeatherData.getHealthAlert());

            // Display last update with full date and time
            if (lastUpdated != null) {
                if (currentWeatherData.lastFetchTimestamp > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.ENGLISH);
                    String lastUpdateDateTime = sdf.format(new java.util.Date(currentWeatherData.lastFetchTimestamp));
                    lastUpdated.setText("اپ ڈیٹ: " + lastUpdateDateTime);
                } else {
                    lastUpdated.setText("اپ ڈیٹ: " + currentWeatherData.lastUpdated);
                }
            }

            // Calculate and display next update time with full date
            if (nextUpdate != null) {
                if (currentWeatherData.lastFetchTimestamp > 0) {
                    long nextUpdateTimestamp = currentWeatherData.lastFetchTimestamp + 86400000; // 24 hours
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.ENGLISH);
                    String nextUpdateDateTime = sdf.format(new java.util.Date(nextUpdateTimestamp));
                    nextUpdate.setText("اگلی اپ ڈیٹ: " + nextUpdateDateTime);
                } else {
                    nextUpdate.setText("اگلی اپ ڈیٹ: --:--");
                }
            }

            // Weather details
            updateWeatherDetail(R.id.weatherDetailFeelsLike, "محسوس ہوتا ہے", currentWeatherData.feelsLike);
            updateWeatherDetail(R.id.weatherDetailHumidity, "نمی", currentWeatherData.humidity);
            updateWeatherDetail(R.id.weatherDetailWind, "ہوا کی رفتار", currentWeatherData.windSpeed);
            updateWeatherDetail(R.id.weatherDetailPressure, "دباؤ", currentWeatherData.pressure);
            updateWeatherDetail(R.id.weatherDetailUV, "UV انڈیکس", currentWeatherData.uvIndex);
            updateWeatherDetail(R.id.weatherDetailVisibility, "مرئیت", currentWeatherData.visibility);
            updateWeatherDetail(R.id.weatherDetailDewPoint, "اوس نقطہ", currentWeatherData.dewPoint);
            updateWeatherDetail(R.id.weatherDetailClouds, "بادل", currentWeatherData.cloudCover);

            // 7-day forecast
            updateForecast();

            // Apply Urdu font
            applyUrduFontToWeather();

        } catch (Exception e) {
            // Error updating weather display
        }
    }

    private void updateWeatherDetail(int viewId, String label, String value) {
        View detailView = findViewById(viewId);
        if (detailView != null) {
            TextView labelView = detailView.findViewById(R.id.detailLabel);
            TextView valueView = detailView.findViewById(R.id.detailValue);
            if (labelView != null) labelView.setText(label);
            if (valueView != null) valueView.setText(value);
        }
    }

    private void updateForecast() {
        LinearLayout forecastContainer = findViewById(R.id.forecastContainer);
        if (forecastContainer == null || currentWeatherData == null) {
            android.util.Log.w("LockScreen", "updateForecast: forecastContainer or currentWeatherData is null");
            return;
        }

        forecastContainer.removeAllViews();
        android.util.Log.d("LockScreen", "updateForecast: forecast array length = " + currentWeatherData.forecast.length);

        for (int i = 0; i < 7 && i < currentWeatherData.forecast.length; i++) {
            WeatherData.DayForecast day = currentWeatherData.forecast[i];
            android.util.Log.d("LockScreen", "updateForecast: Day " + i + " = " + (day == null ? "NULL" : day.dayNameUrdu + " " + day.date));
            if (day == null) {
                android.util.Log.w("LockScreen", "updateForecast: Day " + i + " is null, skipping");
                continue;
            }

            LinearLayout dayCard = new LinearLayout(this);
            dayCard.setOrientation(LinearLayout.VERTICAL);
            dayCard.setGravity(android.view.Gravity.CENTER);
            dayCard.setBackgroundResource(R.drawable.prayer_card_bg);
            dayCard.setPadding(6, 8, 6, 8);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(2, 0, 2, 0);
            dayCard.setLayoutParams(params);

            TextView dayName = new TextView(this);
            dayName.setText(day.dayNameUrdu);
            dayName.setTextSize(10);
            dayName.setTextColor(Color.parseColor("#87CEEB"));
            dayName.setGravity(android.view.Gravity.CENTER);
            dayCard.addView(dayName);

            TextView iconView = new TextView(this);
            iconView.setText(day.icon);
            iconView.setTextSize(22);
            iconView.setGravity(android.view.Gravity.CENTER);
            iconView.setPadding(0, 3, 0, 3);
            dayCard.addView(iconView);

            TextView tempMax = new TextView(this);
            tempMax.setText(day.maxTemp);
            tempMax.setTextSize(14);
            tempMax.setTextColor(Color.WHITE);
            tempMax.setGravity(android.view.Gravity.CENTER);
            tempMax.setTypeface(null, android.graphics.Typeface.BOLD);
            dayCard.addView(tempMax);

            TextView tempMin = new TextView(this);
            tempMin.setText(day.minTemp);
            tempMin.setTextSize(11);
            tempMin.setTextColor(Color.parseColor("#AAAAAA"));
            tempMin.setGravity(android.view.Gravity.CENTER);
            dayCard.addView(tempMin);

            forecastContainer.addView(dayCard);
        }
    }

    private String getWeatherIconFromCondition(String condition) {
        switch (condition.toLowerCase()) {
            case "clear": return "☀️";
            case "clouds": return "☁️";
            case "rain": return "🌧️";
            case "drizzle": return "🌦️";
            case "thunderstorm": return "⛈️";
            case "snow": return "🌨️";
            case "mist":
            case "fog": return "🌫️";
            default: return "🌤️";
        }
    }

    private void applyUrduFontToWeather() {
        try {
            Typeface urduFont = Typeface.createFromAsset(getAssets(), "fonts/Gulzar-Regular.ttf");

            TextView cityName = findViewById(R.id.weatherCityName);
            TextView condition = findViewById(R.id.weatherCondition);
            TextView healthAlert = findViewById(R.id.weatherHealthAlert);
            TextView lastUpdated = findViewById(R.id.weatherLastUpdated);
            TextView nextUpdate = findViewById(R.id.weatherNextUpdate);

            if (cityName != null) cityName.setTypeface(urduFont);
            if (condition != null) condition.setTypeface(urduFont);
            if (healthAlert != null) healthAlert.setTypeface(urduFont);
            if (lastUpdated != null) lastUpdated.setTypeface(urduFont);
            if (nextUpdate != null) nextUpdate.setTypeface(urduFont);

            // Apply to detail labels
            int[] detailIds = {R.id.weatherDetailFeelsLike, R.id.weatherDetailHumidity,
                              R.id.weatherDetailWind, R.id.weatherDetailPressure,
                              R.id.weatherDetailUV, R.id.weatherDetailVisibility,
                              R.id.weatherDetailDewPoint, R.id.weatherDetailClouds};

            for (int id : detailIds) {
                View detailView = findViewById(id);
                if (detailView != null) {
                    TextView label = detailView.findViewById(R.id.detailLabel);
                    if (label != null) label.setTypeface(urduFont);
                }
            }

        } catch (Exception e) {
            // Font loading failed
        }
    }

    // Live brightness control - updates in real-time as you drag
    // Supports "extra dark" mode using overlay when brightness goes below 0%
    private void adjustBrightnessLive(float swipeDistance, float startBrightness) {
        try {
            // Get screen width for calculation
            int screenWidth = getResources().getDisplayMetrics().widthPixels;

            // Calculate brightness change based on swipe distance from start
            // Increased sensitivity: multiply by 2.0 so half-screen swipe = 100% change
            float brightnessChange = (swipeDistance / screenWidth) * 2.0f;

            // Calculate new brightness from initial value
            // Allow range from -1.0 (extra dark) to 1.0 (100%)
            float newBrightness = startBrightness - currentOverlayAlpha + brightnessChange;

            // Get brightness limit from settings (default -5%)
            int brightnessLimit = prefs.getInt("brightness_limit", 5);
            float minBrightness = -brightnessLimit / 100.0f;

            // Clamp between limit and 100%
            newBrightness = Math.max(minBrightness, Math.min(1.0f, newBrightness));

            if (newBrightness >= 0) {
                // Normal brightness range: 0% to 100%
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = newBrightness;
                getWindow().setAttributes(layoutParams);

                // Remove overlay
                if (darkOverlay != null) {
                    darkOverlay.setAlpha(0f);
                    currentOverlayAlpha = 0f;
                }

                // Show feedback
                showBrightnessFeedback((int)(newBrightness * 100));
            } else {
                // Extra dark mode: brightness at minimum, use overlay
                WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                layoutParams.screenBrightness = 0.0f;
                getWindow().setAttributes(layoutParams);

                // Apply dark overlay (negative brightness becomes positive alpha)
                float overlayAlpha = Math.abs(newBrightness);
                if (darkOverlay != null) {
                    darkOverlay.setAlpha(overlayAlpha);
                    currentOverlayAlpha = overlayAlpha;
                }

                // Show feedback with negative percentage
                int displayPercent = (int)(newBrightness * 100);
                showBrightnessFeedback(displayPercent);
            }
        } catch (Exception e) {
            android.util.Log.e("LockScreen", "Error adjusting brightness live: " + e.getMessage());
        }
    }

    // Live volume control - updates in real-time as you drag
    private void adjustVolumeLive(float swipeDistance, int startVolume) {
        try {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_RING);

                // Get screen height for calculation
                int screenHeight = getResources().getDisplayMetrics().heightPixels;

                // Calculate volume change based on swipe distance from start
                // Increased sensitivity: multiply by 2.0 so half-screen swipe = full volume range
                float volumeChangeFloat = (swipeDistance / screenHeight) * maxVolume * 2.0f;
                int volumeChange = Math.round(volumeChangeFloat);

                // Calculate new volume from initial value
                int newVolume = startVolume + volumeChange;

                // Clamp between 0 and maxVolume
                newVolume = Math.max(0, Math.min(maxVolume, newVolume));

                // Set the new volume immediately
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_RING,
                    newVolume,
                    0  // No flags - don't show system UI
                );

                // Show custom feedback (updates bar continuously)
                showVolumeFeedback(newVolume, maxVolume);
            }
        } catch (Exception e) {
            android.util.Log.e("LockScreen", "Error adjusting volume live: " + e.getMessage());
        }
    }

    // Show brightness feedback with visual indicator bar
    private void showBrightnessFeedback(int percentage) {
        if (brightnessIndicator == null || brightnessProgressBar == null || brightnessPercentage == null) {
            return;
        }

        // Update progress bar and percentage text
        // For extra dark mode (negative %), show as -1%, -2%, etc.
        if (percentage < 0) {
            brightnessProgressBar.setProgress(0);
            brightnessPercentage.setText(percentage + "%");
        } else {
            brightnessProgressBar.setProgress(percentage);
            brightnessPercentage.setText(percentage + "%");
        }

        // Show the indicator
        brightnessIndicator.setVisibility(View.VISIBLE);

        // Cancel any existing hide timer
        if (hideBrightnessIndicator != null) {
            handler.removeCallbacks(hideBrightnessIndicator);
        }

        // Auto-hide after 2 seconds
        hideBrightnessIndicator = new Runnable() {
            @Override
            public void run() {
                if (brightnessIndicator != null) {
                    brightnessIndicator.setVisibility(View.GONE);
                }
            }
        };
        handler.postDelayed(hideBrightnessIndicator, 2000);
    }

    // Show volume feedback with visual indicator bar
    private void showVolumeFeedback(int currentVolume, int maxVolume) {
        if (volumeIndicator == null || volumeProgressBar == null || volumePercentage == null) {
            return;
        }

        // Update progress bar and text
        volumeProgressBar.setMax(maxVolume);
        volumeProgressBar.setProgress(currentVolume);
        volumePercentage.setText(currentVolume + "/" + maxVolume);

        // Show the indicator
        volumeIndicator.setVisibility(View.VISIBLE);

        // Cancel any existing hide timer
        if (hideVolumeIndicator != null) {
            handler.removeCallbacks(hideVolumeIndicator);
        }

        // Auto-hide after 2 seconds
        hideVolumeIndicator = new Runnable() {
            @Override
            public void run() {
                if (volumeIndicator != null) {
                    volumeIndicator.setVisibility(View.GONE);
                }
            }
        };
        handler.postDelayed(hideVolumeIndicator, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            if (timeUpdater != null) {
                handler.removeCallbacks(timeUpdater);
            }
            if (dateUpdater != null) {
                handler.removeCallbacks(dateUpdater);
            }
            if (prayerUpdater != null) {
                handler.removeCallbacks(prayerUpdater);
            }
            if (leftInfoUpdater != null) {
                handler.removeCallbacks(leftInfoUpdater);
            }
            if (weatherUpdater != null) {
                handler.removeCallbacks(weatherUpdater);
            }
            handler.removeCallbacks(autoHideOverlay);
        }
        if (batteryReceiver != null) {
            try {
                unregisterReceiver(batteryReceiver);
            } catch (Exception e) {
                // Receiver not registered
            }
        }
        if (weatherFetcher != null) {
            weatherFetcher.shutdown();
        }
    }
}
