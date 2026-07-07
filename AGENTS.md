# TidyPilot Agent Instructions

Future Codex chats working in this repository must read this file and
`PROJECT_CONTEXT.md` before editing source, creating releases, or changing app
metadata.

## Product Rules

- Keep TidyPilot local-first.
- Do not add login, cloud sync, paid APIs, tracking, or network upload without
  explicit approval.
- Keep the copy supportive and non-judgmental. Do not use shame-based language.
- Preserve the adaptive planning rules around work schedules, energy, skipped
  tasks, and recent room scans.
- Keep room photo analysis behind a replaceable analyzer abstraction so on-device
  ML or an approved cloud service can be added later.
- Do not silently treat photo analysis as perfect. Keep issue tags, rough
  confidence, suggested actions, and user feedback visible.

## Release Rules

- Build with the local Android toolchain documented in `README.md`.
- Publish Android updates as GitHub Releases with APK assets.
- DevHub detects releases from APK-backed GitHub Releases, not source pushes
  alone.
- Keep `PROJECT_CONTEXT.md`, DevHub `apps.yml`, DevHub package visibility, and
  release tags aligned.
