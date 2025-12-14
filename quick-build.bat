@echo off
cd /d D:\workspace\android\aod
set JAVA_HOME=D:\workspace\android\aod\jdk\jdk-17.0.2
set ANDROID_HOME=D:\workspace\android\aod\android-sdk
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%
call gradlew.bat assembleDebug --no-daemon
if exist app\build\outputs\apk\debug\app-debug.apk (
    echo Installing APK...
    adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo Done!
) else (
    echo Build failed!
)
