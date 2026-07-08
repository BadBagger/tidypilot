# Android Developer Verification

This app is configured for outside-Google-Play APK distribution through Smithware DevHub, GitHub Releases, and direct installs.

## Package

- Application ID: `com.smithware.tidypilot`
- Do not change the application ID unless the product is intentionally being renamed or forked.

## Release Signing

Release builds must use a local release keystore. They must not use the Android debug key.

Signing secrets are loaded from a local-only `keystore.properties` file at the repository root. This file is ignored by git and must not be committed, pasted into chat, uploaded, or copied into release notes.

Required local properties:

```properties
storeFile=/absolute/path/to/<app-name>-release.jks
storePassword=CHANGE_ME
keyAlias=<app-id-or-name>
keyPassword=CHANGE_ME
```

Use `keystore.properties.example` as the template. Keep the real keystore outside the repository, for example under:

```text
C:\Users\KyleB\.smithware\signing\com.smithware.tidypilot\
```

## Expected Behavior

- `assembleDebug` may use the normal Android debug key.
- `assembleRelease` must fail if `keystore.properties` is missing or incomplete.
- `assembleRelease` must never fall back to `signingConfigs.getByName("debug")`.
- APK, AAB, keystore, and signing property files must remain ignored by git.

## Build Commands

```powershell
.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

## Release Signature Verification

After building release, verify the APK:

```powershell
apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```

The output must include:

- `Verifies`
- `Verified using v2 scheme (APK Signature Scheme v2): true`
- A signer certificate for the local Smithware Studios release key, not `Android Debug`

Record the release certificate SHA-256 in release handoff notes. Do not record passwords or keystore contents.

Current release certificate SHA-256:

```text
365533108b4afeb8f8488cf374ecb9dab5f97c63c0404bb2672a8887fb0c9f5c
```

## Install Note

If a device has a debug-signed build installed, Android may reject an update signed with the release key. Uninstall the debug build first, then install the release APK.

## Backup Requirement

Back up the local release keystore folder securely. Losing the keystore means future APK updates signed with a different key may not install over existing release builds.
