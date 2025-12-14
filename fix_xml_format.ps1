$xmlFile = "D:\workspace\android\aod\app\src\main\res\layout\activity_lockscreen.xml"
$content = Get-Content $xmlFile -Raw

# Fix the malformed XML line by splitting attributes properly
$content = $content -replace 'android:weightSum="7" android:layoutDirection="rtl"', ('android:weightSum="7"' + "`n                android:layoutDirection=`"rtl`"")

Set-Content -Path $xmlFile -Value $content -NoNewline
Write-Host "XML formatting fixed"
