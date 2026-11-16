# PROGRESS

| Phase | Name | Status |
|---|---|---|
| 1 | Foundation | Code written; Android build **NOT verified** |
| 2 | UI and Dashboard | Code written; Android build **NOT verified** |
| 3 | Local Database | Code written; domain/mapper logic verified (JVM); Room/DataStore build **NOT verified** |
| 4 | Schedule and Activities | Code written; domain logic verified (JVM); UI build **NOT verified**; supply checklist and OCR not done |
| 5 | Alarm and Notifications | Code written; scheduling/policy logic verified (JVM); Android layer and UI build **NOT verified**; no on-device alarm test yet |
| 6 | Hanako AI (+ actions, see below) | Code written; chat/prompt/parsing/client logic verified (JVM, HTTP client against a loopback server); screens, Keystore and DataStore layer build **NOT verified**; never tried on a phone or against the real Gemini API |
| 7 | Voice System | Code written; speech-text and settings logic verified (JVM); `TextToSpeech`, speech dialog and screens build **NOT verified**; never tried on a phone |
| 8 | Life Management | Not started |
| 9 | Intelligence and Cloud | Not started |
| 10 | Final QA and APK | Not started |

Verified so far: 409 JVM unit tests pass (`tools/jvm-check.sh`, Kotlin 2.0.21: domain, mappers and the Gemini HTTP client) and `tools/cek-statis.py` (resources, imports, manifest, Hanako images) is clean.
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
- [x] More Hanako expressions: all 12 are used in the Phase 6 chat

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

## Phase 6 checklist
- [x] Chat tab replaces the placeholder; history kept (last 40 messages)
- [x] Local mode: answers from real data without internet, honest when it does not understand
- [x] Gemini mode: REST client, persona prompt in Indonesian, history, per-reply mood marker -> Hanako's face
- [x] API key entered by the user, stored encrypted (Keystore AES-GCM), never in the repo; consent checkbox; "share app data" switch; sensitive memory never sent
- [x] Specific Indonesian errors with retry / open-settings actions; connection test; editable model name
- [x] INTERNET permission added
- [ ] Build verification of the screens, ViewModel, repository and Keystore code — pending CI (no independent review either)
- [ ] First real request to Gemini, default model name `gemini-flash-latest` unverified
- [ ] Phone checklist (HANDOFF.md, step 6)
- [ ] Not in this phase: Hanako changing app data (function calling), streaming, voice (Phase 7), Supabase proxy (Phase 9)

## Phase 7 checklist
- [x] Microphone button: system speech dialog (Indonesian), text added to the input; no microphone permission needed
- [x] Read aloud: speaker icon per Hanako reply, stop button, optional auto-read, Indonesian TextToSpeech
- [x] Text cleaned for speech (times, lists, emoji, markdown, links), tested
- [x] Voice settings (auto-read, speed, pitch, test) with an honest engine status and a link to install missing voice data
- [x] Manifest `<queries>` for the TTS service and the recognizer
- [ ] Build verification of `PembacaSuara`, the screens and `SuaraRepository` — pending CI (no independent review)
- [ ] Phone checklist (HANDOFF.md, step 7)
- [ ] Not in this phase: wake word / always listening, voice commands that change data, spoken reminders
- [ ] Master prompt scope for Phases 6–7 not re-checked (its text was not available): compare and list gaps

## Hanako actions checklist (added after Phase 7, 2026-11-16)
User request: "Hanako belum bisa akses aplikasi, seperti atur alarm, ubah jadwal, dan lain-lain."
- [x] Gemini function calling: Hanako PROPOSES add/change/delete of tasks, activities and timetable lessons, mark a task done, change reminder settings, start/end Fokus and Istirahat mode (13 functions)
- [x] Nothing runs without the user: each proposal is a card with **Setujui** / **Tolak**; deletes are red, say "tidak bisa dibatalkan" and the button says "Setujui dan hapus"
- [x] Every call is validated against the CURRENT data (ids must exist, dates/times/numbers range-checked, text cleaned, unknown function names refused) when it arrives, when it is shown and again when approved
- [x] Approved edits are applied to the item as it is NOW, so changes the user made meanwhile are kept
- [x] Alarms follow automatically: the Phase 5 observer re-arms them after any data or reminder-settings change
- [x] Honest chat text: the app appends "Belum ada yang diubah" to any reply that carries proposals, and reports "Sudah dilakukan" / "Tidak jadi" / "Usulan dibatalkan" itself
- [x] Settings switch "Izinkan Hanako mengusulkan perubahan" (default on); off = no function declarations are sent
- [x] Local (offline) mode answers a change request honestly instead of listing data
- [x] 123 new JVM tests (409 total) + 18 deliberate-bug checks, all detected
- [ ] Build verification of the Android-only parts (`PelaksanaAksi`, `HanakoRepository` additions, `HanakoViewModel`, `HanakoScreen` cards) — pending CI, no independent review
- [ ] Phone checklist (HANDOFF.md, step 8) and a real Gemini function-calling test (never done)
- [ ] Not included: goals (tujuan) and memory actions, holiday/supply lists, bulk reschedule, "undo" after approval, spoken confirmation

