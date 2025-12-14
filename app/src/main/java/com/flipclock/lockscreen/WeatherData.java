package com.flipclock.lockscreen;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherData {
    // Current weather
    public String temperature;
    public String feelsLike;
    public String condition;
    public String conditionUrdu;
    public String humidity;
    public String pressure;
    public String windSpeed;
    public String uvIndex;
    public String visibility;
    public String dewPoint;
    public String cloudCover;
    public String airQualityIndex;
    public String airQualityUrdu;

    // 7-day forecast
    public DayForecast[] forecast;

    // Location
    public String cityName;
    public String lastUpdated;

    // Timestamp tracking
    public long lastFetchTimestamp;

    public static class DayForecast {
        public String dayName;
        public String dayNameUrdu;
        public String date;
        public String maxTemp;
        public String minTemp;
        public String condition;
        public String conditionUrdu;
        public String icon;
        public String precipitation;
        public String humidity;
    }

    public WeatherData() {
        forecast = new DayForecast[7];
        for (int i = 0; i < 7; i++) {
            forecast[i] = new DayForecast();
        }
    }

    public String getHealthAlert() {
        try {
            int temp = Integer.parseInt(temperature.replace("°", "").trim());
            int humid = Integer.parseInt(humidity.replace("%", "").trim());

            if (temp > 40) {
                return "انتہائی گرمی - باہر نہ نکلیں";
            } else if (temp < 5) {
                return "شدید سردی - گرم کپڑے پہنیں";
            } else if (humid < 30) {
                return "خشک ہوا - پانی زیادہ پیئیں";
            } else if (humid > 80) {
                return "زیادہ نمی - احتیاط کریں";
            }
            return "موسم موزوں ہے";
        } catch (Exception e) {
            return "موسم موزوں ہے";
        }
    }

    // Save weather data to SharedPreferences
    public void saveToPreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("FlipClockPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            JSONObject json = new JSONObject();
            json.put("temperature", temperature);
            json.put("feelsLike", feelsLike);
            json.put("condition", condition);
            json.put("conditionUrdu", conditionUrdu);
            json.put("humidity", humidity);
            json.put("pressure", pressure);
            json.put("windSpeed", windSpeed);
            json.put("uvIndex", uvIndex);
            json.put("visibility", visibility);
            json.put("dewPoint", dewPoint);
            json.put("cloudCover", cloudCover);
            json.put("airQualityIndex", airQualityIndex);
            json.put("airQualityUrdu", airQualityUrdu);
            json.put("cityName", cityName);
            json.put("lastUpdated", lastUpdated);
            json.put("lastFetchTimestamp", lastFetchTimestamp);

            // Save forecast array
            JSONArray forecastArray = new JSONArray();
            for (int i = 0; i < forecast.length; i++) {
                if (forecast[i] != null) {
                    JSONObject dayJson = new JSONObject();
                    dayJson.put("dayName", forecast[i].dayName);
                    dayJson.put("dayNameUrdu", forecast[i].dayNameUrdu);
                    dayJson.put("date", forecast[i].date);
                    dayJson.put("maxTemp", forecast[i].maxTemp);
                    dayJson.put("minTemp", forecast[i].minTemp);
                    dayJson.put("condition", forecast[i].condition);
                    dayJson.put("conditionUrdu", forecast[i].conditionUrdu);
                    dayJson.put("icon", forecast[i].icon);
                    dayJson.put("precipitation", forecast[i].precipitation);
                    dayJson.put("humidity", forecast[i].humidity);
                    forecastArray.put(dayJson);
                }
            }
            json.put("forecast", forecastArray);

            editor.putString("cached_weather_data", json.toString());
            editor.apply();
        } catch (Exception e) {
            android.util.Log.e("WeatherData", "Error saving weather data", e);
        }
    }

    // Load weather data from SharedPreferences
    public static WeatherData loadFromPreferences(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("FlipClockPrefs", Context.MODE_PRIVATE);
            String jsonString = prefs.getString("cached_weather_data", null);

            if (jsonString == null) {
                return null;
            }

            JSONObject json = new JSONObject(jsonString);
            WeatherData data = new WeatherData();

            data.temperature = json.optString("temperature", "N/A");
            data.feelsLike = json.optString("feelsLike", "N/A");
            data.condition = json.optString("condition", "N/A");
            data.conditionUrdu = json.optString("conditionUrdu", "N/A");
            data.humidity = json.optString("humidity", "N/A");
            data.pressure = json.optString("pressure", "N/A");
            data.windSpeed = json.optString("windSpeed", "N/A");
            data.uvIndex = json.optString("uvIndex", "N/A");
            data.visibility = json.optString("visibility", "N/A");
            data.dewPoint = json.optString("dewPoint", "N/A");
            data.cloudCover = json.optString("cloudCover", "N/A");
            data.airQualityIndex = json.optString("airQualityIndex", "N/A");
            data.airQualityUrdu = json.optString("airQualityUrdu", "N/A");
            data.cityName = json.optString("cityName", "N/A");
            data.lastUpdated = json.optString("lastUpdated", "N/A");
            data.lastFetchTimestamp = json.optLong("lastFetchTimestamp", 0);

            // Load forecast array
            JSONArray forecastArray = json.optJSONArray("forecast");
            if (forecastArray != null) {
                for (int i = 0; i < forecastArray.length() && i < 7; i++) {
                    JSONObject dayJson = forecastArray.getJSONObject(i);
                    DayForecast day = data.forecast[i];
                    day.dayName = dayJson.optString("dayName", "N/A");
                    day.dayNameUrdu = dayJson.optString("dayNameUrdu", "N/A");
                    day.date = dayJson.optString("date", "N/A");
                    day.maxTemp = dayJson.optString("maxTemp", "N/A");
                    day.minTemp = dayJson.optString("minTemp", "N/A");
                    day.condition = dayJson.optString("condition", "N/A");
                    day.conditionUrdu = dayJson.optString("conditionUrdu", "N/A");
                    day.icon = dayJson.optString("icon", "🌤️");
                    day.precipitation = dayJson.optString("precipitation", "N/A");
                    day.humidity = dayJson.optString("humidity", "N/A");
                }
            }

            return data;
        } catch (Exception e) {
            android.util.Log.e("WeatherData", "Error loading weather data", e);
            return null;
        }
    }
}
