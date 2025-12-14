@echo off
cd /d D:\workspace\android\aod
set JAVA_HOME=D:\workspace\android\aod\jdk\jdk-17.0.2
set ANDROID_HOME=D:\workspace\android\aod\android-sdk

echo Cleaning build directories...
rmdir /s /q .gradle 2>nul
rmdir /s /q app\build 2>nul
rmdir /s /q build 2>nul

echo Building APK...
gradlew.bat clean assembleDebug --no-daemon

if %ERRORLEVEL% EQU 0 (
    echo Installing APK...
    adb install -r app\build\outputs\apk\debug\app-debug.apk
    if %ERRORLEVEL% EQU 0 (
        echo Installation successful!
    ) else (
        echo Installation failed!
    )
) else (
    echo Build failed!
)
