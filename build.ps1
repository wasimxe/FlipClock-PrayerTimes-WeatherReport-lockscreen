$env:JAVA_HOME = "D:\workspace\android\aod\jdk\jdk-17.0.2"
$env:ANDROID_HOME = "D:\workspace\android\aod\android-sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

Write-Host "Cleaning previous build..."
Set-Location "D:\workspace\android\aod"
& ".\gradlew.bat" clean --no-daemon

Write-Host "Building APK..."
& ".\gradlew.bat" assembleDebug --no-daemon

if (Test-Path "app\build\outputs\apk\debug\app-debug.apk") {
    Write-Host "Build successful! Installing..."
    & adb install -r "app\build\outputs\apk\debug\app-debug.apk"
    Write-Host "Installation complete!"
} else {
    Write-Host "Build failed - APK not found"
}
