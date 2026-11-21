# HANDOFF — after Phase 7 (Voice), Hanako actions and the 2026-11 extras (Groq/DeepSeek, reminder sounds, cute voice, neo-brutalism look)

Last updated: 2026-11-22

## Status in one line
Phases 1–7 source code is written, plus **Hanako actions** (she can propose changes to the app; added 2026-11-16 after the user said Hanako "cannot access the app yet"). The **pure logic and the Gemini HTTP client are verified** (409 JVM unit tests pass, the HTTP client against a real loopback server) and the static checks are clean. The **Android build has never run** (Gradle, KSP/Room, Compose, DataStore, WorkManager, Keystore, TextToSpeech, speech dialog, instrumented tests, device): the authoring sandbox cannot reach Gradle/Maven (re-probed 2026-11-08, still HTTP 000). **Phase 5 alarms, the Phase 6 chat and the Phase 7 voice have never been seen on a device, and no request has ever been sent to the real Gemini API.**

**Update 2026-11-22 (extras requested on 2026-11-16, all four done in source):** (1) Hanako can use **Groq and DeepSeek** besides Gemini, switchable, with a separate key and model per service; (2) reminder **sounds**: default, five built-in tones, a system tone, or music from the phone, with preview and an alarm-volume switch; (3) a **cuter voice**: packages Normal / Imut / Imut banget and a list of the TTS engine's voices; (4) a **neo-brutalism look** (the user chose the style and left the details to the session). The JVM suite is now **492 tests, all passing**, and `tools/cek-statis.py` is clean (140 Kotlin, 13 XML files). **Everything Android/Compose in these four items is uncompiled and unseen**: a separate review agent read the new Compose code and found no compile error, but that is a read, not a build. No request was ever sent to Groq or DeepSeek, nobody has listened to the tones or the voice, and nobody has looked at the new screens. The user will judge the look on the phone. If the first CI build fails, fix compile errors first; the newest and riskiest unbuilt files are `ui/komponen/Neo.kt`, `ui/komponen/Komponen.kt`, `ui/theme/Theme.kt`, `ui/theme/GayaNeo.kt`, `ui/pengingat/BagianSuara.kt`, `notifikasi/PengelolaSuara.kt`, `notifikasi/PenampilNotifikasi.kt` and `suara/PembacaSuara.kt`.

Why work continued without a green build: an earlier note said to wait for a green GitHub Actions build. The user answered "Lanjutkan" repeatedly without a build report, so Phases 6 and 7 were written, each in small isolated files (nothing in Phases 1–5 was rewritten, only wired up). Three phases of unbuilt code now stack up: if the first CI run fails, fix compile errors first and add no features.

**Scope caveat**: the text of the original master prompt was not available to the sessions that wrote Phases 6 and 7 (only a summary survived). Their scope was derived from the phase names and the earlier summary. Compare with the master prompt and list anything missing (for example voice commands that change data, wake word, languages, specific screens) before calling those phases complete.

## How the code came to be (read this)
Part of Phases 3/4 (domain, data layer, repositories, ViewModels, most unit tests, `tools/jvm-check.sh`) was written in an earlier working session whose conversation was not available to the session that finished them. Five files from that earlier work were accidentally overwritten and then reconstructed (`domain/memori/Memori.kt`, `domain/dasbor/TataLetakCodec.kt`, the three Gradle files); `Pengaturan.kt` was restored from git. Details were in the Phase 3/4 handoff; after the repair all tests passed, so the reconstruction is consistent. Phase 5 and the Hanako work were written in one session and did not touch those files except to add to the Gradle files and to `PengaturanRepository.kt`.

## Completed work

### Extras of 2026-11-16 → 2026-11-22 (four user requests)
**1. Groq and DeepSeek (commit 197879d).** `PenyediaAi` (GEMINI / GROQ / DEEPSEEK: stable `kode`, `nama`, `modelBawaan`, `saranModel`, fixed HTTPS `urlDasar`, `halamanKunci`, optional `catatanData`). Gemini keeps Google's `generateContent` protocol (key in `x-goog-api-key`); Groq (`https://api.groq.com/openai/v1/chat/completions`) and DeepSeek (`https://api.deepseek.com/chat/completions`) use the OpenAI-style one (`PermintaanOpenAi` / `UraiResponsOpenAi` in `ProtokolAi.kt`: Bearer key, `tools` as `{type:"function"}`, tool-call arguments are a JSON string; Groq `max_completion_tokens`; DeepSeek `max_tokens` + `thinking:{type:"disabled"}`). `KlienAi` replaced `KlienGemini`; `HanakoRepository` stores a key and a model per provider (DataStore keys, key encrypted with the Keystore key); the settings screen has a provider chooser; `LabelHanako` wording follows the provider; 402 becomes `JenisGalat.SALDO_HABIS`; DeepSeek shows a data-in-China note. All 13 Hanako action functions are also declared in the OpenAI-style format (`DeklarasiAksi.alatOpenAi`).
- **Unverified**: default models `llama-3.3-70b-versatile` (Groq) and `deepseek-flash` (DeepSeek) are suggestions from documentation at the time of writing and may be retired or renamed (the field is editable, "Uji koneksi" will say). Whether each model obeys the function schema is unknown. Nothing was ever sent to the real services.

