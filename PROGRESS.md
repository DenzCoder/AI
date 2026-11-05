# PROGRESS

| Phase | Name | Status |
|---|---|---|
| 1 | Foundation | Code written; Android build **NOT verified** |
| 2 | UI and Dashboard | Code written; Android build **NOT verified** |
| 3 | Local Database | Code written; domain/mapper logic verified (JVM); Room/DataStore build **NOT verified** |
| 4 | Schedule and Activities | Code written; domain logic verified (JVM); UI build **NOT verified**; supply checklist and OCR not done |
| 5 | Alarm and Notifications | Code written; scheduling/policy logic verified (JVM); Android layer and UI build **NOT verified**; no on-device alarm test yet |
| 6 | Hanako AI | Not started |
| 7 | Voice System | Not started |
| 8 | Life Management | Not started |
| 9 | Intelligence and Cloud | Not started |
| 10 | Final QA and APK | Not started |

Verified so far: 179 JVM unit tests pass (`tools/jvm-check.sh`, Kotlin 2.0.21, domain + mappers only) and `tools/cek-statis.py` (resources, imports, manifest, Hanako images) is clean.
Not verified: Gradle/AGP build, KSP/Room, Compose, instrumented tests, on-device behavior. No APK exists.

## Phase 1 checklist
- [x] Project initialization, Gradle configuration, package structure, dependencies, basic navigation
- [ ] Build verification — blocked in authoring sandbox; run via GitHub Actions
- [ ] APK generated

## Phase 2 checklist
- [x] Design system and themes (light/dark/system, 4 accents, 3 text sizes, animation toggle)
- [x] Dashboard with 7 widget cards, navigation, Profil screen, Hanako illustration
- [x] Widget customization: reorder, hide/show, undo, reset to default
- [x] Widget arrangement saved permanently (done in Phase 3)
- [ ] More Hanako expressions — Phase 6

## Phase 3 checklist
- [x] Room entities and DAOs (version 1, schema export on)
- [x] Repository layer; hand-written dependency container
- [x] DataStore: settings, profile, widget layout + history
- [x] Profile persistence and onboarding
- [x] Memory storage with a manage screen (view, add, edit, delete, delete all, switch off, sensitive flag)
- [x] Migration policy documented (no destructive fallback); no migration needed for version 1
- [ ] Build verification (KSP/Room) — pending CI
- [ ] Instrumented DB tests executed — written (5), not run
- [ ] Commit `app/schemas/` after the first green build

## Phase 4 checklist
- [x] Calendar: day, week and month views
- [x] Activities: one-time and weekly, priority, conflict detection, free-slot suggestion
- [x] School: hours per weekday, timetable with breaks, duty roster
- [x] Tasks, homework and exams; goals with milestones; completion history
- [x] Beranda cards show real data for schedule, tasks, school, goals
- [ ] School supply checklist — not done
- [ ] OCR / Hanako chat / voice entry — later phases
- [ ] Bulk "rapikan jadwal" — not done (only per-conflict suggestions)
- [ ] On-device check — not yet run

## Phase 5 checklist
- [x] Reminder logic (pure, tested): activities one-time/weekly, task due day + day before, school entry with duty roster
- [x] One exact alarm for the next reminder (AlarmManager), inexact fallback when the permission is denied, WorkManager safety net (3 h)
- [x] Notification channels (sound / silent); actions Selesai (tasks), Tunda, Buka; opens the right tab
- [x] Re-arm after restart, app update, clock/time-zone change, exact-alarm permission change
- [x] Quiet hours, Fokus mode, Istirahat mode (timed, expire by themselves)
- [x] Pengingat screen: permission status, settings, upcoming reminders (real), test notification
- [x] Notification permission requested only on Android 13+ and only from the Pengingat screen
- [ ] Build verification (Gradle/AGP/Compose/WorkManager) — pending CI
- [ ] On-device test: alarm on time, reboot recovery, silent vs sound, Tunda/Selesai/Buka — not run
- [ ] Holiday calendar for school reminders — not done
- [ ] Android 13+: no prompt outside the Pengingat screen (target phone is Android 11)

## Hanako character
- [x] Character sheet saved in `docs/referensi/`; 12 face expressions cut into `res/drawable-nodpi` by `tools/potong-hanako.py`
- [x] Beranda card and onboarding show Hanako; expression follows time of day / reminder state
- [ ] Gestures, motion poses, chibi: not cut yet (coordinates in the script) — Phase 6/7
- [ ] Higher-resolution source sheet: the current images are about 123 px wide (blurry above ~100 dp)
