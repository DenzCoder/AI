# CHANGELOG

## [Unreleased] — Neo-brutalism look (2026-11-22)
User request: "tampilan ... masih jelek, upgrade lebih bagus"; then "kayaknya Neo brutalism bagus jadi kamu atur sendiri nanti biar aku yang nilai" (the style was chosen by the user, the details by the session; the user will judge the result on the phone).
### Changed
- New theme (`ui/theme/Theme.kt`, `GayaNeo.kt`): cream background, white cards, near-black ink outlines, yellow secondary, dark pink tertiary, red error; small corner radii (4–12 dp) and heavier type (Black / ExtraBold for headings, titles and button labels). The accent colour the user picks in Profil (jingga / ungu / hijau / biru) is still the primary colour.
- Dark mode keeps the look: cream outlines, and the hard shadow takes the accent colour (a black shadow would vanish on a dark background).
- Text on the accent colour is chosen by WCAG contrast (`domain/pengaturan/KontrasWarna.kt`, pure and tested) instead of a fixed white: near-black on jingga, hijau and biru, white on ungu.
- New components (`ui/komponen/Neo.kt`): `KartuNeo` (card), `TombolNeo` / `TombolTepiNeo` (primary / secondary button that slides onto its hard shadow while pressed, flat grey when disabled), `TombolTeksNeo` (underlined text action), `ChipNeo` (single-choice or action chip), `TabNeo` (equal-width tabs), `BilahProgresNeo` (thick progress bar with progress semantics), `bayanganNeo` (the hard shadow modifier). Every block reserves room for its own shadow, so nothing is clipped by a scrolling parent.
- Every `Card`, `Button`, `OutlinedButton`, `TextButton`, `FilterChip`, `SuggestionChip`, `TabRow` and `LinearProgressIndicator` in the screens was replaced by these; dialogs (`DialogForm`, `DialogKonfirmasi`) are now a neo card in a `Dialog` (the delete confirmation button is red); page titles are yellow stickers (`JudulHalaman`); text fields get white boxes with ink outlines; the bottom navigation has a thick top line and an accent-coloured selected item; chat bubbles, proposal cards and error cards use the neo card; the calendar day cells are squares (chosen day = accent, today = yellow); the accent picker shows square swatches with a check on the chosen one.
- Text that used the accent colour on the cream background (hard to read for orange) now uses ink or the dark pink tertiary colour.
- The window background before the first Compose frame is cream (light) or dark violet (night) instead of white.
### Verified
- `tools/jvm-check.sh` **OK (492 tests)**; `tools/cek-statis.py` clean (140 Kotlin, 13 XML files). 6 new tests for the contrast logic; deliberate-bug checks in scratch copies (inverted dark/white decision, wrong sRGB exponent, swapped colour weights, wrong linearisation threshold, no contrast offset) are all detected; the first round missed the exponent bug, so a test with fixed WCAG reference values was added and the bug is now caught.
- A separate review agent read the new Compose code and every call site as a "human compiler" and found no compile error; its only doubts (about 90–95 % sure): `Typography().copy(...)` and `Shapes(...)` with the five classic names under Material3 1.4.
### Not done / not verified
- **None of the UI was compiled or seen on a screen.** How it actually looks (spacing, shadow clipping, contrast on the real display, chip row widths on a small phone, the dialog on a small screen with the keyboard) is unknown until the user looks at it. Only the user can judge whether it is "bagus".
- Switch, Checkbox, RadioButton and the circular progress indicator keep the Material look (recoloured by the theme only).

## [Unreleased] — Hanako's voice: cuter presets and the engine's voices (2026-11-21)
User request: "suara nya masih Google, ga bisa kah suara model yang dipilih lebih imut lagi".
### Added
- Voice packages in Hanako's settings: **Normal** (speed 1.0, pitch 1.0), **Imut** (1.1 / 1.4) and **Imut banget** (1.15 / 1.7); the pitch chips also got Imut and Imut banget. The package matching the current values is highlighted; hand-tuned values show no package.
- A list of the voices the installed text-to-speech engine offers (Indonesian first), each with a **Dengar** button and a "Bawaan mesin" entry; the choice is stored and used by `PembacaSuara`.
- `DaftarVoice` / `InfoVoice` (pure, tested) sort and label the engine's voices.
### Honest limits
- It is still the phone's TTS engine (usually Google's). Pitch and speed only make it sound higher and quicker; the app does not say which voice is male or female because Android does not expose that. A truly cute or character voice needs a cloud voice service (an extra key, cost, privacy decision): **not implemented**, offered as a later option.
- `PembacaSuara` voice selection and the settings UI are Android-only code: not compiled, never listened to.

