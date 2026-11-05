# HANDOFF — after Phase 5 (Alarm and Notifications) and the Hanako character

Last updated: 2026-11-05

## Status in one line
Phases 1–5 source code is written. The **pure logic is verified** (179 JVM unit tests pass) and the static checks are clean. The **Android build has never run** (Gradle, KSP/Room, Compose, DataStore, WorkManager, instrumented tests, device): the authoring sandbox cannot reach Gradle/Maven. **Phase 5 behavior (alarms on time, reboot recovery, notification buttons, silent vs sound) has never been seen on a device.** Do not start Phase 6 until the GitHub Actions build is green and the checklist in "Exact next steps" has been tried on the phone.

## How the code came to be (read this)
Part of Phases 3/4 (domain, data layer, repositories, ViewModels, most unit tests, `tools/jvm-check.sh`) was written in an earlier working session whose conversation was not available to the session that finished them. Five files from that earlier work were accidentally overwritten and then reconstructed (`domain/memori/Memori.kt`, `domain/dasbor/TataLetakCodec.kt`, the three Gradle files); `Pengaturan.kt` was restored from git. Details were in the Phase 3/4 handoff; after the repair all tests passed, so the reconstruction is consistent. Phase 5 and the Hanako work were written in one session and did not touch those files except to add to the Gradle files and to `PengaturanRepository.kt`.

## Completed work

### Phase 5 — Alarms and notifications
- **Pure logic** (`domain/pengingat/`, tested): `PenghitungPengingat` lists the reminders that fire in a time window; `ProsesPengingat` decides what is due now and when the next alarm is; `KebijakanNotifikasi` decides sound or silent; `PenyusunNotifikasi` builds the Indonesian text from CURRENT data (a deleted, completed or moved item yields nothing, so an old alarm or snooze can never announce something stale); `TundaCodec` stores snoozes; `PengaturanPengingat` holds the settings and clamps bad stored values.
- **What is reminded**: activities (one-time and weekly) N minutes before the start; open tasks/exams with a due date at a time of day the day before and on the due day; school entry N minutes before on weekdays that have school hours, including that day's duty roster.
- **How it is scheduled** (`notifikasi/`): ONE exact `AlarmManager` alarm for the next reminder only. When it fires, or the app starts, or the device restarts, or the periodic worker runs, `PenjadwalPengingat.proses` delivers what became due, moves a "handled up to" marker (stored in DataStore) forward and arms the next alarm. A mutex serializes everything, so the same reminder cannot be delivered twice. If the exact-alarm permission is missing the app uses `setAndAllowWhileIdle` (may be late by minutes, never dropped) and says so in the UI.
- **Re-arming**: after restart, app update, clock or time-zone change, and exact-alarm permission change (`PenerimaSistem`). After a restart, update or permission change reminders that came due in the last 2 hours are delivered late; older ones are dropped. After a clock or time-zone change nothing is delivered, only re-armed. A WorkManager periodic job (3 h, plain `Worker`) is a safety net for alarms that a vendor's battery manager killed.
- **Editing data never pops stale reminders**: edits (an observer in `WadahAplikasi`, debounced 400 ms) re-arm the alarm without delivering, so an activity created 5 minutes before it starts does not fire an immediate "10 minutes before" notification.
- **Notifications**: channels "Pengingat" (HIGH, sound and vibration) and "Pengingat senyap" (LOW). Buttons: Selesai (tasks only; marks the task done), Tunda (default 10 min, snooze survives restarts), Buka (opens the Jadwal or Aktivitas tab; `MainActivity` is `singleTop`). Each notification has its own PendingIntents (unique data URI) so two notifications never overwrite each other's buttons.
- **Modes** (`PengaturanPengingat`): quiet hours (default off, 22:00–06:00, crosses midnight): reminders still appear, silently. Fokus (25–90 min): only Tinggi-priority reminders make sound. Istirahat (15–120 min): everything silent. Modes expire by themselves; the policy checks the clock at delivery time, nothing needs to be scheduled to end a mode.
- **Permissions**: manifest declares `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`. The notification permission is requested **only on Android 13+, only when notifications are off, only from Profil -> Atur pengingat**; on Android 11 nothing is requested. Exact alarms need no permission on Android 11. `USE_EXACT_ALARM` is deliberately not declared. The screen links to the right system pages (notification settings, exact-alarm page on Android 12+, battery optimization).
- **UI**: Profil -> "Atur pengingat" -> screen with Hanako's mood card, permission status, all settings, mode buttons, the **next real reminders** (computed by the same logic the alarm uses: the quickest way to see that scheduling works) and a **test notification** button that goes through the real channels and tells why it was silent.

