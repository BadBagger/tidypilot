# Smithware Android MVP Template Agent Instructions

This repo is a starter for new Smithware Studios Android apps. When creating a
real app from it, rename the package, product copy, demo data, repository URL,
and release metadata before publishing.

## Product Rules

- Keep v1 local-first.
- Do not add login, cloud sync, paid APIs, or network upload behavior without explicit approval.
- Preserve the UI privacy promise: "Your app ideas stay on this device."
- Prefer a small working MVP over broad product scope.
- Keep the standard screen set unless the app spec clearly needs a different flow.
- Do not leave template names in a shipped app.

## Release Rules

- Build with the local Android toolchain documented in `README.md`.
- Publish Android updates as GitHub Releases with APK assets.
- DevHub detects releases from APK-backed GitHub Releases, not source pushes alone.
