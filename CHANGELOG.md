# CHANGELOG

## [Unreleased] — Phase 7: Voice (2026-11-08)
### Added
- Microphone button in the chat: the system speech dialog (Indonesian) fills the input; the user still presses Send. No microphone permission is requested by this app.
- Read aloud: a speaker icon on each Hanako reply, a stop button while she speaks, and an optional "read replies automatically" switch (off by default). Indonesian text-to-speech with speed and pitch settings.
- Speech text cleaner: times ("07:00–08:30" -> "7 sampai 8 lewat 30"), bullet lists, links, emoji, markdown, dashes and percent signs are turned into something an engine reads well.
- Voice section in Hanako's settings: auto-read, speed, pitch, a test button, an honest engine status and a button to install missing Indonesian voice data.
- Manifest `<queries>` for the text-to-speech service and the speech recognizer (Android 11+).
- Three vector icons (microphone, speaker, stop).
- 22 new JVM tests (286 total); they found and led to the fix of two bugs in the first version of the cleaner.

### Not done / not verified
- Gradle/AGP/Compose build has never run; `TextToSpeech`, the speech dialog and the new screens have never run on a device and had no independent review.
- No wake word or always-on listening, no voice commands that change data (Hanako cannot change app data yet), no spoken reminders.
- The master prompt's own Phase 6 and 7 text was not available to the session that wrote them: scope was derived from the phase names and must be compared.

## [Unreleased] — Phase 6: Hanako AI chat (2026-11-07)
### Added
- "AI Hanako" tab: a real chat with Hanako (history of the last 40 messages, suggestions, typing indicator, per-reply expression from the 12 character faces).
- Local mode (no internet, nothing sent): answers about today's schedule, open tasks and goals from the phone's own data; says plainly when it does not understand.
- Gemini mode: REST client without extra libraries, Indonesian persona prompt, conversation history, user data included only when allowed (sensitive memories never; none when memory is off), mood marker parsed into Hanako's face.
- Settings screen: Gemini key (encrypted with the Android Keystore, consent checkbox), online switch, share-data switch, editable model name (default `gemini-flash-latest`), connection test, delete chat, delete key.
- Specific Indonesian error messages (invalid key, access denied, quota, model not found, blocked, empty answer, server, no internet, timeout) with Coba lagi / Buka pengaturan.
- `INTERNET` permission.
- 85 new JVM tests (264 total), including 13 against a local loopback HTTP server.

### Changed
- Beranda Hanako card text now points to the chat tab; the placeholder section screen was removed.
- `tools/jvm-check.sh` also compiles `data/ai`.

### Not done / not verified
- Gradle/AGP/Compose build has never run; the Android-only Phase 6 files had no independent review (the reviewer was cut off by a usage limit).
- No request has been sent to the real Gemini API; the default model name was not checked against Google's documentation.
- Hanako cannot change app data yet (no function calling), no streaming, no voice (Phase 7), no Supabase proxy and no automatic memory learning (Phase 9).