## [Unreleased] — Reminder sounds: app tones, system tones, your own music (2026-11-20)
User request: "untuk alarm kasih suaranya atau bisa setel musik dari perangkat juga".
### Added
- "Suara pengingat" section on the Pengingat screen: **Bawaan** (phone's default notification sound, as before), five **built-in tones** (Lonceng, Kicau, Kotak musik, Bel sekolah, Alarm digital; generated by `tools/buat-nada.py`, ~15 % RMS, no clipping, stored as `res/raw/nada_*.ogg`), a **system tone** from the ringtone picker, and **music from the phone** (file picker).
- Each choice has a play/stop preview. A switch "Pakai volume alarm" plays the reminder with the alarm audio usage (follows the alarm volume instead of the notification volume).
- Android fixes a notification channel's sound when the channel is created, so the app keeps **one channel per sound choice** (id from an FNV-1a hash of the choice, covered by test vectors) and deletes stale ones; silent / quiet-hours reminders use a separate silent channel.
- Picked music is copied into the media store (`Notifications/ASISTEN`, marked IS_NOTIFICATION, IS_PENDING while copying; no storage permission needed on API 30+) so the notification system can play it; replacing or removing it deletes the copy. System tones are only referenced, never deleted.
- `SuaraPengingat` model + codec (unreadable stored text falls back to the default sound, never to silence), `SuaraPengingatRepository`, `PengelolaSuara`, `PemutarContoh`, `PenampilNotifikasi.siapkanSaluran`, `BagianSuara` UI. The test notification uses the chosen sound.
### Not verified
- Never compiled, never run on a phone, **never listened to**: whether the tones sound pleasant, whether ColorOS plays a custom notification sound from the media store, and whether alarm volume behaves as expected are all unknown. `tools/cek-statis.py` now also checks `R.raw` references.

## [Unreleased] — Two more AI services: Groq and DeepSeek (2026-11-16)
User request: "tambahin 2 model ai lagi yaitu groq sama deepsek jadi bisa ganti".
### Added
- Hanako can now talk to **Gemini, Groq or DeepSeek**; the settings screen has a provider chooser. Each provider has its **own encrypted key and its own model name** (switching never reuses one provider's key for another); the consent box is ticked each time a key is saved, with a provider-specific note.
- Protocols: Gemini `generateContent` (key in the `x-goog-api-key` header); Groq `https://api.groq.com/openai/v1/chat/completions` and DeepSeek `https://api.deepseek.com/chat/completions` (OpenAI-style chat completions, Bearer key, `tools` as `{type:"function"}`, tool-call arguments parsed from a JSON string). Groq sends `max_completion_tokens`; DeepSeek sends `max_tokens` and `thinking: {type: "disabled"}`. Requests only go to each provider's fixed HTTPS address.
- Hanako actions (the 13 proposal functions) work with all three providers (`DeklarasiAksi.alatOpenAi`).
- New error kind for **402 (insufficient balance)**; provider-aware error and status wording (`LabelHanako`).
- DeepSeek shows a note next to the consent switch: its privacy policy says data is processed and stored on servers in China.
- Default model suggestions: Groq `llama-3.3-70b-versatile`; DeepSeek `deepseek-flash` (also `deepseek-v4-pro`). Names are editable with one-tap suggestions.
### Changed
- `KlienGemini` became `KlienAi`; `ProtokolGemini` is Gemini-only and `ProtokolAi.kt` holds the shared parts (`PermintaanAi`, `UraiResponsAi`) and the OpenAI-style ones (`PermintaanOpenAi`, `UraiResponsOpenAi`); the stored settings gained a provider field (old installs default to Gemini).
### Verified / not verified
- 77 new JVM tests in total for the three features of this batch (409 -> 486), including a loopback HTTP server test for all three providers. **No request has ever been sent to the real Groq, DeepSeek or Gemini service**: the exact request fields, the default model names (taken from documentation at the time of writing; DeepSeek's in particular may be retired or renamed) and whether each model follows the function schema are unknown until tried with real keys.

## [Unreleased] — Hanako actions: she can propose changes to the app (2026-11-16)
### Added
- Hanako can now propose (never silently do) changes: add / change / delete tasks, activities and timetable lessons, mark a task done or not done, change reminder settings (minutes before, snooze, quiet hours, on/off), start or end Fokus / Istirahat mode. Uses Gemini function calling (13 declared functions).
- Proposal cards in the chat ("Usulan Hanako, belum dijalankan") with **Setujui** and **Tolak**. Edits show old -> new values; clashes with the schedule and "cannot be undone" are shown as warnings; deletes use the error colours and a "Setujui dan hapus" button.
- The app writes its own chat reports ("Sudah dilakukan: ...", "Tidak jadi: ...", "Usulan dibatalkan ...", labelled "Perubahan di aplikasi"; not read aloud) and always adds "Belum ada yang diubah" under a reply that carries proposals, whatever the model wrote.
- Proposals are stored (DataStore, max 10, expire after 24 h, identical ones not stacked) and re-checked whenever they are shown or approved; one that no longer makes sense (task deleted, date passed, already done) disappears.
- Settings switch "Izinkan Hanako mengusulkan perubahan" (default on). Off: no function declarations are sent and the prompt says she cannot change data.
- Prompt: id-bearing lists of open tasks, activities and timetable plus a one-line summary of the reminder settings (only when "Bagikan data" is on); rules that she proposes, never claims it is done, asks when date or time is unclear, and explains that reminders ring N minutes before an activity starts.
- Offline answer to change requests ("tambah tugas ...", "hapus ...", "ingatkan ...") that says what is needed instead of listing data.
- `MiniJson.tulis` (JSON writer); `HasilGemini.Berhasil` carries the function calls; `PermintaanGemini.badan(..., alat = true)` adds the declarations and lowers the temperature to 0.4.
- 123 new JVM tests (409 total). A declaration-driven test checks that every declared function and field name is really read by the parser and every required field is really required.

### Changed
- `PesanChat.aksi` flag; the history line's kind flag is now 0 normal / 1 local / 2 app report (old histories still read).
- Prompt size cap raised from 6000 to 12000 characters; `tools/cek-statis.py` accepts imports of object members.

### Not done / not verified
- Never compiled by Gradle, never run on a phone, **never tried against the real Gemini API**: whether the model follows the function schema and the rules is unknown. The Android-only parts (`PelaksanaAksi`, repository additions, `HanakoViewModel`, the cards in `HanakoScreen`) had no independent review.
- No actions for goals (tujuan) or memories, no undo after approval, no spoken confirmation, no bulk reschedule.

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
