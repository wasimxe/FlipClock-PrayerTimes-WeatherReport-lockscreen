@echo off
set JAVA_HOME=D:\workspace\android\aod\jdk\jdk-17.0.2
set ANDROID_HOME=D:\workspace\android\aod\android-sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo Building APK with prayer times features...
call gradlew.bat assembleDebug --no-daemon

if %errorlevel% equ 0 (
    echo.
    echo Build successful!
    echo Installing on device...
    adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo Done!
) else (
    echo Build failed!
    exit /b 1
)
