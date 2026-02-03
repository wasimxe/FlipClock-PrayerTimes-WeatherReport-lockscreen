# Prayer Clock & Weather - Islamic Lockscreen

<div align="center">

**A Feature-Rich Android Lockscreen with Islamic Prayer Times, Real-Time Weather, and Elegant Flip Clock Design**

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)
![Language](https://img.shields.io/badge/Language-Java-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

</div>

## Overview

Prayer Clock & Weather transforms your Android lockscreen into a beautiful, functional display featuring accurate Islamic prayer times, comprehensive weather information, and a stunning customizable flip clock. Designed with both aesthetics and functionality in mind, this app serves as a complete replacement for your device's default lockscreen.

## Key Features

### 🕌 Islamic Prayer Times
- **Accurate Prayer Calculations** - Displays all 5 daily prayers (Fajr, Dhuhr, Asr, Maghrib, Isha)
- **Live Countdown** - Real-time countdown to the next prayer with hours and minutes
- **Madhab Support** - Choose between Shafi'i and Hanafi calculation methods for Asr prayer
- **Additional Islamic Information**:
  - Sunrise time (Tuloo' Aftaab)
  - Solar Noon / Zawal time
  - Day and Night duration
  - Middle of the Night (Nisf Al-Layl)
- **Dual Calendar Display** - Alternates between Gregorian and Hijri (Islamic) calendar
- **Full Urdu Support** - Prayer names, calendar, and labels in beautiful Urdu typography

### 🌤️ Comprehensive Weather Integration
- **Real-Time Weather Data** - Current conditions with temperature, feels-like, and weather status
- **7-Day Forecast** - Week-ahead weather prediction with min/max temperatures
- **Detailed Metrics**:
  - Humidity percentage
  - Wind speed
  - Atmospheric pressure
  - UV Index
  - Visibility distance
  - Dew point
  - Cloud coverage
- **Health Alerts** - Temperature-based health recommendations in Urdu
- **Smart Updates** - Automatic weather refresh every 24 hours with failover across multiple APIs
- **Location-Based** - Uses GPS for accurate local weather and prayer times
- **Enable/Disable Option** - Toggle weather display on/off as needed

### ⏰ Customizable Flip Clock
- **Multiple Font Options**:
  - Digital-7 (Classic digital display)
  - Seven Segment (LED-style)
  - Orbitron Bold (Modern futuristic)
  - Monospace Bold (Clean and professional)
- **Adjustable Size** - Clock size from 100sp to 400sp
- **Custom Colors** - Full RGB color picker for personalization
- **Smart Battery Indicator** - Clock turns red when device is unplugged (customizable color when charging)

### 🎛️ Advanced Gesture Controls
- **Horizontal Swipe** - Adjust screen brightness
  - Extra Dark Mode: Supports brightness below 0% using overlay
  - Visual feedback with percentage indicator
  - Configurable brightness limit (default -5%)
- **Vertical Swipe** - Control device volume (can be toggled off)
  - Real-time volume adjustment
  - Visual volume level indicator
- **Single Tap** - Cycle through views (Clock → Prayer Times → Weather → Clock)
- **Double Tap** - Quick unlock to return to your device
- **180° Rotation** - Flip display orientation for versatile placement

### 🚀 System Integration
- **Lockscreen Replacement** - Full lockscreen functionality
- **Auto-Start on Boot** - Launches automatically when device starts
- **Screen Wake Integration** - Displays when screen turns on
- **Battery Optimized** - Efficient update intervals to preserve battery life
- **Permission Management** - Guided setup for all required permissions
- **Foreground Service** - Reliable background operation

## Screenshots

> Add screenshots of your app showing:
> - Main clock display
> - Prayer times overlay
> - Weather information screen
> - Settings/configuration panel

## Technical Specifications

### Requirements
- **Minimum Android Version**: Android 7.0 (API 24)
- **Target Android Version**: Android 14 (API 34)
- **Permissions Required**:
  - Overlay (SYSTEM_ALERT_WINDOW) - Display over lockscreen
  - Location (ACCESS_FINE_LOCATION) - Prayer times and weather
  - Internet - Weather data fetching
  - Wake Lock - Screen wake detection
  - Write Settings - Brightness control
  - Boot Completed - Auto-start functionality
  - Notification Listener - Optional notification integration

### Technology Stack
- **Language**: Java
- **Build System**: Gradle
- **Key Libraries**:
  - `androidx.appcompat` - Modern Android compatibility
  - `com.google.android.material` - Material Design components
  - `com.batoulapps.adhan:1.2.0` - Islamic prayer time calculations
  - `play-services-location:21.0.1` - GPS location services
  - Custom weather API integration with automatic failover

### Architecture
- Service-based architecture for background operation
- SharedPreferences for user settings persistence
- Weather data caching for offline functionality
- Broadcast receivers for system events (boot, screen on/off, battery status)
- Gesture detection system for intuitive controls

## Installation

### From Source
1. Clone the repository:
   ```bash
   git clone https://github.com/wasimxe/FlipClock-PrayerTimes-WeatherReport-lockscreen.git
   ```

2. Open the project in Android Studio

3. Build and run on your Android device:
   ```bash
   ./gradlew assembleDebug
   ```

### Setup Instructions
1. **Grant Permissions**: On first launch, grant overlay permission when prompted
2. **Enable Location**: Allow location access for prayer times and weather
3. **Get Location**: Tap "Get Location" button to fetch your coordinates
4. **Choose Madhab**: Select your preferred Asr calculation method (Shafi or Hanafi)
5. **Customize Appearance**:
   - Select your preferred clock font
   - Adjust font size using the slider
   - Choose your custom clock color
   - Set brightness limit if needed
6. **Configure Features**:
   - Toggle weather on/off
   - Enable/disable volume swipe gesture
   - Set 180° rotation if needed
7. **Enable Lockscreen**: Toggle the service switch to activate

## Usage Guide

### Main Interface
The lockscreen displays the current time in large flip-style digits with the date below. The interface automatically shows when your screen wakes up.

### Viewing Prayer Times
- **Single Tap**: Cycles to prayer times view
- Displays all 5 daily prayers with exact times
- Shows countdown to next prayer prominently
- Rotating display of additional Islamic time information

### Checking Weather
- **Single Tap** (from Prayer view): Switches to weather display
- Current conditions with detailed metrics
- 7-day forecast cards
- Health alerts based on temperature

### Adjusting Brightness
- **Swipe Left/Right**: Increase or decrease screen brightness
- Supports extra dark mode (negative brightness percentages)
- Real-time visual feedback

### Controlling Volume
- **Swipe Up/Down**: Adjust ringer volume
- Can be disabled in settings if not needed
- Real-time volume level indicator

### Unlocking Device
- **Double Tap**: Returns to system unlock screen (PIN/Pattern/Fingerprint)

## Configuration Options

| Setting | Description | Default |
|---------|-------------|---------|
| Clock Font | Choose display font style | Digital-7 |
| Font Size | Adjust clock size (100-400sp) | 200sp |
| Clock Color | Custom RGB color picker | White (#FFFFFF) |
| Madhab | Asr calculation method | Shafi |
| Weather Toggle | Enable/disable weather display | Enabled |
| Rotation | 180° display flip | Disabled |
| Volume Swipe | Enable/disable volume gesture | Enabled |
| Brightness Limit | Minimum brightness percentage | -5% |

## Project Structure

```
app/src/main/java/com/flipclock/lockscreen/
├── MainActivity.java              # Configuration and settings UI
├── LockScreenActivity.java        # Main lockscreen display
├── LockScreenService.java         # Background service
├── PrayerTimeCalculator.java      # Islamic prayer calculations
├── WeatherFetcher.java           # Multi-API weather fetching
├── WeatherData.java              # Weather data model
├── NotificationListener.java      # Notification integration
└── BootReceiver.java             # Boot auto-start receiver
```

## Development Features

### Highlights for Developers
- **Clean Code Architecture** - Well-organized, maintainable codebase
- **Robust Error Handling** - Graceful fallbacks for all external dependencies
- **Battery Efficient** - Optimized update intervals (15s for clock, 1s for countdown)
- **Multi-API Failover** - Weather fetching tries multiple sources for reliability
- **Persistent Storage** - Smart caching of weather data and user preferences
- **Responsive UI** - Smooth gesture detection and real-time visual feedback
- **Localization Ready** - Urdu language support with custom fonts
- **Material Design** - Modern Android UI/UX principles

### Code Quality
- Comprehensive exception handling
- Memory-efficient data structures
- Proper lifecycle management
- Thread-safe operations
- Resource cleanup in onDestroy()

## Contributing

Contributions are welcome! If you'd like to improve this project:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Ideas for Contributions
- Additional language support (Arabic, Persian, etc.)
- More weather providers
- Widget support
- Notification display integration
- Customizable swipe gestures
- Theme presets
- Prayer time notifications/alarms

## Known Limitations
- Requires Android 7.0 or higher
- Weather updates limited to once per 24 hours to conserve data
- Brightness control requires WRITE_SETTINGS permission
- Some devices may restrict overlay drawing in battery saver mode

## Troubleshooting

**Lockscreen not showing?**
- Ensure overlay permission is granted in Settings
- Check that the service is running (green indicator in app)
- Verify battery optimization is disabled for the app

**Prayer times incorrect?**
- Confirm location permission is granted
- Press "Get Location" button to update coordinates
- Check madhab setting matches your preference

**Weather not updating?**
- Verify internet connection is active
- Ensure location services are enabled
- Weather updates automatically every 24 hours

**Brightness/Volume swipe not working?**
- Grant WRITE_SETTINGS permission when prompted
- Check that gestures aren't disabled in settings
- Try swiping from the center of the screen

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **Batoulapps Adhan Library** - Accurate Islamic prayer time calculations
- **Google Play Services** - Location services
- **Multiple Weather APIs** - Reliable weather data
- **Urdu Fonts** - Beautiful typography for Urdu text (Gulzar-Regular)
- **Open Source Community** - For inspiration and support

## Contact

**Developer**: Wasim
**GitHub**: [@wasimxe](https://github.com/wasimxe)
**Repository**: [FlipClock-PrayerTimes-WeatherReport-lockscreen](https://github.com/wasimxe/FlipClock-PrayerTimes-WeatherReport-lockscreen)

---

<div align="center">

**If you find this project useful, please consider giving it a ⭐ star on GitHub!**

Made with ❤️ for the Muslim community

</div>
