# Smithware Android MVP Template

Reusable native Android starter for Smithware Studios apps.

Tagline: Ship local-first Android MVPs faster.

## What is included

- Kotlin + Jetpack Compose + Material 3 Android app
- Room database for local project and settings records
- DataStore preferences for dark mode and compact dashboard cards
- Dashboard, Add/Edit, Detail, Settings, and Export screens
- Demo data, empty states, input validation, edit, delete, and archive support
- Codex-ready prompt generation
- Light and dark mode
- No login, no cloud sync, no paid APIs, and no network permission

## Create a new app from this template

1. Copy this repo to a new folder.
2. Replace `com.smithware.mvpstarter` with the new package name.
3. Replace `MVP Starter`, app copy, sample data, and prompt-generation text.
4. Update `AGENTS.md` and `PROJECT_CONTEXT.md`.
5. Build with `.\gradlew.bat :app:assembleRelease`.
6. Publish with DevHub's `scripts\publish-smithware-android-app.ps1`.

## Privacy stance

The template defaults to local-first behavior. Do not add login, cloud sync,
paid APIs, or network upload behavior unless the app spec explicitly approves it.

## Build

On this Windows machine:

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:assembleRelease
```

The release APK is generated at:

```text
app\build\outputs\apk\release\app-release.apk
```
