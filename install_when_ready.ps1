Write-Host "Waiting for device authorization..."

for ($i = 1; $i -le 60; $i++) {
    $devices = adb devices 2>&1 | Out-String
    if ($devices -match "9419974d0405\s+device") {
        Write-Host "Device authorized! Installing APK..."
        adb install -r app\build\outputs\apk\debug\app-debug.apk
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Installation successful!"
            Write-Host "Launching app..."
            adb shell am start -n com.flipclock.lockscreen/.MainActivity
            Write-Host "App launched. Waiting 3 seconds..."
            Start-Sleep -Seconds 3
            Write-Host "Taking screenshot..."
            adb exec-out screencap -p > "D:/workspace/android/aod/app_with_debug_logging.png"
            Write-Host "Screenshot saved!"
            Write-Host "Fetching logcat..."
            adb logcat -d -s LockScreen:D LockScreen:W LockScreen:E PrayerCalc:D PrayerCalc:W PrayerCalc:E WeatherFetcher:D > "D:/workspace/android/aod/debug_logs.txt"
            Write-Host "Logs saved to debug_logs.txt"
            exit 0
        } else {
            Write-Host "Installation failed!"
            exit 1
        }
    }
    Write-Host "Attempt $i/60... Device not authorized yet"
    Start-Sleep -Seconds 2
}

Write-Host "Timeout waiting for authorization. Please authorize the device and run: adb install -r app\build\outputs\apk\debug\app-debug.apk"
exit 1
