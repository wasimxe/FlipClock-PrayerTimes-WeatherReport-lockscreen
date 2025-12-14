$file = "D:\workspace\android\aod\app\src\main\res\layout\activity_lockscreen.xml"
$content = Get-Content $file -Raw

# Update Dhuhr
$content = $content -replace '(?s)<!-- Dhuhr -->.*?<LinearLayout\s+android:layout_width="wrap_content"\s+android:layout_height="wrap_content"\s+android:background="@drawable/prayer_card_bg"\s+android:paddingTop="20dp"\s+android:paddingBottom="16dp"\s+android:paddingStart="25dp"\s+android:paddingEnd="25dp"\s+android:orientation="vertical"\s+android:gravity="center"\s+android:layout_marginStart="8dp"\s+android:layout_marginEnd="8dp">','<!-- Dhuhr -->
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:background="@drawable/prayer_card_bg"
                android:paddingTop="8dp"
                android:paddingBottom="8dp"
                android:paddingStart="4dp"
                android:paddingEnd="4dp"
                android:orientation="vertical"
                android:gravity="center"
                android:layout_marginStart="3dp"
                android:layout_marginEnd="3dp">'

# Update Asr
$content = $content -replace '(?s)<!-- Asr -->.*?<LinearLayout\s+android:layout_width="wrap_content"\s+android:layout_height="wrap_content"\s+android:background="@drawable/prayer_card_bg"\s+android:paddingTop="20dp"\s+android:paddingBottom="16dp"\s+android:paddingStart="25dp"\s+android:paddingEnd="25dp"\s+android:orientation="vertical"\s+android:gravity="center"\s+android:layout_marginStart="8dp"\s+android:layout_marginEnd="8dp">','<!-- Asr -->
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:background="@drawable/prayer_card_bg"
                android:paddingTop="8dp"
                android:paddingBottom="8dp"
                android:paddingStart="4dp"
                android:paddingEnd="4dp"
                android:orientation="vertical"
                android:gravity="center"
                android:layout_marginStart="3dp"
                android:layout_marginEnd="3dp">'

# Update Maghrib
$content = $content -replace '(?s)<!-- Maghrib -->.*?<LinearLayout\s+android:layout_width="wrap_content"\s+android:layout_height="wrap_content"\s+android:background="@drawable/prayer_card_bg"\s+android:paddingTop="20dp"\s+android:paddingBottom="16dp"\s+android:paddingStart="25dp"\s+android:paddingEnd="25dp"\s+android:orientation="vertical"\s+android:gravity="center"\s+android:layout_marginStart="8dp"\s+android:layout_marginEnd="8dp">','<!-- Maghrib -->
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:background="@drawable/prayer_card_bg"
                android:paddingTop="8dp"
                android:paddingBottom="8dp"
                android:paddingStart="4dp"
                android:paddingEnd="4dp"
                android:orientation="vertical"
                android:gravity="center"
                android:layout_marginStart="3dp"
                android:layout_marginEnd="3dp">'

# Update Isha
$content = $content -replace '(?s)<!-- Isha -->.*?<LinearLayout\s+android:layout_width="wrap_content"\s+android:layout_height="wrap_content"\s+android:background="@drawable/prayer_card_bg"\s+android:paddingTop="20dp"\s+android:paddingBottom="16dp"\s+android:paddingStart="25dp"\s+android:paddingEnd="25dp"\s+android:orientation="vertical"\s+android:gravity="center"\s+android:layout_marginStart="6dp"\s+android:layout_marginEnd="6dp">','<!-- Isha -->
            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:background="@drawable/prayer_card_bg"
                android:paddingTop="8dp"
                android:paddingBottom="8dp"
                android:paddingStart="4dp"
                android:paddingEnd="4dp"
                android:orientation="vertical"
                android:gravity="center"
                android:layout_marginStart="3dp"
                android:layout_marginEnd="3dp">'

# Update all icon text sizes and margins
$content = $content -replace 'android:textSize="32sp"\s+android:layout_marginBottom="12dp"','android:textSize="28sp"
                    android:layout_marginBottom="4dp"'

# Update all prayer name text sizes and margins
$content = $content -replace '(android:id="@\+id/prayerName(?:Dhuhr|Asr|Maghrib|Isha)".*?android:textSize=")28sp("\s+.*?android:textStyle="bold"\s+android:layout_marginBottom=")8dp(")', '$124sp$2
                    android:layout_marginTop="2dp"
                    android:layout_marginBottom="2dp"$3'

# Update all prayer time text sizes
$content = $content -replace '(android:id="@\+id/prayerTime(?:Dhuhr|Asr|Maghrib|Isha)".*?android:textSize=")24sp(")', '$120sp$2'

Set-Content -Path $file -Value $content
Write-Host "Prayer cards updated successfully!"
