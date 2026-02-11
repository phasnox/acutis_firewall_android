# Acutis Firewall

<p align="center">
  <img src="assets/acutis_logo.png" alt="Acutis Firewall Logo" width="200"/>
</p>

<p align="center">
  <a href="https://github.com/phasnox/acutis_firewall_android/actions/workflows/android-ci.yml">
    <img src="https://github.com/phasnox/acutis_firewall_android/actions/workflows/android-ci.yml/badge.svg" alt="Build Status"/>
  </a>
  <img src="https://img.shields.io/badge/Android-10%2B-brightgreen" alt="Android 10+"/>
  <img src="https://img.shields.io/badge/Kotlin-1.9-purple" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="License"/>
</p>

A parental control Android app that blocks adult and dangerous content using a local VPN-based DNS filter. Parents can protect settings with a PIN, manage custom blocklists, and set time-based access rules.

## Features

### Content Blocking
- **VPN-based DNS filtering** - Intercepts DNS queries locally to block dangerous domains
- **No external server required** - All filtering happens on-device for privacy
- **Works across all apps** - Not limited to browsers

### Pre-built Blocklists
- **Adult content** - Blocks pornographic and explicit material
- **Malware/Phishing** - Protects against known malicious domains
- **Gambling** - Optional blocking of gambling sites
- **Social Media** - Optional blocking of social media platforms

### Custom Blocklists
- Create custom lists for specific blocking needs
- Add/remove individual domains
- Enable/disable lists independently
- Wildcard support (`*.example.com`)

### Time-Based Rules
- **Daily limits** - Set maximum usage time per day (e.g., 1 hour of YouTube)
- **Scheduled blocking** - Block content during specific hours (e.g., bedtime)
- **Day-of-week selection** - Apply rules only on certain days
- **Target selection** - Apply rules to domains, categories, or custom lists

### PIN Protection
- Secure settings with a 4-digit PIN
- PIN required to disable blocking or modify settings
- Enabling protections does not require PIN (only disabling)

## Technical Details

### Architecture
- **Language**: Kotlin
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 35 (Android 15)
- **UI**: Jetpack Compose with Material 3
- **Database**: Room for blocklists and time rules
- **Preferences**: Encrypted DataStore for PIN storage
- **DI**: Hilt for dependency injection
- **Background**: Foreground Service for VPN

### How It Works

1. The app creates a local VPN tunnel using Android's `VpnService`
2. All DNS queries are routed through the VPN
3. Queries for blocked domains return NXDOMAIN (domain not found)
4. Allowed queries are forwarded to upstream DNS (Cloudflare 1.1.1.1)
5. Time rules are evaluated in real-time for dynamic blocking

## Building

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 35

### Build Steps

```bash
# Clone the repository
git clone https://github.com/phasnox/acutis_firewall_android.git
cd acutis_firewall_android

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint checks
./gradlew lintDebug
```

### Project Structure

```
app/src/main/java/com/acutis/firewall/
├── AcutisFirewallApp.kt          # Application class
├── MainActivity.kt                # Single activity
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt        # Room database
│   │   ├── BlockedSiteDao.kt     # Blocked sites DAO
│   │   ├── TimeRuleDao.kt        # Time rules DAO
│   │   ├── CustomBlocklistDao.kt # Custom lists DAO
│   │   └── entities/             # Database entities
│   ├── preferences/
│   │   └── SettingsDataStore.kt  # Encrypted preferences
│   └── repository/
│       ├── BlocklistRepository.kt
│       ├── CustomBlocklistRepository.kt
│       └── TimeRuleRepository.kt
├── di/
│   └── AppModule.kt              # Hilt modules
├── service/
│   ├── FirewallVpnService.kt     # VPN service
│   └── BootReceiver.kt           # Auto-start on boot
├── blocklist/
│   ├── BlocklistDownloader.kt    # Download blocklists
│   └── DefaultBlocklists.kt      # Built-in lists
├── ui/
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── screens/
│   │   ├── home/                 # Main toggle screen
│   │   ├── blocklist/            # Manage blocked sites
│   │   ├── timerules/            # Time-based rules
│   │   ├── settings/             # App settings
│   │   └── pin/                  # PIN entry/setup
│   ├── components/               # Reusable UI components
│   └── theme/                    # Material 3 theme
└── worker/
    └── TimeRuleWorker.kt         # Background time rule checks
```

## Testing

The project includes comprehensive unit tests:

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "com.acutis.firewall.viewmodel.HomeViewModelTest"
```

### Test Coverage

| Category | Tests | Description |
|----------|-------|-------------|
| Repository | 44 | Database operations, domain normalization |
| ViewModel | 79 | UI state management, PIN protection |
| Domain Logic | 31 | DNS blocking, blocklist parsing |
| **Total** | **154** | |

## CI/CD

GitHub Actions runs on every push and pull request:
- **Unit Tests** - All 154 tests must pass
- **Lint** - Code quality checks
- **Build** - APK compilation

PRs cannot be merged if any check fails.

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## Privacy

- **No data collection** - All filtering happens locally on your device
- **No external servers** - DNS queries go directly to Cloudflare (1.1.1.1)
- **No analytics** - The app does not track usage or behavior
- **Encrypted PIN** - PIN is stored using Android's EncryptedSharedPreferences

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

All PRs must pass CI checks before merging.
