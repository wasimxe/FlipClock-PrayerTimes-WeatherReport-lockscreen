@echo off
echo ========================================
echo  Flip Clock Lock Screen Builder
echo ========================================
echo.

echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found!
    echo Please install JDK 17 or newer from:
    echo https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo Java found!
echo.

echo Building APK...
call gradlew.bat assembleDebug

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Build failed!
    echo Check the errors above.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build successful!
echo ========================================
echo.

echo Checking for connected device...
adb devices | findstr /R "device$" >nul
if %errorlevel% neq 0 (
    echo WARNING: No device connected!
    echo Please connect your Android device and enable USB debugging.
    echo.
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
    pause
    exit /b 0
)

echo Device found!
echo.
echo Installing app...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo Installation successful!
    echo ========================================
    echo.
    echo Next steps:
    echo 1. Open "Flip Clock Lock Screen" on your device
    echo 2. Tap "Enable Lock Screen"
    echo 3. Grant overlay permission
    echo 4. Lock your device to test!
) else (
    echo.
    echo ERROR: Installation failed!
    echo Try installing manually with:
    echo adb install app\build\outputs\apk\debug\app-debug.apk
)

echo.
pause
