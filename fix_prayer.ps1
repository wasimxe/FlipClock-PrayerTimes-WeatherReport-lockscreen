$file = "D:\workspace\android\aod\app\src\main\java\com\flipclock\lockscreen\PrayerTimeCalculator.java"
$content = Get-Content $file -Raw

# Fix getNextPrayer method
$oldMethod = @"
    public Prayer getNextPrayer() {
        return prayerTimes.nextPrayer();
    }
"@

$newMethod = @"
    public Prayer getNextPrayer() {
        Prayer nextPrayer = prayerTimes.nextPrayer();
        // If no more prayers today (after Isha), next prayer is tomorrow's Fajr
        if (nextPrayer == null) {
            return Prayer.FAJR;
        }
        return nextPrayer;
    }
"@

$content = $content.Replace($oldMethod, $newMethod)
Set-Content -Path $file -Value $content -NoNewline
Write-Host "File updated successfully"
