# TidyPilot Project Context

## App

- Name: TidyPilot
- Studio: Smithware Studios
- Package: `com.smithware.tidypilot`
- Repo: `https://github.com/BadBagger/tidypilot`
- Current release target: `v0.1.0-mvp`
- Tagline: A tidy home plan that works around your real life.

## Purpose

TidyPilot helps people keep a home reset plan without guilt when work schedules,
low energy, ADHD-style task overwhelm, busy weeks, or skipped routines make
static chore lists fail.

## MVP State

- Dashboard shows today's date, work status, energy check-in, adaptive suggested
  tasks, quick replanning actions, room scan entry, progress cards, low-energy
  fallback, and reset plan action.
- Add/Edit supports cleaning tasks, rooms, and work shifts.
- Detail supports task actions and photo scan detail.
- Room Photo Scan saves local photos through a FileProvider and runs a local v1
  analyzer abstraction.
- Photo Analysis Results shows tidy score, mess score, detected issue tags,
  suggested actions, estimated cleanup time, energy recommendation, feedback,
  and add-to-plan actions.
- Work Schedule and Room Management are first-class flows.
- Export/Reports provides weekly summary, room progress, skipped tasks, energy
  versus cleaning progress, workday versus day-off progress, scan trends, and
  plain-text export.
- Settings includes cleaning intensity, recovery minutes, reminders, day-off
  copy, exhausted-day minimum task size, camera/photo privacy, local photo save,
  theme mode, demo data reset, privacy note, and Smithware Studios about copy.

## Privacy

All app data is local through Room and DataStore. There is no login, no cloud,
no tracking, no paid API, and no network upload in v1.

## Build Verification

- `:app:assembleDebug` passed on 2026-07-07 with the known local Android
  toolchain.
- Release verification is expected before publishing `v0.1.0-mvp`.

## DevHub

After the APK-backed GitHub Release is published, update SoftSmith DevHub:

- `apps.yml`
- `PROJECT_CONTEXT.md`
- `android-app/app/src/main/AndroidManifest.xml`
- `android-app/app/src/main/java/com/softsmith/devhub/MainActivity.java`

