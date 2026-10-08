# Accessibility-Hiding — LSPosed Module

![Build APK](https://github.com/binit-ops/accessibility-hiding/actions/workflows/build.yml/badge.svg)
![Release](https://img.shields.io/github/v/release/binit-ops/accessibility-hiding)
![License](https://img.shields.io/badge/license-GPL%20v3-blue)

Hides enabled accessibility services from app detection while keeping them
fully functional. Useful for apps that refuse to run (banking, games, DRM
apps) when any accessibility service is active.

## ⚠️ Disclaimer

- For educational/personal use on devices you own
- Some apps detect accessibility for legitimate security reasons
  (e.g., screen-reader overlays aiding phishing)
- You are responsible for complying with app terms of service
- May break apps that legitimately use accessibility APIs

## Features

- ✅ Hide all accessibility services, or only specific ones
- ✅ Per-app targeting — only hook the apps you choose
- ✅ Hides from `Settings.Secure`, `AccessibilityManager`,
  `ContentResolver`, and `PackageManager` detection
- ✅ Config UI with service list and app list
- ✅ Verbose logging for debugging via LSPosed log viewer

## Requirements

| Component | Version |
|-----------|---------|
| Android   | 8.0+ (API 26+) |
| Root      | Magisk (recommended) |
| Framework | LSPosed |

## Build

### GitHub Actions (recommended)

1. Fork or push this repo to GitHub
2. Go to **Actions** tab → "Build APK" workflow runs automatically on push
3. Download the APK from **Artifacts** (or **Releases** if you pushed a `v*` tag)

### Local

```bash
gradle wrapper --gradle-version 8.5
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/`

## Installation

1. Install the APK
2. Open **LSPosed** → Modules → enable **Hide Accessibility**
3. In LSPosed scope, select the target apps to hook
4. Open the module app and configure:
   - Which services to hide (or enable "Hide All Services")
   - Which target apps to hook
5. Force-stop target apps, then reopen

## Configuration

| Option | Description |
|--------|-------------|
| **Hide All Services** | Reports accessibility as fully disabled |
| **Hide Touch Exploration** | Reports touch exploration as disabled |
| **Services list** | Toggle which individual services to hide |
| **Target Apps** | Which apps get hooked (must also match LSPosed scope) |
| **Verbose Logging** | Detailed output in LSPosed log viewer |

## How It Works

Hooks installed per target app, via LSPosed:

| Hook | Defeats |
|------|---------|
| `Settings.Secure.getString` / `getInt` | Direct settings checks |
| `AccessibilityManager` service lists | Framework API enumeration |
| `AccessibilityManager.isEnabled` | Simple enabled checks |
| `ContentResolver.query` | Raw settings URI queries |
| `PackageManager.queryIntentServices` | Intent-based service discovery |
| `File.exists` / `SystemProperties.get` | Framework artifact detection |

## Troubleshooting

**Target app still detects accessibility**

- Verify module is enabled AND app is in LSPosed scope
- Force-stop the app completely (not just swipe away)
- Check LSPosed logs for `[HideA11y]` entries
- The app may use native detection — see logs for `exec()` calls

**Config changes not applying**

- Force-stop target apps after changing config

**Module not appearing in LSPosed**

- Confirm the APK is installed and not hidden by Magisk DenyList
- Reboot after first install

## License

This project is licensed under the **GNU General Public License v3.0**.

- See the [LICENSE](LICENSE) file for the full license text
- Summary: you may use, modify, and distribute this software, but any
  distributed modifications must also be released under GPL-3.0 with
  source code available
- Full license text: https://www.gnu.org/licenses/gpl-3.0.html
