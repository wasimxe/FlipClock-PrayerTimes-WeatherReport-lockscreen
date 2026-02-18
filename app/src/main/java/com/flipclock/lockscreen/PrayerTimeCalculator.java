package com.flipclock.lockscreen;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.Prayer;
import com.batoulapps.adhan.PrayerTimes;
import com.batoulapps.adhan.SunnahTimes;
import com.batoulapps.adhan.data.DateComponents;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PrayerTimeCalculator {

    private double latitude;
    private double longitude;
    private int madhab; // 0 = Shafi, 1 = Hanafi
    private PrayerTimes prayerTimes;
    private SunnahTimes sunnahTimes;

    public PrayerTimeCalculator(double latitude, double longitude, int madhab) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.madhab = madhab;
        calculatePrayerTimes();
    }

    private CalculationParameters getCalculationParameters() {
        // Use University of Karachi calculation method (KARACHI)
        CalculationParameters parameters = CalculationMethod.KARACHI.getParameters();
        // Set madhab for Asr calculation
        parameters.madhab = (madhab == 1) ? Madhab.HANAFI : Madhab.SHAFI;
        return parameters;
    }

    private void calculatePrayerTimes() {
        Coordinates coordinates = new Coordinates(latitude, longitude);
        DateComponents dateComponents = DateComponents.from(new Date());
        CalculationParameters parameters = getCalculationParameters();

        prayerTimes = new PrayerTimes(coordinates, dateComponents, parameters);
        sunnahTimes = new SunnahTimes(prayerTimes);
    }

    public String getUrduPrayerName(Prayer prayer) {
        switch (prayer) {
            case FAJR:
                return "فجر";
            case SUNRISE:
                return "طلوع آفتاب";
            case DHUHR:
                return "ظہر";
            case ASR:
                return "عصر";
            case MAGHRIB:
                return "مغرب";
            case ISHA:
                return "عشاء";
            default:
                return "";
        }
    }

    public String getPrayerTime(Prayer prayer) {
        Date time = null;
        switch (prayer) {
            case FAJR:
                time = prayerTimes.fajr;
                break;
            case SUNRISE:
                time = prayerTimes.sunrise;
                break;
            case DHUHR:
                time = prayerTimes.dhuhr;
                break;
            case ASR:
                time = prayerTimes.asr;
                break;
            case MAGHRIB:
                time = prayerTimes.maghrib;
                break;
            case ISHA:
                time = prayerTimes.isha;
                break;
        }

        if (time != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            return formatter.format(time);
        }
        return "";
    }

    public Prayer getNextPrayer() {
        Prayer nextPrayer = prayerTimes.nextPrayer();
        android.util.Log.d("PrayerCalc", "getNextPrayer: prayerTimes.nextPrayer() returned " + nextPrayer);
        // If no more prayers today (after Isha), next prayer is tomorrow's Fajr
        if (nextPrayer == null || nextPrayer == Prayer.NONE) {
            android.util.Log.d("PrayerCalc", "getNextPrayer: Returning FAJR (after Isha)");
            return Prayer.FAJR;
        }
        return nextPrayer;
    }

    public Date getNextPrayerTime() {
        Prayer nextPrayer = getNextPrayer();
        android.util.Log.d("PrayerCalc", "getNextPrayerTime: nextPrayer = " + nextPrayer);

        // After Isha, nextPrayer will be FAJR, and we need tomorrow's Fajr
        if (nextPrayer == Prayer.FAJR) {
            Date now = new Date();
            Date todayFajr = prayerTimes.fajr;
            android.util.Log.d("PrayerCalc", "getNextPrayerTime: FAJR case - now = " + now + ", todayFajr = " + todayFajr);

            // If Fajr has already passed today, calculate tomorrow's Fajr
            if (todayFajr != null && todayFajr.getTime() < now.getTime()) {
                android.util.Log.d("PrayerCalc", "getNextPrayerTime: Fajr has passed, calculating tomorrow's Fajr");
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);

                Coordinates coordinates = new Coordinates(latitude, longitude);
                DateComponents tomorrowDate = DateComponents.from(tomorrow.getTime());
                CalculationParameters parameters = getCalculationParameters();
                PrayerTimes tomorrowPrayers = new PrayerTimes(coordinates, tomorrowDate, parameters);

                android.util.Log.d("PrayerCalc", "getNextPrayerTime: tomorrow's Fajr = " + tomorrowPrayers.fajr);
                return tomorrowPrayers.fajr;
            }
        }

        Date prayerTime = getPrayerTimeDate(nextPrayer);
        android.util.Log.d("PrayerCalc", "getNextPrayerTime: returning " + prayerTime);
        return prayerTime;
    }

    public Date getPrayerTimeDate(Prayer prayer) {
        Date prayerTime = null;
        switch (prayer) {
            case FAJR:
                prayerTime = prayerTimes.fajr;
                break;
            case SUNRISE:
                prayerTime = prayerTimes.sunrise;
                break;
            case DHUHR:
                prayerTime = prayerTimes.dhuhr;
                break;
            case ASR:
                prayerTime = prayerTimes.asr;
                break;
            case MAGHRIB:
                prayerTime = prayerTimes.maghrib;
                break;
            case ISHA:
                prayerTime = prayerTimes.isha;
                break;
            default:
                return null;
        }

        // If prayer time is in the past and it's Fajr, calculate tomorrow's Fajr
        if (prayerTime != null && prayer == Prayer.FAJR) {
            Date now = new Date();
            if (prayerTime.getTime() < now.getTime()) {
                // Calculate tomorrow's prayer times
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);

                Coordinates coordinates = new Coordinates(latitude, longitude);
                DateComponents tomorrowDate = DateComponents.from(tomorrow.getTime());
                CalculationParameters parameters = getCalculationParameters();
                PrayerTimes tomorrowPrayers = new PrayerTimes(coordinates, tomorrowDate, parameters);

                prayerTime = tomorrowPrayers.fajr;
            }
        }

        return prayerTime;
    }

    public String getTimeUntilNextPrayer() {
        Date nextPrayerTime = getNextPrayerTime();
        android.util.Log.d("PrayerCalc", "getTimeUntilNextPrayer: nextPrayerTime = " + nextPrayerTime);
        if (nextPrayerTime == null) {
            android.util.Log.e("PrayerCalc", "getTimeUntilNextPrayer: nextPrayerTime is NULL, returning empty string");
            return "";
        }

        Date now = new Date();
        long diffInMillis = nextPrayerTime.getTime() - now.getTime();
        android.util.Log.d("PrayerCalc", "getTimeUntilNextPrayer: diffInMillis = " + diffInMillis);

        if (diffInMillis < 0) {
            android.util.Log.w("PrayerCalc", "getTimeUntilNextPrayer: diffInMillis < 0, returning 00:00:00");
            return "00:00:00";
        }

        long hours = diffInMillis / (1000 * 60 * 60);
        long minutes = (diffInMillis / (1000 * 60)) % 60;
        long seconds = (diffInMillis / 1000) % 60;

        String result = String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds);
        android.util.Log.d("PrayerCalc", "getTimeUntilNextPrayer: returning " + result);
        return result;
    }

    public String getFormattedCountdownToNextPrayer() {
        Prayer nextPrayer = getNextPrayer();
        if (nextPrayer == null) {
            return "";
        }

        Date nextPrayerTime = getNextPrayerTime();
        if (nextPrayerTime == null) {
            return "";
        }

        Date now = new Date();
        long diffInMillis = nextPrayerTime.getTime() - now.getTime();

        if (diffInMillis < 0) {
            return getUrduPrayerName(nextPrayer) + " کا وقت ہو گیا ہے";
        }

        long hours = diffInMillis / (1000 * 60 * 60);
        long minutes = (diffInMillis / (1000 * 60)) % 60;

        String prayerName = getUrduPrayerName(nextPrayer);

        // Format: "remaining 2h:30m to عصر"
        return String.format(Locale.ENGLISH, "remaining %dh:%02dm to %s", hours, minutes, prayerName);
    }

    public String getSunriseTime() {
        if (prayerTimes.sunrise != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            return formatter.format(prayerTimes.sunrise);
        }
        return "";
    }

    public String getDayLength() {
        if (prayerTimes.sunrise != null && prayerTimes.maghrib != null) {
            long diffInMillis = prayerTimes.maghrib.getTime() - prayerTimes.sunrise.getTime();
            long hours = diffInMillis / (1000 * 60 * 60);
            long minutes = (diffInMillis / (1000 * 60)) % 60;
            return String.format(Locale.ENGLISH, "%d گھنٹے %d منٹ", hours, minutes);
        }
        return "";
    }

    public String getNightLength() {
        // Night length is from Maghrib to next day's Fajr
        // Approximation: 24 hours - day length
        if (prayerTimes.sunrise != null && prayerTimes.maghrib != null) {
            long dayLengthMillis = prayerTimes.maghrib.getTime() - prayerTimes.sunrise.getTime();
            long nightLengthMillis = (24 * 60 * 60 * 1000) - dayLengthMillis;
            long hours = nightLengthMillis / (1000 * 60 * 60);
            long minutes = (nightLengthMillis / (1000 * 60)) % 60;
            return String.format(Locale.ENGLISH, "%d گھنٹے %d منٹ", hours, minutes);
        }
        return "";
    }

    public String getMiddleOfNight() {
        if (sunnahTimes.middleOfTheNight != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            return formatter.format(sunnahTimes.middleOfTheNight);
        }
        return "";
    }

    public String getZawalTime() {
        // Zawal is approximately 5 minutes before Dhuhr
        if (prayerTimes.dhuhr != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(prayerTimes.dhuhr);
            cal.add(Calendar.MINUTE, -5);
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            return formatter.format(cal.getTime());
        }
        return "";
    }

    public Date getMaghribDate() {
        return prayerTimes.maghrib;
    }

    public String[] getAllPrayerTimesFormatted() {
        return new String[]{
                getUrduPrayerName(Prayer.FAJR) + ": " + getPrayerTime(Prayer.FAJR),
                getUrduPrayerName(Prayer.DHUHR) + ": " + getPrayerTime(Prayer.DHUHR),
                getUrduPrayerName(Prayer.ASR) + ": " + getPrayerTime(Prayer.ASR),
                getUrduPrayerName(Prayer.MAGHRIB) + ": " + getPrayerTime(Prayer.MAGHRIB),
                getUrduPrayerName(Prayer.ISHA) + ": " + getPrayerTime(Prayer.ISHA)
        };
    }
}
