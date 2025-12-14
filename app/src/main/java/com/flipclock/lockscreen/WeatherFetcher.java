package com.flipclock.lockscreen;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherFetcher {

    // API Keys
    private static final String OPENWEATHER_API_KEY = "bd5e378503939ddaee76f12ad7a97608";
    private static final String WEATHERAPI_KEY = "4dd3cf3146c542c2b8b80449241123"; // Free tier

    // API URLs
    private static final String[] API_NAMES = {"Open-Meteo", "WeatherAPI", "OpenWeather"};
    private static final int MAX_TRIES_PER_API = 2;
    private static final int TOTAL_APIS = 3;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface WeatherCallback {
        void onWeatherFetched(WeatherData data);
        void onError(String error);
        void onProgress(String apiName, int apiIndex, int tryNumber);
    }

    public void fetchWeather(double latitude, double longitude, WeatherCallback callback) {
        // Start with first API
        fetchWithFallback(latitude, longitude, callback, 0, 0);
    }

    private void fetchWithFallback(double latitude, double longitude, WeatherCallback callback,
                                    int apiIndex, int tryNumber) {
        // Check if we've exhausted all APIs
        if (apiIndex >= TOTAL_APIS) {
            android.util.Log.e("WeatherFetcher", "All APIs failed after all retry attempts");
            mainHandler.post(() -> callback.onError("All weather APIs failed"));
            return;
        }

        // Check if we've exhausted tries for current API
        if (tryNumber >= MAX_TRIES_PER_API) {
            android.util.Log.d("WeatherFetcher", "Moving to next API after " + tryNumber + " failed attempts");
            // Move to next API
            fetchWithFallback(latitude, longitude, callback, apiIndex + 1, 0);
            return;
        }

        // Check if executor is shut down
        if (executor.isShutdown()) {
            android.util.Log.d("WeatherFetcher", "Executor was shut down, creating new one");
            executor = Executors.newSingleThreadExecutor();
        }

        String apiName = API_NAMES[apiIndex];
        int displayTry = tryNumber + 1;
        android.util.Log.d("WeatherFetcher", "Fetching from " + apiName + " (API " + (apiIndex + 1) + "/" + TOTAL_APIS + ", Try " + displayTry + "/" + MAX_TRIES_PER_API + ")");

        // Update progress on UI
        mainHandler.post(() -> callback.onProgress(apiName, apiIndex + 1, displayTry));

        executor.execute(() -> {
            try {
                WeatherData data = null;

                switch (apiIndex) {
                    case 0: // Open-Meteo (Free, no key needed)
                        data = fetchFromOpenMeteo(latitude, longitude);
                        break;
                    case 1: // WeatherAPI.com
                        data = fetchFromWeatherAPI(latitude, longitude);
                        break;
                    case 2: // OpenWeatherMap
                        data = fetchFromOpenWeather(latitude, longitude);
                        break;
                }

                if (data != null) {
                    data.lastFetchTimestamp = System.currentTimeMillis();
                    android.util.Log.d("WeatherFetcher", "Successfully fetched weather from " + apiName);
                    WeatherData finalData = data;
                    mainHandler.post(() -> callback.onWeatherFetched(finalData));
                } else {
                    throw new Exception("Failed to parse data from " + apiName);
                }

            } catch (Exception e) {
                android.util.Log.e("WeatherFetcher", apiName + " attempt " + displayTry + " failed: " + e.getMessage(), e);

                // Wait 3 seconds before retry
                mainHandler.postDelayed(() -> {
                    fetchWithFallback(latitude, longitude, callback, apiIndex, tryNumber + 1);
                }, 3000);
            }
        });
    }

    // Open-Meteo API (Free, no API key needed)
    private WeatherData fetchFromOpenMeteo(double latitude, double longitude) throws Exception {
        String currentUrl = String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,pressure_msl,cloud_cover&timezone=auto",
            latitude, longitude);

        String forecastUrl = String.format(Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=7",
            latitude, longitude);

        String currentJson = makeHttpRequest(currentUrl);
        String forecastJson = makeHttpRequest(forecastUrl);

        return parseOpenMeteoData(currentJson, forecastJson, latitude, longitude);
    }

    // WeatherAPI.com
    private WeatherData fetchFromWeatherAPI(double latitude, double longitude) throws Exception {
        String url = String.format(Locale.US,
            "http://api.weatherapi.com/v1/forecast.json?key=%s&q=%f,%f&days=7&aqi=no",
            WEATHERAPI_KEY, latitude, longitude);

        String json = makeHttpRequest(url);
        return parseWeatherAPIData(json);
    }

    // OpenWeatherMap
    private WeatherData fetchFromOpenWeather(double latitude, double longitude) throws Exception {
        WeatherData data = new WeatherData();

        String currentUrl = String.format(Locale.US,
            "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=metric&appid=%s",
            latitude, longitude, OPENWEATHER_API_KEY);

        String forecastUrl = String.format(Locale.US,
            "https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&units=metric&appid=%s",
            latitude, longitude, OPENWEATHER_API_KEY);

        String currentJson = makeHttpRequest(currentUrl);
        parseOpenWeatherCurrent(currentJson, data);

        String forecastJson = makeHttpRequest(forecastUrl);
        parseOpenWeatherForecast(forecastJson, data);

        return data;
    }

    private String makeHttpRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("HTTP error code: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();

        return response.toString();
    }

    // Parse Open-Meteo data
    private WeatherData parseOpenMeteoData(String currentJson, String forecastJson, double lat, double lon) throws Exception {
        WeatherData data = new WeatherData();
        JSONObject current = new JSONObject(currentJson);
        JSONObject forecast = new JSONObject(forecastJson);

        JSONObject currentData = current.getJSONObject("current");

        double temp = currentData.getDouble("temperature_2m");
        data.temperature = Math.round(temp) + "°";
        data.feelsLike = Math.round(currentData.getDouble("apparent_temperature")) + "°";
        data.humidity = currentData.getInt("relative_humidity_2m") + "%";
        data.pressure = Math.round(currentData.getDouble("pressure_msl")) + " hPa";
        data.windSpeed = Math.round(currentData.getDouble("wind_speed_10m") * 3.6) + " km/h";
        data.cloudCover = currentData.getInt("cloud_cover") + "%";

        int weatherCode = currentData.getInt("weather_code");
        data.condition = getConditionFromWMOCode(weatherCode);
        data.conditionUrdu = getUrduCondition(data.condition);

        data.visibility = "10 km"; // Default
        data.uvIndex = estimateUVIndex(currentData.getInt("cloud_cover"));

        int humid = currentData.getInt("relative_humidity_2m");
        double dewPoint = temp - ((100 - humid) / 5.0);
        data.dewPoint = Math.round(dewPoint) + "°";

        data.airQualityIndex = "معمولی";
        data.airQualityUrdu = "ہوا صاف ہے";
        data.cityName = "Location"; // Open-Meteo doesn't provide city name

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
        data.lastUpdated = sdf.format(new Date()).replace("AM", "am").replace("PM", "pm");

        // Parse forecast
        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray times = daily.getJSONArray("time");
        JSONArray maxTemps = daily.getJSONArray("temperature_2m_max");
        JSONArray minTemps = daily.getJSONArray("temperature_2m_min");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray precip = daily.getJSONArray("precipitation_probability_max");

        String[] urduDays = {"اتوار", "پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ"};
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < Math.min(7, times.length()); i++) {
            WeatherData.DayForecast day = data.forecast[i];

            String dateStr = times.getString(i);
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date date = parser.parse(dateStr);
            cal.setTime(date);

            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            day.dayNameUrdu = urduDays[dayOfWeek];

            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            day.date = formatter.format(date);

            day.maxTemp = Math.round(maxTemps.getDouble(i)) + "°";
            day.minTemp = Math.round(minTemps.getDouble(i)) + "°";

            int code = codes.getInt(i);
            day.condition = getConditionFromWMOCode(code);
            day.conditionUrdu = getUrduCondition(day.condition);
            day.icon = getWeatherIcon(day.condition);
            day.precipitation = precip.getInt(i) + "%";
            day.humidity = data.humidity; // Reuse current humidity
        }

        return data;
    }

    // Parse WeatherAPI.com data
    private WeatherData parseWeatherAPIData(String json) throws Exception {
        WeatherData data = new WeatherData();
        JSONObject root = new JSONObject(json);

        JSONObject location = root.getJSONObject("location");
        data.cityName = location.getString("name");

        JSONObject current = root.getJSONObject("current");
        data.temperature = Math.round(current.getDouble("temp_c")) + "°";
        data.feelsLike = Math.round(current.getDouble("feelslike_c")) + "°";
        data.humidity = current.getInt("humidity") + "%";
        data.pressure = Math.round(current.getDouble("pressure_mb")) + " hPa";
        data.windSpeed = Math.round(current.getDouble("wind_kph")) + " km/h";
        data.cloudCover = current.getInt("cloud") + "%";
        data.visibility = Math.round(current.getDouble("vis_km")) + " km";
        data.uvIndex = "UV: " + Math.round(current.getDouble("uv"));

        JSONObject condition = current.getJSONObject("condition");
        String conditionText = condition.getString("text");
        data.condition = normalizeCondition(conditionText);
        data.conditionUrdu = getUrduCondition(data.condition);

        double temp = current.getDouble("temp_c");
        int humid = current.getInt("humidity");
        double dewPoint = temp - ((100 - humid) / 5.0);
        data.dewPoint = Math.round(dewPoint) + "°";

        data.airQualityIndex = "معمولی";
        data.airQualityUrdu = "ہوا صاف ہے";

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
        data.lastUpdated = sdf.format(new Date()).replace("AM", "am").replace("PM", "pm");

        // Parse forecast
        JSONObject forecast = root.getJSONObject("forecast");
        JSONArray forecastDays = forecast.getJSONArray("forecastday");

        String[] urduDays = {"اتوار", "پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ"};
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < Math.min(7, forecastDays.length()); i++) {
            JSONObject forecastDay = forecastDays.getJSONObject(i);
            WeatherData.DayForecast day = data.forecast[i];

            String dateStr = forecastDay.getString("date");
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date date = parser.parse(dateStr);
            cal.setTime(date);

            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            day.dayNameUrdu = urduDays[dayOfWeek];

            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            day.date = formatter.format(date);

            JSONObject dayData = forecastDay.getJSONObject("day");
            day.maxTemp = Math.round(dayData.getDouble("maxtemp_c")) + "°";
            day.minTemp = Math.round(dayData.getDouble("mintemp_c")) + "°";
            day.humidity = dayData.getInt("avghumidity") + "%";
            day.precipitation = dayData.getInt("daily_chance_of_rain") + "%";

            JSONObject dayCondition = dayData.getJSONObject("condition");
            String dayCondText = dayCondition.getString("text");
            day.condition = normalizeCondition(dayCondText);
            day.conditionUrdu = getUrduCondition(day.condition);
            day.icon = getWeatherIcon(day.condition);
        }

        return data;
    }

    // Parse OpenWeatherMap current weather
    private void parseOpenWeatherCurrent(String json, WeatherData data) throws Exception {
        JSONObject obj = new JSONObject(json);

        JSONObject main = obj.getJSONObject("main");
        data.temperature = Math.round(main.getDouble("temp")) + "°";
        data.feelsLike = Math.round(main.getDouble("feels_like")) + "°";
        data.humidity = main.getInt("humidity") + "%";
        data.pressure = main.getInt("pressure") + " hPa";

        JSONObject wind = obj.getJSONObject("wind");
        data.windSpeed = Math.round(wind.getDouble("speed") * 3.6) + " km/h";

        JSONArray weather = obj.getJSONArray("weather");
        String condition = weather.getJSONObject(0).getString("main");
        data.condition = condition;
        data.conditionUrdu = getUrduCondition(condition);

        data.cityName = obj.getString("name");

        if (obj.has("visibility")) {
            data.visibility = (obj.getInt("visibility") / 1000) + " km";
        } else {
            data.visibility = "N/A";
        }

        JSONObject clouds = obj.getJSONObject("clouds");
        data.cloudCover = clouds.getInt("all") + "%";

        double temp = main.getDouble("temp");
        int humid = main.getInt("humidity");
        double dewPoint = temp - ((100 - humid) / 5.0);
        data.dewPoint = Math.round(dewPoint) + "°";

        data.uvIndex = estimateUVIndex(clouds.getInt("all"));
        data.airQualityIndex = "معمولی";
        data.airQualityUrdu = "ہوا صاف ہے";

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
        data.lastUpdated = sdf.format(new Date()).replace("AM", "am").replace("PM", "pm");
    }

    // Parse OpenWeatherMap forecast
    private void parseOpenWeatherForecast(String json, WeatherData data) throws Exception {
        JSONObject obj = new JSONObject(json);
        JSONArray list = obj.getJSONArray("list");

        String[] urduDays = {"اتوار", "پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ"};

        java.util.Map<Integer, java.util.List<JSONObject>> dayMap = new java.util.HashMap<>();
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            long dt = item.getLong("dt");
            Date date = new Date(dt * 1000);
            cal.setTime(date);

            int dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR);

            if (!dayMap.containsKey(dayKey)) {
                dayMap.put(dayKey, new java.util.ArrayList<>());
            }
            dayMap.get(dayKey).add(item);
        }

        java.util.List<Integer> sortedDays = new java.util.ArrayList<>(dayMap.keySet());
        java.util.Collections.sort(sortedDays);

        int forecastIndex = 0;
        for (Integer dayKey : sortedDays) {
            if (forecastIndex >= 7) break;

            java.util.List<JSONObject> dayItems = dayMap.get(dayKey);
            if (dayItems.isEmpty()) continue;

            WeatherData.DayForecast forecast = data.forecast[forecastIndex];

            double maxTemp = -999;
            double minTemp = 999;
            double totalHumidity = 0;
            String condition = "";
            double maxPop = 0;

            for (JSONObject item : dayItems) {
                JSONObject main = item.getJSONObject("main");
                double tempMax = main.getDouble("temp_max");
                double tempMin = main.getDouble("temp_min");

                if (tempMax > maxTemp) maxTemp = tempMax;
                if (tempMin < minTemp) minTemp = tempMin;

                totalHumidity += main.getInt("humidity");

                if (item.has("pop")) {
                    double pop = item.getDouble("pop");
                    if (pop > maxPop) maxPop = pop;
                }

                if (condition.isEmpty()) {
                    JSONArray weather = item.getJSONArray("weather");
                    condition = weather.getJSONObject(0).getString("main");
                }
            }

            forecast.maxTemp = Math.round(maxTemp) + "°";
            forecast.minTemp = Math.round(minTemp) + "°";
            forecast.humidity = Math.round(totalHumidity / dayItems.size()) + "%";
            forecast.condition = condition;
            forecast.conditionUrdu = getUrduCondition(condition);
            forecast.icon = getWeatherIcon(condition);
            forecast.precipitation = Math.round(maxPop * 100) + "%";

            JSONObject firstItem = dayItems.get(0);
            long dt = firstItem.getLong("dt");
            Date date = new Date(dt * 1000);
            cal.setTime(date);

            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            forecast.dayNameUrdu = urduDays[dayOfWeek];

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            forecast.date = sdf.format(date);

            forecastIndex++;
        }

        // Pad remaining days if needed
        while (forecastIndex < 7 && forecastIndex > 0) {
            WeatherData.DayForecast lastForecast = data.forecast[forecastIndex - 1];
            WeatherData.DayForecast forecast = data.forecast[forecastIndex];

            forecast.maxTemp = lastForecast.maxTemp;
            forecast.minTemp = lastForecast.minTemp;
            forecast.humidity = lastForecast.humidity;
            forecast.condition = lastForecast.condition;
            forecast.conditionUrdu = lastForecast.conditionUrdu;
            forecast.icon = lastForecast.icon;
            forecast.precipitation = lastForecast.precipitation;

            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, forecastIndex);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            forecast.dayNameUrdu = urduDays[dayOfWeek];

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            forecast.date = sdf.format(cal.getTime());

            forecastIndex++;
        }
    }

    // Convert WMO weather code to condition
    private String getConditionFromWMOCode(int code) {
        if (code == 0) return "Clear";
        if (code <= 3) return "Clouds";
        if (code <= 48) return "Fog";
        if (code <= 67) return "Rain";
        if (code <= 77) return "Snow";
        if (code <= 82) return "Rain";
        if (code <= 86) return "Snow";
        if (code <= 99) return "Thunderstorm";
        return "Clear";
    }

    // Normalize condition text
    private String normalizeCondition(String text) {
        text = text.toLowerCase();
        if (text.contains("clear") || text.contains("sunny")) return "Clear";
        if (text.contains("cloud") || text.contains("overcast")) return "Clouds";
        if (text.contains("rain") || text.contains("drizzle")) return "Rain";
        if (text.contains("thunder") || text.contains("storm")) return "Thunderstorm";
        if (text.contains("snow") || text.contains("sleet")) return "Snow";
        if (text.contains("mist") || text.contains("fog")) return "Mist";
        return "Clear";
    }

    private String getUrduCondition(String condition) {
        switch (condition.toLowerCase()) {
            case "clear": return "صاف";
            case "clouds": return "ابر آلود";
            case "rain": return "بارش";
            case "drizzle": return "ہلکی بارش";
            case "thunderstorm": return "آندھی";
            case "snow": return "برف باری";
            case "mist":
            case "fog": return "دھند";
            case "haze": return "دھواں";
            case "dust": return "گرد";
            case "smoke": return "دھواں";
            default: return condition;
        }
    }

    private String getWeatherIcon(String condition) {
        switch (condition.toLowerCase()) {
            case "clear": return "☀️";
            case "clouds": return "☁️";
            case "rain": return "🌧️";
            case "drizzle": return "🌦️";
            case "thunderstorm": return "⛈️";
            case "snow": return "🌨️";
            case "mist":
            case "fog": return "🌫️";
            default: return "🌤️";
        }
    }

    private String estimateUVIndex(int cloudCover) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 10 && hour <= 16) {
            if (cloudCover < 30) return "زیادہ (7-8)";
            else if (cloudCover < 70) return "معمولی (4-6)";
            else return "کم (1-3)";
        } else {
            return "کم (0-2)";
        }
    }

    public void shutdown() {
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
