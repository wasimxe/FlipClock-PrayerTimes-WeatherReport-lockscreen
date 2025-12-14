@echo off
cd /d D:\workspace\android\aod
set JAVA_HOME=D:\workspace\android\aod\jdk\jdk-17.0.2
set ANDROID_HOME=D:\workspace\android\aod\android-sdk
rmdir /s /q app\build 2>nul
gradlew.bat assembleDebug --no-daemon
if %ERRORLEVEL% EQU 0 (
    adb install -r app\build\outputs\apk\debug\app-debug.apk
)