### Hanako character
- The user's character sheet is kept at `docs/referensi/hanako-lembar-karakter.png` (1536x1024). `tools/potong-hanako.py` cuts the 12 face expressions (normal, senyum, tertawa, malu, sedih, marah, bingung, terkejut, cemberut, mengantuk, berpikir, panik) into `app/src/main/res/drawable-nodpi/hanako_*.webp` (112 KB total). Coordinates are relative to a 1536x1024 sheet and scaled to the real file size, so a higher-resolution version of the same sheet can be dropped in and the script re-run for sharper images.
- `domain/hanako/EkspresiHanako.kt` (enum + `PilihEkspresi`, tested) and `ui/hanako/HanakoAvatar.kt`. The Beranda card shows Hanako with an expression that follows the time of day (sleepy 22:00–03:59); onboarding changes expression per step; the Pengingat screen shows her mood (confused when notifications are blocked, thinking in Fokus, sleepy in Istirahat).
- The earlier temporary vector illustration was deleted.
- **Limit**: the faces are only about 123 px wide, so they get blurry above roughly 100 dp. Not cut yet (coordinates are in the script comment): chibi, 7 gestures, 6 motion poses, front/side/back poses. Use them in Phase 6 (chat moods) and Phase 7 (voice).

### Earlier phases (unchanged)
Phases 1–4: Room v1 (all tables), DataStore settings/profile/layout, onboarding, Memori Hanako, Jadwal (day/week/month, conflicts, school hours/timetable/duty), Aktivitas (tasks, goals, milestones, history), dashboard cards with real data. See CHANGELOG.md.

## Current project structure (new or changed in Phase 5)
```
app/src/main/kotlin/id/asisten/app/
  AsistenAplikasi.kt *              container + mulaiPengingat() (channels, worker, startup catch-up, edit observer)
  MainActivity.kt *                 opens the tab requested by a notification (singleTop + onNewIntent)
  domain/pengingat/                 Pengingat, PengaturanPengingat, Kebijakan, PenghitungPengingat(+ProsesPengingat),
                                    PenyusunNotifikasi, LabelPengingat   (pure, tested)
  domain/hanako/EkspresiHanako.kt   expressions + choice rules (pure, tested)
  notifikasi/                       PenampilNotifikasi, PenjadwalPengingat, Penerima (3 receivers), PekerjaPengaman,
                                    CekIzin, KunciIntent
  data/pengaturan/PengaturanRepository.kt *   + reminder keys, `pengingat`, `keadaanPengingat`
  ui/pengingat/                     PengingatScreen, PengingatViewModel
  ui/hanako/HanakoAvatar.kt
  ui/{beranda,onboarding,profil,navigation}/ *   Hanako avatar, entry to Pengingat, tab from notification
app/src/main/res/drawable/ic_notifikasi.xml      bell icon;  drawable-nodpi/hanako_*.webp
tools/jvm-check.sh  tools/cek-statis.py  tools/potong-hanako.py
docs/referensi/hanako-lembar-karakter.png
```

## Important technical decisions
1–7 as in earlier handoffs (Room 2.8.5 not Room 3; KSP 2.3.10 with the `android.disallowKotlinSourceSets=false` workaround if CI complains; DataStore for small data; no explicit `sourceSets`; opt-in memory; weekly-only recurrence; mixed string style).
8. **One alarm, not one per reminder.** Simpler to keep correct (nothing to cancel when data changes) and cheap on battery; the cost is that everything depends on re-arming after each event, which is what `proses` does.
9. **Watermark + grace window instead of "fire exactly once" flags.** A marker in DataStore says what was handled; reminders older than 2 hours are dropped rather than delivered late. A marker of 0 (fresh install / cleared data) delivers nothing.
10. **Quiet hours and modes silence, never drop.** A reminder is always shown; only sound and vibration change (two channels).
11. **Reminder text is rebuilt from current data at delivery time**, and identified by (kind, id, occurrence instant), so changed or deleted items cannot produce stale notifications.
12. **Plain `Worker` + `runBlocking`** for WorkManager: no ktx/coroutine-worker artifact to get wrong.
13. **No new Room table for snoozes**: they live in DataStore, so the Room schema is still version 1.
14. **No holiday calendar**: school reminders also fire on holidays; this is stated on the screen.

## Tests performed and their REAL results
- `tools/jvm-check.sh`: compiles 26 domain files plus the entity/mapper files against Room annotation stubs, and 17 test files, with Kotlin 2.0.21, then runs JUnit. **Result: OK (179 tests)**: 109 from Phases 1–4, 59 for reminder logic (window math, midnight crossing, weekly recurrence, task/school reminders, catch-up window, clock moved back, dedupe, snooze, quiet hours, Fokus/Istirahat rules, text for every case, codecs), 5 for Hanako expressions, 6 for screen labels.
- **Sanity check of the tests themselves**: four logic bugs were introduced on purpose in a scratch copy (quiet-hours wrap, "never ran" guard, window boundary, completed-task filter); the suite failed 6 tests, i.e. it detects them.
- `tools/cek-statis.py` (new, also run first in CI): XML well-formed, every `R.string`/`R.drawable`/`@string`/`@drawable` resolves, brackets balance and `package` matches the folder in all 80 Kotlin files, internal imports resolve, manifest classes exist, every Hanako expression has an image and a mapping. **Result: clean.** It was verified against 5 deliberately introduced problems (all reported). It cannot find type errors or API misuse.
- **Not run**: `./gradlew` (the sandbox gets 403 from services.gradle.org / Maven hosts), KSP/Room processing, Compose compilation, WorkManager, `AsistenDatabaseTest`, lint, **any on-device test**. **No APK exists.**