**2. Reminder sounds (commit 7bd6e2a).** `SuaraPengingat` (Bawaan / Aplikasi(kode) / Sistem(uri) / Berkas(uri)) + one-line codec that falls back to the default sound, `SuaraPengingatRepository`, `PengelolaSuara` (copies picked music into MediaStore.Audio `Notifications/ASISTEN`, IS_NOTIFICATION, IS_PENDING while copying; resolves `android.resource://` URIs for bundled tones), `PemutarContoh` (preview), `PenampilNotifikasi.siapkanSaluran(suara)` (one channel per sound choice, id from an FNV-1a hash, stale ones deleted, separate silent channel, USAGE_ALARM variant), UI in `ui/pengingat/BagianSuara.kt`. Tones are `res/raw/nada_*.ogg` made by `tools/buat-nada.py` (numpy + ffmpeg libvorbis; RMS about -15 dBFS, peaks <= 0.85).
- **Unverified**: nobody has listened to the tones; whether ColorOS plays a custom song from the media store as a notification sound is unknown; alarm-volume behaviour on the phone is unknown.

**3. Cute voice (commit c4efbbe).** `PaketSuara` (Normal 1.0/1.0, Imut 1.1/1.4, Imut banget 1.15/1.7 as speed/pitch), extra pitch chips, `DaftarVoice` / `InfoVoice` (pure) and a voice list in Hanako's settings with a Dengar button; `PembacaSuara` applies `setVoice` / `setPitch` / `setSpeechRate`. The voice is still the phone's TTS engine; Android exposes no gender, so the app never claims one. A real character voice would need a cloud voice service (extra key, cost, privacy): not implemented.

**4. Neo-brutalism look (2026-11-22, the commit that carries this note).** Theme: cream background, white cards, ink (`0xFF111111`) outlines, hard offset shadows, yellow secondary, dark pink tertiary, small radii, Black/ExtraBold type; dark mode = cream outlines and accent-coloured shadows; accent colour from Profil kept. `KontrasWarna` (pure, tested) picks near-black or white text on the accent by WCAG contrast. Components in `ui/komponen/Neo.kt`; all screens moved from Material `Card`/`Button`/`OutlinedButton`/`TextButton`/`FilterChip`/`SuggestionChip`/`TabRow`/`LinearProgressIndicator` to them. `AlertDialog` was replaced by a `Dialog` + neo card (`KerangkaDialog` in `Komponen.kt`). Window background colour set in `res/values*/colors.xml` + `themes.xml`.
- **Unverified**: never compiled, never seen. Things to look at first: shadow clipped at screen edges; chip rows and tabs on a 720 px wide phone; dialog with the keyboard open; contrast of text on cream; bottom bar selected item; `TombolNeo` press effect; the Hanako hero card on the accent colour (avatar art on an orange background); dark mode.
- Switch / Checkbox / RadioButton / CircularProgressIndicator still use the Material look (recoloured by the theme).

### Hanako actions — she can propose changes to the app (2026-11-16)
The user reported that build, features and AI were fine (no log was pasted, so this is an unverified report) and asked for Hanako to be able to "access the app: set alarms, change the schedule and so on". Implemented as **proposals the user approves**, never as silent changes.
- **How it works**: with a Gemini key, online mode and the switch "Izinkan Hanako mengusulkan perubahan" (default on), every chat request carries 13 function declarations (`DeklarasiAksi`): `tambah_tugas`, `ubah_tugas`, `tandai_tugas`, `hapus_tugas`, `tambah_kegiatan`, `ubah_kegiatan`, `hapus_kegiatan`, `tambah_pelajaran`, `ubah_pelajaran`, `hapus_pelajaran`, `atur_pengingat`, `mulai_mode`, `akhiri_mode`. When Gemini answers with function calls, `UraiAksi` validates each one against the CURRENT data; valid ones are stored as proposals (`RiwayatUsulan`, DataStore, max 10, 24 h) and shown as cards under the chat with **Setujui** / **Tolak**. Only Setujui executes anything.
- **"Set an alarm"**: the app has no free-standing alarms. Reminders ring N minutes before an activity or a task's due time (Phase 5). So "ingatkan aku jam 7" becomes a proposed activity starting at 07:00, the card/prompt explains that the reminder rings per the reminder settings (default 10 min before), and Hanako can also propose `atur_pengingat` (for example 0 minutes before). After any approved change the Phase 5 observer re-arms the alarms by itself.
- **Safety rules (all unit-tested)**: unknown function names refused; ids must exist now (the prompt lists ids; break slots cannot be addressed); dates ISO and within one year back / three years ahead, a new one-time activity may not be in the past; times 24 h and end after start; numbers range-checked (lead times 0–720, snooze 1–120, mode 1–480 min); text cleaned (control and zero-width characters removed) and length-limited (title 100, others 60, notes 300); no-op changes refused; at most 5 proposals per reply; identical pending proposals are not stacked; an edit is applied to the item as it is at approval time (`PerencanaAksi`), so edits the user made meanwhile survive; a proposal that no longer makes sense disappears; deleting is flagged red + "tidak bisa dibatalkan" + "Setujui dan hapus".
- **Honest chat**: the app itself appends "Belum ada yang diubah. Cek kartu usulan di bawah…" to any reply with proposals (whatever the model wrote) and posts "Sudah dilakukan / Tidak jadi / Usulan dibatalkan" reports (flag `PesanChat.aksi`, not read aloud, labelled "Perubahan di aplikasi").
- **Privacy**: ids and the reminder summary are in the prompt only when "Bagikan data" is on (same flag and same filters as before; notes of tasks are not sent). With sharing off Hanako can only propose additions.
- **Offline (local) mode** never changes anything and says so when asked to ("tambah tugas…").
- **Not included**: actions for goals (tujuan) and memories, undo after approval, spoken confirmation, bulk reschedule, holiday/supply lists.
- **Never tried against the real Gemini API**: whether the model follows the schema and the rules is unknown. The temperature is 0.4 when functions are declared (0.8 otherwise). If proposals come out wrong, first tune the rules text in `KonteksHanako.ATURAN_AKSI` and the descriptions in `DeklarasiAksi`; the parser stays strict.

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

