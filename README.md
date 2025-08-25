# 🧠 Brave Brain

**Be brave, train your brain, reclaim your focus**

<div align="center">
  <img src="app/src/main/res/drawable/logo_no_bg.png" alt="Brave Brain Logo" width="200"/>
</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Permissions](#-permissions)
- [Usage](#-usage)
- [Architecture](#-architecture)
- [Technical Details](#-technical-details)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎯 Overview

**Brave Brain** is a powerful Android application designed to help users break free from digital addiction and regain control over their screen time. In our hyper-connected world, it's easy to fall into the trap of endless scrolling and mindless app usage. Brave Brain provides intelligent app blocking, time tracking, and gamified challenges to promote healthier digital habits.

### Why Brave Brain?

- 🎯 **Smart Blocking**: Automatically blocks distracting apps when time limits are reached
- 📊 **Real-time Monitoring**: Tracks app usage with precision and provides detailed insights
- 🧮 **Gamified Challenges**: Math challenges to unlock additional time or access settings
- 🔔 **Gentle Reminders**: Non-intrusive notifications about usage patterns
- 🎨 **Beautiful UI**: Clean, modern Material Design interface
- 🛡️ **Privacy-First**: All data stays on your device

---

## ✨ Features

### 🚫 Intelligent App Blocking
- Select specific apps to monitor and block
- Set daily time limits for each selected app
- Automatic blocking when limits are exceeded
- Immediate redirection to home screen when blocked apps are accessed

### ⏱️ Advanced Time Tracking
- Real-time monitoring of app usage
- Daily usage statistics and progress tracking
- Automatic reset at midnight
- Usage warnings at regular intervals (every 5 minutes)

### 🧮 Math Challenge System
- Solve mathematical problems to access restricted features
- Configurable difficulty levels
- Prevents impulsive bypassing of restrictions
- Encourages mindful decision-making

### 🎨 Intuitive User Interface
- **Onboarding Experience**: Guided setup with clear explanations
- **Material Design**: Modern, clean interface following Android design guidelines
- **Dashboard**: Overview of current status and quick actions
- **App Selection**: Easy-to-use interface for selecting apps to monitor
- **Time Management**: Simple time limit configuration

### 🔐 Comprehensive Permission Management
- Usage access permissions for app monitoring
- Overlay permissions for blocking screens
- Notification permissions for gentle reminders
- Foreground service for reliable background monitoring

### 🔄 Background Monitoring
- Efficient background service with minimal battery impact
- Real-time app detection and blocking
- Persistent monitoring even when the app is closed
- Smart detection algorithms with fallback mechanisms

---

## 📱 Screenshots

*Screenshots will be added once the app is built and running*

---

## 📋 Requirements

- **Android Version**: Android 7.0 (API level 24) or higher
- **Target SDK**: Android 14 (API level 36)
- **RAM**: Minimum 2GB recommended
- **Storage**: 50MB available space
- **Permissions**: Usage Access, Overlay, and Notifications

### Supported Android Versions
- ✅ Android 14 (API 34+)
- ✅ Android 13 (API 33)
- ✅ Android 12 (API 31-32)
- ✅ Android 11 (API 30)
- ✅ Android 10 (API 29)
- ✅ Android 9 (API 28)
- ✅ Android 8 (API 26-27)
- ✅ Android 7 (API 24-25)

---

## 🛠️ Installation

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK with API level 36

### Build from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/Xenonesis/Brave-Brain.git
   cd Brave-Brain
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Click "Open an Existing Project"
   - Navigate to the cloned directory
   - Select the project folder

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

### APK Installation
1. Download the latest APK from the [Releases](https://github.com/Xenonesis/Brave-Brain/releases) page
2. Enable "Install from Unknown Sources" in your device settings
3. Install the APK file
4. Launch the app and complete the onboarding process

---

## 🔐 Permissions

Brave Brain requires specific permissions to function effectively:

### 📊 Usage Access Permission
- **Purpose**: Monitor which apps are currently in use and track usage time
- **Why needed**: Essential for detecting when blocked apps are accessed
- **Privacy**: Data never leaves your device

### 🖼️ Display over Other Apps (Overlay)
- **Purpose**: Show blocking screens over restricted applications
- **Why needed**: Provides immediate feedback when time limits are reached
- **Privacy**: Only used to display blocking interfaces

### 🔔 Notifications
- **Purpose**: Send gentle reminders about usage patterns
- **Why needed**: Keep users informed about their digital habits
- **Privacy**: All notifications are generated locally

### 🔄 Foreground Service
- **Purpose**: Maintain continuous monitoring in the background
- **Why needed**: Ensures blocking works even when the app is not active
- **Privacy**: Service only monitors app usage patterns

---

## 🚀 Usage

### Initial Setup

1. **Launch the App**
   - Complete the interactive onboarding process
   - Learn about features and benefits

2. **Grant Permissions**
   - Usage Access: Settings → Apps → Special app access → Usage access
   - Overlay Permission: Automatically prompted during setup
   - Notifications: Grant when prompted

3. **Select Apps to Monitor**
   - Browse installed applications
   - Select apps you want to limit
   - Common choices: Social media, games, entertainment apps

4. **Set Time Limits**
   - Configure daily limits for each selected app
   - Start with realistic goals (e.g., 30-60 minutes)
   - Adjust limits based on your usage patterns

### Daily Usage

1. **Monitor Progress**
   - Check the main dashboard for current usage
   - View real-time statistics
   - Receive periodic usage updates

2. **Handle Blocks**
   - When time limits are reached, blocking screens appear
   - Choose to return home or complete math challenges
   - Reflect on your digital habits

3. **Adjust Settings**
   - Modify time limits as needed
   - Add or remove monitored apps
   - Complete math challenges to access settings

### Advanced Features

- **Math Challenges**: Solve 3 problems to unlock settings access
- **Time Extensions**: Complete challenges to add extra time
- **Usage Analytics**: Track progress over time
- **Smart Detection**: Automatic app detection with multiple fallback methods

---

## 🏗️ Architecture

Brave Brain follows modern Android development practices:

### 📁 Project Structure
```
app/
├── src/main/
│   ├── java/com/example/testing/
│   │   ├── MainActivity.kt              # Main dashboard
│   │   ├── OnboardingActivity.kt        # Initial setup flow
│   │   ├── AppSelectionActivity.kt      # App selection interface
│   │   ├── TimeLimitActivity.kt         # Time limit configuration
│   │   ├── BlockingActivity.kt          # App blocking screen
│   │   ├── MathChallengeActivity.kt     # Math challenge system
│   │   ├── BlockerService.kt            # Background monitoring service
│   │   ├── OverlayBlockingService.kt    # Overlay management
│   │   └── UsageUtils.kt                # Usage tracking utilities
│   ├── res/
│   │   ├── layout/                      # UI layouts
│   │   ├── drawable/                    # Icons and graphics
│   │   ├── values/                      # Strings, colors, themes
│   │   └── xml/                         # Preferences and rules
│   └── AndroidManifest.xml              # App configuration
└── build.gradle.kts                     # Build configuration
```

### 🔧 Key Components

#### Activities
- **OnboardingActivity**: Guided setup experience with ViewPager2
- **MainActivity**: Central dashboard with SwipeRefreshLayout
- **AppSelectionActivity**: Dual-pane app selection interface
- **TimeLimitActivity**: Time configuration with input validation
- **BlockingActivity**: Full-screen blocking interface
- **MathChallengeActivity**: Challenge system with random problem generation

#### Services
- **BlockerService**: Foreground service for continuous monitoring
- **OverlayBlockingService**: Manages overlay windows for blocking

#### Utilities
- **UsageUtils**: Centralized usage tracking and statistics
- **OnboardingAdapter**: ViewPager adapter for onboarding screens

### 🛠️ Technologies Used

- **Language**: Kotlin 100%
- **UI Framework**: Android Views with Material Design Components
- **Architecture**: MVVM-inspired with SharedPreferences persistence
- **Async Operations**: Handlers and Runnables for background tasks
- **System Integration**: UsageStatsManager, WindowManager, NotificationManager

---

## 🔧 Technical Details

### App Monitoring System

The app uses Android's `UsageStatsManager` to monitor foreground applications:

```kotlin
private fun getForegroundAppPackageName(): String? {
    val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val end = System.currentTimeMillis()
    val begin = end - 10000 // 10-second window
    
    // Query usage events for real-time detection
    val events = usm.queryEvents(begin, end)
    // Process events to find current foreground app
}
```

### Time Tracking Implementation

Usage time is tracked with second-level precision:

```kotlin
private fun trackUsageTime(packageName: String, elapsedSeconds: Int) {
    val prefs = getSharedPreferences("usage_tracking", Context.MODE_PRIVATE)
    val currentUsage = prefs.getInt(packageName, 0)
    prefs.edit().putInt(packageName, currentUsage + elapsedSeconds).apply()
}
```

### Blocking Mechanism

When time limits are exceeded, the app immediately redirects users:

```kotlin
private fun redirectToHome() {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    startActivity(homeIntent)
}
```

### Performance Optimizations

- **Efficient Polling**: 2-second intervals balance responsiveness with battery life
- **Smart Caching**: Minimizes repeated system calls
- **Fallback Detection**: Multiple methods ensure reliable app detection
- **Memory Management**: Proper cleanup prevents memory leaks

---

## 🎨 Customization

### Themes and Colors
The app supports both light and dark themes, automatically adapting to system settings:

- **Primary Colors**: Material Design Blue/Teal palette
- **Typography**: Roboto font family
- **Icons**: Material Design icons with custom illustrations

### Configuration Options
- **Polling Interval**: Adjustable monitoring frequency
- **Challenge Difficulty**: Customizable math problem complexity
- **Notification Frequency**: Configurable reminder intervals
- **UI Preferences**: Theme and layout customizations

---

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] App selection and deselection
- [ ] Time limit configuration
- [ ] Blocking functionality
- [ ] Math challenge completion
- [ ] Permission handling
- [ ] Background service reliability
- [ ] UI responsiveness across different screen sizes

---

## 🤝 Contributing

We welcome contributions to Brain Detox! Here's how you can help:

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

### Contribution Guidelines
- Follow Kotlin coding conventions
- Write clear commit messages
- Add documentation for new features
- Include tests for new functionality
- Respect user privacy and data protection

### Areas for Contribution
- 🌐 Internationalization and localization
- 🎨 UI/UX improvements
- 📊 Advanced analytics and insights
- 🔧 Performance optimizations
- 🧪 Additional testing coverage
- 📚 Documentation improvements

---

## 🐛 Troubleshooting

### Common Issues

#### App Not Blocking
1. Verify Usage Access permission is granted
2. Check if the app is in the selected apps list
3. Ensure time limits are properly configured
4. Restart the app to refresh the service

#### Background Service Stops
1. Disable battery optimization for Brave Brain
2. Check if the app has been force-stopped
3. Verify foreground service permissions
4. Restart the device if issues persist

#### Math Challenges Not Working
1. Ensure overlay permission is granted
2. Check for conflicts with other overlay apps
3. Verify the app has notification permissions
4. Clear app cache and restart

### Performance Tips
- Regularly clear app cache
- Restart the app weekly for optimal performance
- Monitor battery usage and adjust settings accordingly
- Keep the app updated to the latest version

---

## 📞 Support

### Getting Help
- **Documentation**: Check this README and in-app help
- **Issues**: Report bugs on the [GitHub Issues](https://github.com/Xenonesis/Brave-Brain/issues) page
- **Discussions**: Join conversations in [GitHub Discussions](https://github.com/Xenonesis/Brave-Brain/discussions)

### Feedback
We value your feedback! Please share:
- Feature requests
- Bug reports
- User experience improvements
- Success stories

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### Third-Party Libraries
- AndroidX Core KTX
- Material Design Components
- SwipeRefreshLayout
- ViewPager2

---

## 🙏 Acknowledgments

- **Material Design Team** for excellent design guidelines
- **Android Development Community** for valuable resources and support
- **Open Source Contributors** who make projects like this possible
- **Users** who provide feedback and help improve the app

---

## 🔄 Version History

### v0.21 (Latest)
- 🔧 **Fixed Continue button visibility**: Enhanced onboarding button styling and visibility
- 🎨 **Improved button design**: Added proper spacing, elevation, and minimum width for better UX
- 📱 **Better accessibility**: Added string resources for all button texts
- 🛠️ **Layout improvements**: Enhanced button container layout for consistent appearance

### v0.20
- ✨ Rebranded to "Brave Brain" with updated messaging
- 🎯 Enhanced focus on courage and brain training
- 📱 Updated app identity and descriptions
- 🔄 Version increment for continued development

### v0.10
- ✨ Initial release as "Brain Detox"
- 🚫 App blocking functionality
- ⏱️ Time tracking and limits
- 🧮 Math challenge system
- 🎨 Material Design UI
- 🔐 Comprehensive permission management

### Planned Features
- 📊 Advanced analytics dashboard
- 🎯 Goal setting and achievement system
- 🌐 Backup and sync capabilities
- 🔧 Advanced customization options
- 🧘 Mindfulness and break reminders

---

<div align="center">
  <p><strong>Built with ❤️ for digital wellness</strong></p>
  <p>© 2024 Brave Brain. All rights reserved.</p>
</div>

---

*Be brave, train your brain. Start your journey today!* 🧠✨