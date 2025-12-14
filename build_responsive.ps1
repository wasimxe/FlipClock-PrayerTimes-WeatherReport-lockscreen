$ErrorActionPreference = "Continue"
cd D:\workspace\android\aod
$env:JAVA_HOME = "D:\workspace\android\aod\jdk\jdk-17.0.2"
$env:ANDROID_HOME = "D:\workspace\android\aod\android-sdk"

Write-Host "Stopping any running Java processes..."
Stop-Process -Name java -Force -ErrorAction SilentlyContinue

Write-Host "Cleaning build directory..."
Remove-Item -Recurse -Force .\app\build -ErrorAction SilentlyContinue

Write-Host "Running build..."
& .\gradlew.bat assembleDebug --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful! Installing..."
    adb install -r app\build\outputs\apk\debug\app-debug.apk

    if ($LASTEXITCODE -eq 0) {
        Write-Host "Installation successful!"
    } else {
        Write-Host "Installation failed!"
    }
} else {
    Write-Host "Build failed!"
}
