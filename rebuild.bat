@echo off
cd /d D:\workspace\android\aod
set JAVA_HOME=D:\workspace\android\aod\jdk\jdk-17.0.2
set ANDROID_HOME=D:\workspace\android\aod\android-sdk
set PATH=%JAVA_HOME%\bin;%PATH%

echo Starting Gradle build...
gradlew.bat clean assembleDebug --no-daemon --stacktrace

echo Build completed with exit code: %ERRORLEVEL%
