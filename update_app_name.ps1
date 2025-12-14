$manifestFile = "D:\workspace\android\aod\app\src\main\AndroidManifest.xml"
$content = Get-Content $manifestFile -Raw

# Update app name to reflect clock, weather, and prayer features
$content = $content -replace 'android:label="Flip Clock Lock Screen"', 'android:label="Prayer Clock & Weather"'

Set-Content -Path $manifestFile -Value $content -NoNewline
Write-Host "App name updated to 'Prayer Clock & Weather'"
