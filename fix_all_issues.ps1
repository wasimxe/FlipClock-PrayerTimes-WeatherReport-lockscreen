# Fix all remaining issues

# 1. Add RTL to forecast container
$xmlFile = "D:\workspace\android\aod\app\src\main\res\layout\activity_lockscreen.xml"
$content = Get-Content $xmlFile -Raw

# Add layoutDirection="rtl" to forecastContainer
$content = $content -replace '(<LinearLayout\s+android:id="@\+id/forecastContainer"[^>]+android:orientation="horizontal"[^>]+android:weightSum="7")', '$1 android:layoutDirection="rtl"'

# Add top margin to prayer cards container (line 237)
$content = $content -replace '(<!-- All 5 prayers in a horizontal row \(RTL for Urdu\) -->\s+<LinearLayout\s+android:layout_width="wrap_content"\s+android:layout_height="wrap_content"\s+android:orientation="horizontal"\s+android:layoutDirection="rtl"\s+android:gravity="center">)', ('<!-- All 5 prayers in a horizontal row (RTL for Urdu) -->' + "`n        <LinearLayout`n            android:layout_width=`"wrap_content`"`n            android:layout_height=`"wrap_content`"`n            android:orientation=`"horizontal`"`n            android:layoutDirection=`"rtl`"`n            android:gravity=`"center`"`n            android:layout_marginTop=`"40dp`">")

Set-Content -Path $xmlFile -Value $content -NoNewline
Write-Host "XML fixes applied successfully"
