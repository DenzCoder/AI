# PROGRESS

| Phase | Name | Status |
|---|---|---|
| 1 | Foundation | Code written; Android build **NOT verified** |
| 2 | UI and Dashboard | Code written; Android build **NOT verified** |
| 3 | Local Database | Code written; domain/mapper logic verified (JVM); Room/DataStore build **NOT verified** |
| 4 | Schedule and Activities | Code written; domain logic verified (JVM); UI build **NOT verified**; supply checklist and OCR not done |
| 5 | Alarm and Notifications | Not started (blocked on a green CI build) |
| 6 | Hanako AI | Not started |
| 7 | Voice System | Not started |
| 8 | Life Management | Not started |
| 9 | Intelligence and Cloud | Not started |
| 10 | Final QA and APK | Not started |

Verified so far: 109 JVM unit tests pass (`tools/jvm-check.sh`, Kotlin 2.0.21, domain + mappers only) and static resource/import checks are clean.
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