### Phase 7 — Voice
- **Dictation (speech to text)**: a microphone button next to the chat input opens the **system's own speech dialog** (`RecognizerIntent`, Indonesian, free-form). The recognized text is added to the input field (first letter capitalised when it starts a sentence, a single space before it, cut at 2000 characters) and the user still presses Send. This app records nothing itself, so **no `RECORD_AUDIO` permission** is requested. The dialog is normally Google's and usually needs internet; the settings screen says that audio is processed by Google, not by this app. If no recognizer exists the chat shows an honest message under the input.
- **Read aloud (text to speech)**: each Hanako bubble has a speaker icon; a stop button appears in the header while she speaks; a switch in the settings reads every new reply automatically (default off). `PembacaSuara` wraps `android.speech.tts.TextToSpeech` (Indonesian, speech rate and pitch from settings, queue flush so a new reply replaces the old one). The engine is started on first use and released when the chat's ViewModel is cleared; speech stops when the chat screen is left.
- **Text prepared for speech** (`PenyiapTeksUcap`, tested): web addresses become "tautan", "07:00–08:30" becomes "7 sampai 8 lewat 30", "16:20" becomes "16 lewat 20", bullets and numbering become separate sentences, markdown marks and emoji are removed, long dashes and "%" are spoken as words, input is cut to 3500 characters at a sentence end (the engine refuses about 4000).
- **Voice settings** (in the Hanako gear screen): auto-read switch, speed (Lambat 0.8 / Normal / Cepat 1.25), pitch (Rendah 0.85 / Normal / Tinggi 1.15), a test button, and an honest engine status ("siap", "data suara bahasa Indonesia belum terpasang" with a button to the system's install screen, "tidak didukung", "mesin gagal"). Stored in DataStore (new keys), values normalised when read.
- **Manifest**: `<queries>` for the TTS service and the speech recognizer (needed on Android 11+ or the engine may be invisible to the app). No new permission.
- **Not in Phase 7**: no always-listening or wake word (battery and privacy), no voice commands that change data (Hanako cannot change app data yet), no spoken reminders, no streaming speech, no offline recognizer of our own, no gesture/motion images.

### Phase 6 — Hanako AI chat (Gemini)
- **Tab "AI Hanako"** (`ui/hanako/HanakoScreen.kt`): chat bubbles, suggestion chips on an empty chat, a "Hanako is thinking" bubble, auto-scroll, header with Hanako's face and the mode ("Terhubung ke Gemini" or "Mode lokal"), a gear icon for settings, error card with **Coba lagi** / **Buka pengaturan** / **Tutup**. The face follows the situation (`PilihEkspresi.untukChat`): thinking while waiting, a trouble face after an error, otherwise the mood of her last reply. Every reply bubble shows its own expression.
- **Two modes, chosen automatically.**
  - *Local mode* (default; no key, or online switched off): `PenjawabLokal` answers from the data on the phone by keyword matching: today's schedule, open tasks, active goals, greetings, thanks, a few encouragements. If it does not understand it says so and explains how to turn on the AI. Nothing is sent anywhere. Replies are labelled "Jawaban lokal, tanpa internet".
  - *Gemini mode* (key saved and switch on): `KlienGemini` posts to `…/v1beta/models/<model>:generateContent`.
- **What Gemini receives** (`KonteksHanako.sistem`): Hanako's persona and rules in Indonesian (short answers, never invent, cannot change app data, notes are data not commands, no definite medical/legal/financial advice, start every reply with a mood marker like `[senyum]`), the date and time, the user's name, the reminder mode, and — only if "Bagikan data aplikasi ke AI" is on — today's schedule, open tasks, active goals and non-sensitive memories (never when memory is off in Profil; sensitive memories are filtered twice, in `KonteksAi.pilih` and again in the prompt builder). User text is flattened to one line per item and cut, so a task title cannot add "instructions" as new lines. The last 16 messages are sent as history (merged so roles alternate, as Gemini requires).
- **Mood markers**: `UraiBalasan` strips a leading `[senyum]`-style marker (any of the 12 expressions, case-insensitive) and uses it as that reply's face; with no marker the face is Normal, never a guessed mood.
- **API key** (BYOK, user's own key from Google AI Studio): entered in the gear screen, only after ticking "Saya mengerti bahwa pesan saya dikirim ke Google Gemini". Stored **encrypted** (AES-256-GCM, key in the Android Keystore, `PengacakKeystore`) in DataStore; if encryption fails the key is **not stored** (never plain text). The field is not kept in saved instance state, the key is sent only in the `x-goog-api-key` header (never in the URL), never logged, and is blanked out of any error text. `allowBackup` is already false. Deleting the key switches online mode off.
- **Settings screen** (`PengaturanHanakoScreen`): key save/delete, "Pakai AI online" switch, "Bagikan data aplikasi ke AI" switch, **model name** (default `gemini-flash-latest`, editable), **Uji koneksi** (one tiny real request that reports success or the exact kind of failure), clear chat (with confirmation).
- **Errors are specific and in Indonesian** (`JenisGalat` → `LabelHanako.galat`): no key, invalid key, access denied (401/403, key restrictions or region), quota/rate limit (429), model not found (404), request rejected, blocked by Google's safety filter, empty/truncated answer, server trouble (5xx), no internet, timeout. The server's short message is shown in small print.
- **History**: the last 40 messages are kept in DataStore (`RiwayatChatCodec`, tolerant of corrupt lines), a failed message stays in the history so **Coba lagi** can resend it.
- **Manifest**: added `INTERNET` (only used in Gemini mode).
- **Not in Phase 6**: Hanako cannot change app data (no function calling: she tells the user which tab to open), no streaming (the whole reply arrives at once), no voice (Phase 7), no Supabase/Edge Function proxy and no automatic memory learning (Phase 9), no gestures/chibi images.

### Hanako character
- The user's character sheet is kept at `docs/referensi/hanako-lembar-karakter.png` (1536x1024). `tools/potong-hanako.py` cuts the 12 face expressions (normal, senyum, tertawa, malu, sedih, marah, bingung, terkejut, cemberut, mengantuk, berpikir, panik) into `app/src/main/res/drawable-nodpi/hanako_*.webp` (112 KB total). Coordinates are relative to a 1536x1024 sheet and scaled to the real file size, so a higher-resolution version of the same sheet can be dropped in and the script re-run for sharper images.
- `domain/hanako/EkspresiHanako.kt` (enum + `PilihEkspresi`, tested) and `ui/hanako/HanakoAvatar.kt`. The Beranda card shows Hanako with an expression that follows the time of day (sleepy 22:00–03:59); onboarding changes expression per step; the Pengingat screen shows her mood (confused when notifications are blocked, thinking in Fokus, sleepy in Istirahat).
- The earlier temporary vector illustration was deleted.
- **Limit**: the faces are only about 123 px wide, so they get blurry above roughly 100 dp. Not cut yet (coordinates are in the script comment): chibi, 7 gestures, 6 motion poses, front/side/back poses. Use them in Phase 6 (chat moods) and Phase 7 (voice).

### Earlier phases (unchanged)
Phases 1–4: Room v1 (all tables), DataStore settings/profile/layout, onboarding, Memori Hanako, Jadwal (day/week/month, conflicts, school hours/timetable/duty), Aktivitas (tasks, goals, milestones, history), dashboard cards with real data. See CHANGELOG.md.

## Current project structure (new or changed in Phases 5, 6 and 7 and the Hanako actions)
```
app/src/main/kotlin/id/asisten/app/
  AsistenAplikasi.kt *              container (+ hanako, klienGemini) + mulaiPengingat() (channels, worker, startup catch-up, edit observer)
  MainActivity.kt *                 opens the tab requested by a notification (singleTop + onNewIntent)
  domain/pengingat/                 Pengingat, PengaturanPengingat, Kebijakan, PenghitungPengingat(+ProsesPengingat),
                                    PenyusunNotifikasi, LabelPengingat   (pure, tested)
  domain/hanako/ (actions)          AksiHanako (+Perubahan*, terapkan, UsulanTampil/Mentah), UraiAksi (validation), DeklarasiAksi (Gemini
                                    function declarations), LabelAksi (card text, warnings, chat reports), PerencanaAksi (approved action ->
                                    concrete step), UsulanHanako (RiwayatUsulan storage codec, BalasanAksi)   (pure, tested)
  data/hanako/PelaksanaAksi.kt      executes a step through the existing repositories (Android layer, not compiled here)
  domain/hanako/                    EkspresiHanako (+PilihEkspresi), MiniJson (+tulis), PercakapanHanako (messages, history, codec),
                                    ProtokolGemini (request body, response parser, error kinds, mood marker),
                                    KonteksHanako (persona prompt + RingkasData), PenjawabLokal, LabelHanako,
                                    PengaturanHanako (+NamaModel, KunciGemini)   (pure, tested)
  domain/suara/                     PenyiapTeksUcap, PengaturanSuara (+PilihanSuara, StatusSuara, LabelSuara, PenyisipDiktean)   (pure, tested)
  data/ai/KlienGemini.kt            HttpURLConnection client (tested against a loopback server)
  data/suara/SuaraRepository.kt     voice settings in DataStore
  suara/PembacaSuara.kt             TextToSpeech wrapper (Android only)
  data/hanako/                      HanakoRepository (settings, encrypted key, history), PenyimpanKunci (Keystore AES-GCM)
  ui/hanako/ *                      HanakoScreen, PengaturanHanakoScreen, HanakoViewModel (+ HanakoAvatar)
  notifikasi/                       PenampilNotifikasi, PenjadwalPengingat, Penerima (3 receivers), PekerjaPengaman,
                                    CekIzin, KunciIntent
  data/pengaturan/PengaturanRepository.kt *   + reminder keys, `pengingat`, `keadaanPengingat`
  ui/pengingat/                     PengingatScreen, PengingatViewModel
  ui/{beranda,onboarding,profil,navigation}/ *   Hanako avatar, entry to Pengingat, tab from notification
app/src/main/res/drawable/ic_notifikasi.xml, ic_mikrofon.xml, ic_suara.xml, ic_berhenti.xml;  drawable-nodpi/hanako_*.webp
tools/jvm-check.sh (now also compiles data/ai)  tools/cek-statis.py (+R.raw)  tools/potong-hanako.py  tools/buat-nada.py
-- added by the 2026-11 extras --
  domain/hanako/PenyediaAi.kt, HasilAi.kt, ProtokolAi.kt (shared + OpenAI-style for Groq/DeepSeek), ProtokolGemini.kt (Gemini only)   data/ai/KlienAi.kt (replaces KlienGemini)
  domain/pengingat/SuaraPengingat.kt   data/pengingat/SuaraPengingatRepository.kt   notifikasi/PengelolaSuara.kt, PemutarContoh.kt
  ui/pengingat/BagianSuara.kt   res/raw/nada_{lonceng,kicau,kotak_musik,bel_sekolah,alarm_digital}.ogg
  domain/suara/SuaraMesin.kt (DaftarVoice)   PengaturanSuara.kt (+PaketSuara)
  domain/pengaturan/KontrasWarna.kt   ui/theme/Theme.kt, GayaNeo.kt   ui/komponen/Neo.kt, Komponen.kt
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
15. **Bring-your-own Gemini key, no backend yet.** The app has no server, so the user pastes their own key; it is encrypted with a Keystore key. Moving the key into a Supabase Edge Function is a Phase 9 option. No key ever lives in the repository or the APK.
16. **No JSON library, no HTTP library.** `MiniJson` (reader + string writer) and `HttpURLConnection` keep the dependency list unchanged and let all of it run in plain JVM tests (Android's `org.json` is a stub there).
17. **Model name is a setting, default `gemini-flash-latest`.** Google's model names change often. Only third-party pages could be reached while writing this (they mentioned names such as `gemini-3.6-flash`), so the default alias was NOT checked against Google's documentation. If "Uji koneksi" says the model is not found, change the name in the gear screen; no rebuild needed.
18. **Generous output budget (2048 tokens) and no thinking parameter.** Thinking models spend output tokens on reasoning; the thinking-config field name differs per model generation and a wrong one can be rejected, so none is sent. An answer that is empty because the budget ran out is reported as "terpotong", not shown blank.
19. **Local mode is a real feature, not a placeholder**: keyword answers from real data, honest when it does not understand. It never sends anything.
20. **Chat history in DataStore, not Room**: keeps the Room schema at version 1 (a migration is planned in Phase 9 for memory status).
21. **Dictation through the system dialog, not an in-app `SpeechRecognizer`.** No microphone permission, no foreground service, no always-on listening; the trade-off is that the Google dialog appears over the app and needs the Google app (and usually internet).
22. **Own icons as vector drawables** (microphone, speaker, stop): only `material-icons-core` is a dependency and it has none of them; adding icons-extended would bloat the APK.
23. **Speech text is cleaned in pure code** so that what the engine says is testable here; the engine and recognizer are the only untestable parts.
24. **TTS is released when the chat ViewModel is cleared** and stops when the screen is left, so the engine does not stay bound in memory on a 4 GB phone.
25. **One protocol per API family, not per provider**: Groq and DeepSeek share `PermintaanOpenAi` / `UraiResponsOpenAi`; the few differences (token-limit field, DeepSeek's `thinking` switch) are explicit per provider. The base URL is a fixed constant per provider so a key can only ever go to its own service.
26. **One notification channel per sound choice.** Android freezes a channel's sound at creation, so a changed sound needs a new channel; the id is a stable hash of the choice and stale channels are deleted.
27. **Picked music is copied into the media store** (no storage permission on API 30+) because a notification can only play a URI the notification system can read; the app deletes only copies it made.
28. **Neo look without new dependencies**: shadows are `drawBehind` rounded rectangles, buttons are `Box`/`Row` with `clickable`, so no API beyond foundation/material3 core is needed; each block reserves its shadow space with padding. Text colour on the accent follows WCAG contrast, never a fixed white.

## Tests performed and their REAL results
- **Extras of 2026-11 (this session)**: `tools/jvm-check.sh` **OK (492 tests)**: 409 before, 77 new for Groq/DeepSeek + reminder sounds + cute voice (including loopback-server tests for all three providers, FNV-1a test vectors for channel ids, existence of the bundled `.ogg` files) and 6 for `KontrasWarna`. `tools/cek-statis.py` clean on 140 Kotlin and 13 XML files (it now also checks `R.raw` references; verified by deleting a raw file, which it reported).
- **Deliberate-bug checks on `KontrasWarna`** (scratch copies): inverted dark/white decision -> 3 failures; swapped RGB weights -> 1; wrong sRGB exponent (2.0) -> **first survived**, then a fixed-reference-value test was added and it is caught; wrong linearisation threshold -> caught; contrast offset 0.05 -> 0.0 -> 4 failures.
- **Independent read of the new Compose code**: a separate review agent checked `Neo.kt`, `Komponen.kt`, `Theme.kt`, `GayaNeo.kt`, `AsistenNavHost.kt`, `KontrasWarna.kt` and every call site against the Material3 1.4 / foundation APIs and reported no compile error; unsure (90–95 %): `Typography().copy(...)`, `Shapes(...)`. It is a read, not a compile. The earlier Android-only code of the Groq/DeepSeek, sound and voice work had no such review.
- **Not run (still)**: Gradle, KSP, Compose compilation, any device, real calls to Groq / DeepSeek / Gemini, listening to tones and voices, looking at the new screens.
- **Hanako actions (2026-11-16)**: `tools/jvm-check.sh` **OK (409 tests)**, `tools/cek-statis.py` clean on 123 Kotlin and 12 XML files. 123 new tests: `UraiAksiTest` (every function valid and invalid, ids, dates, times, ranges, cleaning), `DeklarasiAksiTest` (driven from the declarations: every declared name, every field and every required field is really read by the parser; enum lists equal the app's enums; JSON shape; no forbidden words), `ProtokolAksiTest` (function calls in responses, request body with tools, temperature), `UsulanHanakoTest` (storage codec, expiry, de-duplication, balasan text, chat codec flag), `PerencanaAksiTest` (edits applied to current data, deleted items, invalid results), `LabelAksiTest`, `KonteksAksiTest` (ids, rules, privacy, injection by line breaks, size), `PenjawabLokalUbahTest`, `MiniJsonTulisTest`.
- **Sanity check of those tests**: deliberate bugs in scratch copies (delete not flagged destructive, ids not required to exist, "Belum ada yang diubah" removed, planner not checking existence, a misspelled declared field name, date range unchecked, tanggal+hari accepted together, expiry unchecked, unknown function accepted, snooze limit loosened, break slots editable, no 5-proposal cap, invisible characters not stripped, tools always / never sent, chat flag lost, prefix matching of change words). Result: **18 of 18 deliberate bugs made at least one test fail** (none went unnoticed).
- `tools/jvm-check.sh`: compiles 36 domain/`data/ai` files plus the entity/mapper files against Room annotation stubs, and 27 test files, with Kotlin 2.0.21, then runs JUnit. **Result: OK (286 tests)**: 179 from Phases 1–5, **22 new for Phase 7** (speech text cleaner 10, settings/status/dictation helpers 12) and **85 for Phase 6**: `MiniJson` 9 (nested values, escapes, round trip of tricky text, malformed input, depth limit), chat history and codec 9, request body / response parser / error mapping / mood marker 16, prompt builder 12 (date and time, sensitive memory never included, "not shared" mode, injection by line breaks, size cap, all 12 expression markers named), local responder 14, error labels 4, settings/model/key validation 6, expression choice 2, and **13 tests of `KlienGemini` against a real server on 127.0.0.1** (method, path, key only in the header, never in the URL, request body bytes incl. emoji, 400/403/404/429/503 mapping, key blanked out of error text, timeout, connection refused, invalid model names and blank key never reach the server, redirects not followed, 3 MB response cut off, HTTPS-only rule, coroutine entry point).
- **Sanity check of the tests themselves**: seven deliberate bugs in scratch copies (sensitive memories included, "thought" parts not skipped, quote not escaped, history not starting with a user turn, redirects followed, key not blanked, model name not validated): the suite failed 7 + 3 tests, i.e. it detects them.
- The Phase 7 tests found **two real bugs** in the first version of the speech text cleaner (a full stop was placed after an emoji, and an emoji-only reply became "."); both were fixed and are covered now.
- `tools/cek-statis.py`: clean on 106 Kotlin and 12 XML files (all `R.string` / `R.drawable` references resolve, imports resolve, manifest classes exist).
- **Independent review**: a separate review agent was started for the Android-only Phase 6 files (screens, ViewModel, repository, Keystore code) but was cut off twice by a usage limit and reported nothing. **That code, and the Android-only Phase 7 code (`PembacaSuara`, the screens' voice parts), has had no independent review.** Only signatures of the functions it calls were re-checked by grep.
- **Not run**: `./gradlew`, KSP/Room, Compose compilation, `HanakoRepository`/`SuaraRepository`/DataStore, the Keystore code, `TextToSpeech`, the speech dialog, WorkManager, instrumented tests, lint, **any on-device test, any request to the real Gemini API**. **No APK exists.**

## Known issues, risks and gaps
Newest unverified risks (2026-11 extras, in the order I would check them if the build fails):
- `Typography().copy(...)` and `Shapes(...)` in `Theme.kt` (Material3 1.4 added expressive overloads; if overload resolution complains, build `Typography(headlineLarge = ..., ...)` from the defaults).
- `Neo.kt`: `Modifier.clickable(interactionSource, indication = null, ...)`, `selectable`, `Box(propagateMinConstraints = true)`, `offset { }`, `ProvideTextStyle`.
- `Komponen.kt`: the `Dialog`-based dialogs (`weight(1f, fill = false)` inside a bounded column) and `OutlinedTextFieldDefaults.colors`.
- `PengelolaSuara` (MediaStore insert with IS_PENDING), `PenampilNotifikasi.siapkanSaluran` (`NotificationChannel.setSound` with `AudioAttributes`), `PembacaSuara` (`TextToSpeech.voices`, `setVoice`), the ringtone-picker and document-picker launchers in `BagianSuara.kt`.
- Behaviour: a custom song may not play from the media store on ColorOS; the bundled tones may be too quiet or too sharp; Imut pitch 1.7 may sound distorted on some engines; the neo shadows may clip or look heavy on a small phone; DeepSeek / Groq default model names may be retired.
Unverified compile risks of earlier phases (most likely first):
- KSP + AGP 9 built-in Kotlin (decision 2).
- Compose/Android API details written without a compiler: `LifecycleEventEffect`, `rememberLauncherForActivityResult(RequestPermission)`, `mutableLongStateOf`, `TabRow`/`Tab`, `LinearProgressIndicator(progress = { })`, `SystemBarStyle.auto`, NavHost transitions.
- Phase 5 platform APIs: `Notification.Builder` + `Notification.Action.Builder(Icon, …)`, `PendingIntent` flags, trailing commas in `when` conditions in `PenerimaSistem`, `PeriodicWorkRequest.Builder(Class, Long, TimeUnit)`, `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` constant, `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- WorkManager 2.12.0: its minimum compileSdk/AGP was not re-checked.
- `wadahViewModel` (inline composable with `crossinline`) and smart-casts in `MainActivity`'s `when`.
- Phase 6 Android files (never compiled, never reviewed by a second pair of eyes): `SuggestionChip`, `OutlinedTextField` with `visualTransformation`/`keyboardOptions`, `imePadding`, `SelectionContainer`, `LocalUriHandler`, `PasswordVisualTransformation` in the Compose screens; `KeyGenParameterSpec`, `GCMParameterSpec(128, bytes, 0, 12)` and `java.util.Base64` in `PenyimpanKunci.kt`; `data object` and expression-bodied `try` in `HanakoViewModel`.
- `KlienGeminiTest` uses `com.sun.net.httpserver`. It compiles in the JVM harness, but if the Android Gradle unit-test compilation cannot see that JDK package, delete `app/src/test/kotlin/id/asisten/app/data/ai/KlienGeminiTest.kt` (the logic is also covered by the other tests) and say so in the next handoff.

Phase 7 compile risks (never compiled or reviewed): `rememberLauncherForActivityResult(StartActivityForResult)` and `ActivityResult.data` in `HanakoScreen`, `UtteranceProgressListener` overrides (the deprecated `onError(String)` must stay), `TextToSpeech.speak(CharSequence, Int, Bundle?, String)`, `floatPreferencesKey`, `Icon(painterResource(...))` with the new vector drawables, `<queries>` placement in the manifest.

Phase 7 behavior risks:
- The speech dialog may be missing or may need internet; the Indonesian TTS voice may be missing (the settings screen reports it). On the Narzo 50i Prime (Android 11) the Google speech engine is normally present, but this was not tried.
- Spoken times ("16 lewat 20") and bullet pauses are a best guess at how the engine reads them; adjust `PenyiapTeksUcap` after listening.
- Auto-read speaks every new reply, including local ones; if that is annoying, leave the switch off (default).

Phase 6 behavior risks:
- The default model name may be wrong or retired: use Uji koneksi first (see decision 17).
- A free Google AI Studio key has rate limits; "Jatah pemakaian … habis" (429) is expected under heavy use.
- The keyboard: the chat input uses `imePadding()` inside a Scaffold that also has the bottom bar, so there may be an extra gap above the keyboard equal to the bar's height. Cosmetic; check on the phone.
- Quality of the Indonesian replies and whether Gemini follows the "[mood] first" rule is unknown until tried; without the marker the face stays Normal.
- Google may use free-tier data to improve its products (the screen says so); the user decides by ticking the consent box and by the "Bagikan data" switch.

Behavior risks to watch on the realme Narzo 50i Prime (ColorOS kills background apps aggressively):
- If alarms are late or missing, the user must allow background activity and disable battery optimization (the screen explains this and links to the page). The WorkManager net only helps when the system lets it run.
- Force-stopping the app cancels its alarms until the app is opened again; on the next start the last 2 hours of missed reminders are delivered.

Gaps against the master prompt (not hidden):
- Holiday calendar for school reminders; per-day exceptions for weekly activities.
- Android 13+ has no prompt outside the Pengingat screen (the target phone is Android 11).
- School supply checklist, OCR import, bulk reschedule, memory prediction status (see earlier handoff; the last needs a Room migration in Phase 9).
- Hanako: only faces are used (now also in the chat); gestures, motion poses, chibi and higher-resolution images are still to do.
- Hanako can propose changes to tasks, activities, timetable and reminder settings (see "Hanako actions"), but not to goals or memories; no streaming, no wake word, no spoken reminders.
- Prayer, habits, money, study, wellness, journal: Phase 8. Supabase and automatic memory learning: Phase 9.

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
6. Phone checklist, **Phase 6**:
   a. Open the **AI Hanako** tab: the empty chat shows Hanako's face and three suggestions. Tap "Apa jadwalku hari ini?": a local answer with real data appears, labelled "Jawaban lokal".
   b. Gear icon -> paste your Gemini key (from aistudio.google.com/apikey), tick the consent box, **Simpan kunci**: it says saved and the field empties. Tap **Uji koneksi**: expect "Berhasil". If it says the model was not found, change the model name (see decision 17) and test again.
   c. Back in the chat, ask something free ("ceritakan lelucon singkat") and something about your data ("tugas apa yang paling mendesak?"): the answer must match your real tasks, Hanako's face should change with the mood, and the header must say "Terhubung ke Gemini".
   d. Turn **Bagikan data aplikasi ke AI** off and ask about your schedule: she must say she cannot see it. Turn memory off in Profil, or mark a memory sensitive: it must never show up in her answers.
   e. Switch airplane mode on and send a message: a "Tidak bisa terhubung ke internet" card with **Coba lagi**; switch it off and tap Coba lagi: the answer arrives.
   f. Kill and reopen the app: the chat history is still there; **Hapus semua percakapan** empties it. **Hapus kunci** returns to local mode.
   g. Note any crash, odd layout (keyboard gap), wrong face or wrong answer.
7. Phone checklist, **Phase 7**:
   a. In the chat tap the **microphone**: the system speech dialog opens in Indonesian; say "apa jadwalku hari ini": the text appears in the input (capitalised) and nothing is sent until you press Send. Deny or cancel: nothing breaks.
   b. Tap the **speaker icon** under a Hanako bubble: she reads it; the header shows a stop button while she speaks; tapping stop silences her. Leave the tab while she speaks: she stops.
   c. Gear screen -> **Suara**: tap **Uji suara**; change speed and pitch and test again; the status line says whether Indonesian is available (if the data is missing, **Pasang data suara** opens the system installer).
   d. Switch **Bacakan balasan otomatis** on and send a message: the reply is read by itself. Listen to how times and lists sound ("07:00–08:30 Matematika"), and note anything that sounds wrong.
   e. Switch airplane mode on and try the microphone: note whether the recognizer works offline on this phone.
8. Phone checklist, **Hanako actions** (needs the key from step 6):
   a. Gear screen: the switch **Izinkan Hanako mengusulkan perubahan** is on. Ask "tambah tugas PR Matematika tenggat besok": a card "Usulan Hanako, belum dijalankan" appears with the description; the tab **Aktivitas** still has NO such task. Tap **Setujui**: a grey "Sudah dilakukan" message appears and the task is now in Aktivitas.
   b. Ask "ubah tenggat PR Matematika jadi lusa": the card shows old -> new date. Tap **Tolak**: "Usulan dibatalkan", nothing changed.
   c. Ask "hapus tugas PR Matematika": the card is red, says "Tidak bisa dibatalkan", the button says "Setujui dan hapus".
   d. Ask "ingatkan aku besok jam 7 pagi untuk olahraga": she should propose an activity at 07:00 and say when the reminder rings. After Setujui, Profil -> Atur pengingat -> Pengingat berikutnya must list it. Then ask "atur pengingat jadi tepat waktu": a reminder-settings card.
   e. Ask "mulai mode fokus 30 menit": card; after Setujui the Pengingat screen shows Fokus.
   f. Ask something that clashes with a lesson: the card must show "Bentrok dengan ...".
   g. Switch the new setting off and repeat (a): no card, and she says she cannot change data. Turn sharing off: she must say she cannot see the data and can only add.
   h. Note every case where she proposes something wrong or claims it is done; paste the chat text next time.
9. Phone checklist, **extras of 2026-11**:
   a. **Look**: open every tab (Beranda, Jadwal with Kalender and Sekolah, Aktivitas with its three tabs, AI Hanako, Profil) in light and in dark mode and with each of the four accent colours; open a dialog (Tambah kegiatan) with the keyboard up; the Hanako chat bubbles; the Pengingat screen. Note anything clipped, overlapping, too small to tap, hard to read, or ugly, and say what you want changed (the style details were chosen by the session, not by you).
   b. **Groq / DeepSeek**: gear screen -> pick Groq, paste a key from console.groq.com/keys, tick consent, Simpan kunci, Uji koneksi. Ask a free question and a data question. Then the same with DeepSeek (platform.deepseek.com/api_keys; it needs balance: "saldo habis" = 402). Switch back to Gemini: its key and model must still be there. Try "tambah tugas ..." with each service and report whether a proposal card appears. If a model name is refused, use one of the suggestion chips or the provider's current name.
   c. **Reminder sound**: Profil -> Atur pengingat -> Suara pengingat. Try the preview of each of the five tones; pick one and press Kirim notifikasi uji; pick a system tone; pick a music file from the phone and test again (does ColorOS play it?); switch "Pakai volume alarm" and compare the volume. Choose Bawaan to go back. Note which tones are too quiet, too loud or unpleasant.
   d. **Cute voice**: Hanako's settings -> Suara: tap the package Imut, then Imut banget, and Uji suara; scroll the engine's voices and tap Dengar on each (is there a better Indonesian voice installed? Settings -> Language -> Text-to-speech may offer more); say which sounds best.
10. Then Phase 8 (Life Management): prayer, habits, money, study, wellness, journal. Check the master prompt for the exact scope first.

## Continuation prompt (paste into the next session)
```
Lanjutkan proyek ASISTEN dari HANDOFF.md terbaru (jangan mulai ulang, jangan minta saya mengulang persyaratan).
Status: kode Fase 1–7, "aksi Hanako" (Hanako mengusulkan perubahan yang harus disetujui pengguna), dan tambahan November 2026 (Groq + DeepSeek di samping Gemini, suara pengingat/alarm dengan nada bawaan dan musik dari HP, suara Hanako paket Imut, tampilan neo-brutalism) sudah ditulis; logika murni dan klien HTTP semua layanan AI terverifikasi (492 tes JVM lolos lewat tools/jvm-check.sh, pemeriksa statis tools/cek-statis.py bersih), tetapi build Android, alarm/notifikasi, chat Hanako, suara, dan tampilan baru belum pernah dikompilasi atau dijalankan di HP, dan belum ada permintaan nyata ke Gemini, Groq, maupun DeepSeek.
Hasil GitHub Actions saya: <tempel log error, atau tulis "hijau, APK terpasang, semua pemeriksaan di langkah 4 sampai 9 berhasil" / sebutkan yang gagal, termasuk nama model yang ditolak, suara yang tidak ada, nada yang jelek, bagian tampilan yang tidak enak dilihat, atau jawaban Hanako yang salah>.
Master prompt asli: <tempel bagian Fase 6 dan 7 (dan Fase 8–10) kalau ada yang belum tercakup>.
Jika ada error: perbaiki penyebabnya secara minimal dan jelaskan, tanpa menambah fitur baru. Jika error KSP "kotlin.sourceSets DSL", tambahkan android.disallowKotlinSourceSets=false. Jika KlienGeminiTest tidak bisa dikompilasi di Gradle, hapus berkas tes itu dan catat.
Jika hijau: kerjakan Fase 8 (manajemen hidup) sesuai master prompt, lalu perbarui PROGRESS.md, HANDOFF.md, CHANGELOG.md dengan hasil tes yang nyata.
```
