# TidyPilot

Smithware Studios Android app for realistic home reset planning.

Tagline: A tidy home plan that works around your real life.

## What is included

- Kotlin, Jetpack Compose, Material 3, Room, and DataStore.
- Local-first cleaning tasks by room, priority, time, energy, frequency, and photo-detectable category.
- Work schedule entry and energy check-ins.
- Rule-based adaptive daily cleaning plan.
- Room photo scan flow with local saved photos and a replaceable analyzer abstraction.
- Photo analysis results with issue tags, tidy score, suggested actions, estimated cleanup time, and feedback.
- Room management, task detail actions, work schedule, settings, and plain-text reports.
- Starter rooms, tasks, shifts, and scan results.
- Light, dark, and system theme modes.

## Privacy

All cleaning plans, work schedules, energy check-ins, room photos, scan results,
and reports stay on device. There is no login, no cloud sync, no tracking, no
paid API, and no network upload in v1.

## Build

On this Windows machine:

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:ANDROID_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleRelease
```

The release APK is generated at:

```text
app\build\outputs\apk\release\app-release.apk
```

## Release

Current release: `v0.1.2-signed-icon`

GitHub repo: `https://github.com/BadBagger/tidypilot`

Package: `com.smithware.tidypilot`
