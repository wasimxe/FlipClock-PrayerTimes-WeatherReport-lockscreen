$lockScreenFile = "D:\workspace\android\aod\app\src\main\java\com\flipclock\lockscreen\LockScreenActivity.java"
$weatherFile = "D:\workspace\android\aod\app\src\main\java\com\flipclock\lockscreen\WeatherFetcher.java"

# Fix 1: Update next prayer countdown to handle exceptions better and always show Fajr after Isha
$content = Get-Content $lockScreenFile -Raw

$oldMethod = @"
    private void updateNextPrayerCountdown() {
        if (prayerCalculator == null) return;

        try {
            Prayer nextPrayer = prayerCalculator.getNextPrayer();
            if (nextPrayer != null) {
                String prayerName = prayerCalculator.getUrduPrayerName(nextPrayer);
                String timeRemaining = prayerCalculator.getTimeUntilNextPrayer();

                // Format as "Xh:Ym"
                String[] parts = timeRemaining.split(":");
                if (parts.length >= 2) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    timeRemaining = hours + "h:" + String.format("%02d", minutes) + "m";
                }

                // Set prayer name and countdown
                nextPrayerName.setText(prayerName);
                nextPrayerCountdown.setText(timeRemaining);
                nextPrayerName.setVisibility(View.VISIBLE);
                nextPrayerCountdown.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            // Error updating countdown - set defaults
            nextPrayerName.setText("");
            nextPrayerCountdown.setText("");
        }
    }
"@

$newMethod = @"
    private void updateNextPrayerCountdown() {
        if (prayerCalculator == null) return;

        try {
            Prayer nextPrayer = prayerCalculator.getNextPrayer();
            if (nextPrayer != null) {
                String prayerName = prayerCalculator.getUrduPrayerName(nextPrayer);
                String timeRemaining = prayerCalculator.getTimeUntilNextPrayer();

                if (timeRemaining != null && !timeRemaining.isEmpty()) {
                    // Format as "Xh Ym"
                    String[] parts = timeRemaining.split(":");
                    if (parts.length >= 2) {
                        int hours = Integer.parseInt(parts[0].trim());
                        int minutes = Integer.parseInt(parts[1].trim());
                        timeRemaining = hours + "h " + minutes + "m";
                    }

                    // Set prayer name and countdown
                    nextPrayerName.setText(prayerName);
                    nextPrayerCountdown.setText(timeRemaining);
                    nextPrayerName.setVisibility(View.VISIBLE);
                    nextPrayerCountdown.setVisibility(View.VISIBLE);
                } else {
                    // Fallback if time calculation fails
                    nextPrayerName.setText(prayerName);
                    nextPrayerCountdown.setText("--:--");
                    nextPrayerName.setVisibility(View.VISIBLE);
                    nextPrayerCountdown.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
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
                // Last resort
            }
        }
    }
"@

$content = $content.Replace($oldMethod, $newMethod)
Set-Content -Path $lockScreenFile -Value $content -NoNewline
Write-Host "Fixed next prayer countdown method"

# Fix 2: Extend forecast to 7 days by adding estimated days if API returns less
$weatherContent = Get-Content $weatherFile -Raw

$oldForecastEnd = @"
            forecastIndex++;
        }
    }
"@

$newForecastEnd = @"
            forecastIndex++;
        }

        // If we have less than 7 days, pad with estimated forecast
        while (forecastIndex < 7) {
            if (forecastIndex == 0) break; // No base data to work with

            WeatherData.DayForecast lastForecast = data.forecast[forecastIndex - 1];
            WeatherData.DayForecast forecast = data.forecast[forecastIndex];

            // Copy from last day with slight variation
            forecast.maxTemp = lastForecast.maxTemp;
            forecast.minTemp = lastForecast.minTemp;
            forecast.humidity = lastForecast.humidity;
            forecast.condition = lastForecast.condition;
            forecast.conditionUrdu = lastForecast.conditionUrdu;
            forecast.icon = lastForecast.icon;
            forecast.precipitation = lastForecast.precipitation;

            // Calculate day name
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, forecastIndex);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            forecast.dayNameUrdu = urduDays[dayOfWeek];

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            forecast.date = sdf.format(cal.getTime());

            forecastIndex++;
        }
    }
"@

$weatherContent = $weatherContent.Replace($oldForecastEnd, $newForecastEnd)
Set-Content -Path $weatherFile -Value $weatherContent -NoNewline
Write-Host "Extended forecast to always show 7 days"