## Known issues, risks and gaps
Unverified compile risks (most likely first):
- KSP + AGP 9 built-in Kotlin (decision 2).
- Compose/Android API details written without a compiler: `LifecycleEventEffect`, `rememberLauncherForActivityResult(RequestPermission)`, `mutableLongStateOf`, `TabRow`/`Tab`, `LinearProgressIndicator(progress = { })`, `SystemBarStyle.auto`, NavHost transitions.
- Phase 5 platform APIs: `Notification.Builder` + `Notification.Action.Builder(Icon, …)`, `PendingIntent` flags, trailing commas in `when` conditions in `PenerimaSistem`, `PeriodicWorkRequest.Builder(Class, Long, TimeUnit)`, `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` constant, `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- WorkManager 2.12.0: its minimum compileSdk/AGP was not re-checked.
- `wadahViewModel` (inline composable with `crossinline`) and smart-casts in `MainActivity`'s `when`.

Behavior risks to watch on the realme Narzo 50i Prime (ColorOS kills background apps aggressively):
- If alarms are late or missing, the user must allow background activity and disable battery optimization (the screen explains this and links to the page). The WorkManager net only helps when the system lets it run.
- Force-stopping the app cancels its alarms until the app is opened again; on the next start the last 2 hours of missed reminders are delivered.

Gaps against the master prompt (not hidden):
- Holiday calendar for school reminders; per-day exceptions for weekly activities.
- Android 13+ has no prompt outside the Pengingat screen (the target phone is Android 11).
- School supply checklist, OCR import, bulk reschedule, memory prediction status (see earlier handoff; the last needs a Room migration in Phase 9).
- Hanako: only faces are used; gestures, motion poses, chibi and higher-resolution images are still to do.
- Prayer, habits, money, study, wellness, journal: Phase 8. Gemini/Supabase: Phases 6 and 9.

## Exact next steps
1. Create a (private) GitHub repo; in Termux: `git remote add origin <url>` then `git push -u origin main`.
2. Actions -> run **Build ASISTEN**. If it fails, paste the full failing log into the next session; fix compile errors first, add no features.
3. When green: download **room-schemas** and commit it into `app/schemas/`; download `asisten-debug-apk` and install it on the phone.
4. Phone checklist, Phases 3–4 (see the previous handoff list: onboarding, persistence, Memori, Jadwal, Sekolah, Aktivitas).
5. Phone checklist, **Phase 5**:
   a. Profil -> Atur pengingat: the Hanako card appears with a face; permission status is correct.
   b. Tap **Kirim notifikasi uji**: it appears with sound; start Istirahat 15 min and test again: it must appear silently and the message must say why.
   c. Add an activity 12 minutes from now (reminder 10 min before): "Pengingat berikutnya" lists it; wait: it must ring at the right minute. Test Tunda (comes back after the snooze time) and Buka (opens Jadwal).
   d. Add a task due tomorrow: both reminders are listed; mark it Selesai from the notification: it moves to Riwayat and disappears from the list.
   e. Restart the phone with a reminder pending: it must still ring (reboot recovery).
   f. Turn on quiet hours around the current time: the reminder shows without sound.
   g. Note anything late or missing and the battery settings used.
6. Then Phase 6 (Hanako AI chat with Gemini): use the other expressions per message mood; cut the gestures/chibi if wanted. Do not request the Android 13 notification permission on Android 11.

## Continuation prompt (paste into the next session)
```
Lanjutkan proyek ASISTEN dari HANDOFF.md terbaru (jangan mulai ulang, jangan minta saya mengulang persyaratan).
Status: kode Fase 1–5 sudah ditulis; logika murni terverifikasi (179 tes JVM lolos lewat tools/jvm-check.sh, pemeriksa statis tools/cek-statis.py bersih), tetapi build Android dan perilaku alarm/notifikasi di HP belum pernah dijalankan.
Hasil GitHub Actions saya: <tempel log error, atau tulis "hijau, APK terpasang, semua pemeriksaan di langkah 4 dan 5 berhasil" / sebutkan yang gagal, termasuk pengingat yang terlambat atau tidak muncul>.
Jika ada error: perbaiki penyebabnya secara minimal dan jelaskan, tanpa menambah fitur baru. Jika error KSP "kotlin.sourceSets DSL", tambahkan android.disallowKotlinSourceSets=false.
Jika hijau: kerjakan Fase 6 (Hanako AI) sesuai master prompt, pakai ekspresi dari lembar karakter (docs/referensi), lalu perbarui PROGRESS.md, HANDOFF.md, CHANGELOG.md dengan hasil tes yang nyata.
```