## [Unreleased] — Phase 5: Alarm and Notifications; Hanako character (2026-11-05)
### Added
- Reminders for activities (one-time and weekly), tasks and exams (day before and due day) and school entry (with the day's duty roster), computed by pure, unit-tested logic.
- One exact `AlarmManager` alarm for the next reminder (inexact fallback when exact alarms are not allowed), a 3-hourly WorkManager safety net, and re-arming after restart, app update, clock/time-zone change and exact-alarm permission change.
- Notification channels "Pengingat" (sound) and "Pengingat senyap" (silent); actions Selesai (tasks only), Tunda and Buka; tapping opens the Jadwal or Aktivitas tab.
- Quiet hours (silent, never dropped), Fokus mode (only high-priority reminders make sound) and Istirahat mode (all silent), both timed.
- Profil -> Pengingat dan mode screen: permission status with buttons to the right system pages, all settings, the next real reminders, and a test notification.
- Hanako character: 12 expressions cut from the user's character sheet; shown on the Beranda card and in onboarding; the Pengingat screen shows her mood.
- `tools/cek-statis.py` (saved static checks, also run in CI), `tools/potong-hanako.py` (reproducible image cutting).
- 70 new JVM tests (179 total).

### Changed
- Manifest: permissions `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`; three non-exported receivers; MainActivity is `singleTop`.
- DataStore: new reminder keys only (Room schema untouched, still version 1).
- The temporary vector illustration of Hanako was removed.

### Not done / not verified
- Gradle/AGP/Compose/WorkManager build has never run; alarms, reboot recovery and notification behavior have never run on a device.
- No holiday calendar: school reminders also fire on holidays.
- Android 13+: the notification permission is only requested from the Pengingat screen.
- Hanako images are small (about 123 px wide).

## [Unreleased] — Phases 3 and 4: Local Database; Schedule and Activities (2026-11-03)
### Added
- Room database (version 1) with memory, activities, school hours, timetable, duty roster, tasks, goals and milestones; DAOs, entity/domain mappers, repositories, hand-written dependency container.
- DataStore storage for appearance settings, profile and the dashboard layout with undo history (lenient text codec).
- First-launch onboarding (name, appearance, privacy and opt-in memory) and a Memori Hanako screen (view, add, edit, delete, delete all, sensitive flag, switch off).
- Jadwal tab: day/week/month calendar, activity form, conflict detection with free-slot suggestion; school hours, timetable with breaks, duty roster.
- Aktivitas tab: tasks/homework/exams, goals with milestones and progress, completion history.
- Beranda cards for schedule, tasks, school and goals now show recorded data.
- `tools/jvm-check.sh` (JVM-only logic check), 109 JVM unit tests, 5 instrumented DB tests (not run).
- CI uploads the Room schema folder as an artifact.

### Changed
- Appearance settings, name, memory switch and widget arrangement now persist across restarts.
- Build: added KSP 2.3.10, Room 2.8.5, DataStore 1.3.0 and test libraries; removed the explicit `sourceSets` block (built-in Kotlin handles `src/main/kotlin`).
- Profil screen: name editing and memory entry; outdated "not saved yet" notes removed.

### Not done / not verified
- Gradle/KSP/Compose build, instrumented tests and on-device behavior have never run (see HANDOFF.md).
- School supply checklist, OCR import, bulk reschedule: not implemented.
- Automatic-learning memory fields (needs a migration in Phase 9).

## [Unreleased] — Phase 2: UI and Dashboard (2026-11-02)
### Added
- Dynamic theme: light/dark/system, 4 accent colors, 3 text sizes, status-bar contrast handling.
- Animation toggle that switches tab transitions between fade and none.
- Beranda dashboard: Hanako card (static vector illustration) and 7 widget cards with honest empty states.
- "Atur widget": reorder, hide/show, undo (history of 20), reset to default.
- Profil screen with appearance settings.
- Unit tests for widget layout/undo logic (9) alongside the greeting tests (4).

### Changed
- `AsistenTheme` now takes `Pengaturan` and a resolved dark flag; `MainActivity` owns settings state.
- Strings reorganized; placeholder Beranda/Profil texts replaced.

### Not done / not verified
- Settings and widget arrangement are not persisted yet (Phase 3).
- Nothing has been compiled or run (see HANDOFF.md).

## [Unreleased] — Phase 1: Foundation (2026-11-02)
### Added
- Gradle project, version catalog, wrapper 9.6.0, module `:app` (AGP 9.5.0, Kotlin 2.4.20, compileSdk 37, minSdk 30, targetSdk 36).
- Compose + Material 3 base theme (system light/dark), edge-to-edge.
- Bottom navigation with Beranda, Jadwal, Aktivitas, AI Hanako, Profil.
- Time-of-day greeting (`SapaanProvider`) with unit tests, shown on Beranda.
- Indonesian string resources, adaptive launcher icon.
- GitHub Actions workflow for unit tests and debug APK.
- Documentation: PROGRESS.md, HANDOFF.md, CHANGELOG.md, docs/DEPENDENCIES.md.

### Not verified
- Build, unit tests, and APK generation have not been run (see HANDOFF.md).
