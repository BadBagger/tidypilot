# TidyPilot Project Context

## App

- Name: TidyPilot
- Studio: Smithware Studios
- Package: `com.smithware.tidypilot`
- Repo: `https://github.com/BadBagger/tidypilot`
- Current release target: `v0.1.13-schedule-import-hotfix`
- Tagline: A tidy home plan that works around your real life.

## Purpose

TidyPilot helps people keep a home reset plan without guilt when work schedules,
low energy, ADHD-style task overwhelm, busy weeks, or skipped routines make
static chore lists fail.

## MVP State

- Main navigation is organized around Today, Rooms, Tasks, Plan, and Settings.
- Today is a focused dashboard with a home status score, top 3 tasks today, one
  quick win, rooms needing attention, completed-today progress, and a first-run
  setup empty state.
- Tasks have a local dirtiness/need score from last completion, recommended
  frequency, room priority, task priority, skip history, effort, and high-impact
  categories like hygiene, clutter, smell, laundry, dishes, trash, and visual
  mess. The score feeds Today recommendations and task status labels.
- One Thing mode is available from Today for overwhelmed users. It asks for
  2 minutes, 5 minutes, 15 minutes, or Full reset, then shows exactly one task
  with why it helps plus start, complete, skip, and pick-another actions.
- Tasks contains the full chore list, overdue chores, recommended next tasks,
  and quick mini-plan actions.
- Tasks links to a free local chore library organized by room and frequency.
  Library chores include estimated time, effort, hygiene importance, clutter
  impact, seasonal flag, supplies, and can be added as normal editable tasks.
- Plan contains guided cleaning missions, quick clean, routine autopilot, energy
  check-in, work schedule, schedule import, reports, and recent completions.
  Guided plans include daily reset, weekly reset, monthly deep clean, spring,
  fall, move-in/move-out, guest coming over, new baby/pet/roommate, and holiday
  hosting. They generate local task checklists, support 30-minute mode, room
  skipping, and spreading tasks over 3 days.
- Add/Edit supports cleaning tasks, rooms, and work shifts as a supporting flow
  rather than a primary tab.
- Household sharing is prepared but not active. Tasks and completions have
  nullable local fields for assignment/household attribution (`assignedTo`,
  `householdId`, `createdBy`, `completedBy`) with no account requirement, no
  cloud sync, and no leaderboard or rewards enabled by default.
- Home setup is a guided room/zone setup flow with home type, room selection,
  starter task checkboxes, cleaning style, and preview-before-save. It writes
  local Room and CleaningTask records and can be reopened from Rooms.
- Detail supports task actions and photo scan detail.
- Onboarding collects a lightweight household profile with rooms, bedroom and
  bathroom counts, household type, goals, delegation interest, reminder
  preference, and a profile-aware starter routine.
- Room Photo Scan saves local photos through a FileProvider and runs a local v1
  analyzer abstraction with mess-level detection, confidence labels, detected
  zones, photo quality guidance, review controls, not-accurate feedback, and
  task creation from scan issues.
- Photo Analysis Results shows mess level, tidy score, mess score, detected
  issue cards, suggested actions, estimated cleanup time, energy recommendation,
  feedback, and add-to-plan actions.
- Work Schedule and Room Management are first-class flows.
- Work Schedule includes local schedule-photo OCR import with editable text and
  review-before-save shift previews.
- Android widgets include Today's Cleaning, One Thing, and Room Status widgets
  backed by local Room data and refreshed after key plan/task updates. Today's
  Cleaning shows the top 3 tasks with complete buttons, One Thing shows one
  recommended quick task with Done and Another actions, and Room Status shows
  rooms needing attention. Widgets use readable RemoteViews layouts, local-only
  data, empty setup states, and at-least-daily launcher updates without
  battery-heavy background work.
- Export/Reports provides weekly summary, room progress, skipped tasks, energy
  versus cleaning progress, workday versus day-off progress, scan trends, and
  plain-text export.
- Settings includes cleaning intensity, recovery minutes, local reminders,
  quiet hours/days, reminder tone, max reminders per day, reminder type controls,
  day-off copy, exhausted-day minimum task size, camera/photo privacy, local
  photo save, theme mode, starter data reset, privacy note, and Smithware
  Studios about copy.
- Reminders are local-only and permission-aware. TidyPilot supports daily,
  task-specific, room reset, weekly reset, seasonal task, and one-quick-win
  reminder planning with gentle/direct/minimal copy, duplicate-safe IDs, a local
  scheduled reminder receiver, a test notification button, and cancellation when
  tasks are completed or deleted.
- Supplies and cleaning budget tracking are optional and local-first. Users can
  keep a supplies list, link supplies to task details, mark supplies as running
  low or on the shopping list, log cleaning supply purchases, set an optional
  monthly cleaning budget, and see monthly supply spend plus cleaning time in
  Reports.
- Premium model is designed but billing is not connected. Free includes rooms,
  custom tasks, basic recurring schedules, basic reminders, Today, One Thing,
  starter chore library, and basic dirtiness scoring. Premium positioning is
  for advanced plans, future AI setup, household sharing, advanced chore
  rotation, widgets, deeper supplies/budget tools, seasonal/deep-clean plans,
  backup/export, themes, advanced stats, and recommendation tuning. Settings
  includes a Premium screen with monthly/yearly/lifetime mock entitlement,
  feature comparison, restore placeholder, and copy that existing data remains
  visible if Premium expires.
- `TidyPilotSummaryProvider` exposes a read-only, summaries-only chores snapshot
  for Smithware Central at `content://com.smithware.tidypilot.summary/summary`.
  It returns counts, due-soon task names, a status line, an optional supportive
  alert, and no raw notes/photos/history.

## Privacy

All app data is local through Room and DataStore. There is no login, no cloud,
no tracking, no paid API, and no network upload in v1.

## Build Verification

- `:app:testDebugUnitTest` passed on 2026-07-08 with scanner, room score,
  planning, starter routine profile, reminder planner, premium model, supply
  suggestion, online-reference scanner fixture tests, and schedule import
  week-list date pairing coverage.
- `:app:assembleDebug` passed on 2026-07-08 with the known local Android
  toolchain.
- `:app:assembleRelease` passed on 2026-07-08 with the known local Android
  toolchain.
- Release `v0.1.13-schedule-import-hotfix` is the current release target. It
  fixes Today task-card text wrapping and improves schedule-photo import so
  week-list screenshots map shifts and days off to the correct dates instead of
  collapsing entries onto one day.

## DevHub

After the APK-backed GitHub Release is published, update SoftSmith DevHub:

- `apps.yml`
- `PROJECT_CONTEXT.md`
- `android-app/app/src/main/AndroidManifest.xml`
- `android-app/app/src/main/java/com/softsmith/devhub/MainActivity.java`
