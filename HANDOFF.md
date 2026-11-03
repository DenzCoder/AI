# HANDOFF — after Phases 3 and 4 (Local Database; Schedule and Activities)

Last updated: 2026-11-03

## Status in one line
Phases 1–4 source code is written. The **domain logic is verified** (109 JVM unit tests pass). The **Android build has never run** (Gradle, KSP/Room, Compose, DataStore, instrumented tests, device): the authoring sandbox cannot reach Gradle/Maven. Do not start Phase 5 until the GitHub Actions build is green and the debug APK has been tried on the phone.

## How the code came to be (read this)
Part of Phase 3/4 (domain, data layer, repositories, ViewModels, most unit tests, `tools/jvm-check.sh`) was written in an earlier working session whose conversation was not available to the session that finished the phases. While finishing, five files from that earlier work were accidentally overwritten and then reconstructed from the code and tests that use them:
- `domain/memori/Memori.kt` (rebuilt to the API its callers and tests need: `ItemMemori`, `KategoriMemori`, `KonteksAi.pilih`, `ValidasiMemori`; the `MAKS` limit of 500 is a re-chosen value).
- `domain/dasbor/TataLetakCodec.kt` (rebuilt; passes the 8 existing codec tests; the stored text format may differ from the lost original, which is harmless because no build was ever installed).
- `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` (rewritten; versions below were re-verified against official pages).
- `Pengaturan.kt` was restored to its committed state.
After the repair, `tools/jvm-check.sh` passes all 109 tests, so the reconstruction is consistent with the rest of the code.

## Completed work

### Phase 3 — Local database
- **Room** database `asisten.db`, **version 1**, `exportSchema = true`. Entities: `memori`, `kegiatan`, `jam_sekolah`, `pelajaran`, `piket`, `tugas`, `tujuan`, `tonggak` (FK to `tujuan`, ON DELETE CASCADE). Conventions: dates are epoch days, times are minutes since midnight, weekdays ISO 1..7, enums stored by name; conversion lives in `data/db/Pemetaan.kt` (unit-tested, unknown stored values fall back safely).
- **DataStore (Preferences)**: appearance settings, user profile (name, onboarding done, memory on/off) and the dashboard layout with its undo history (`TataLetakCodec`). Updates are read-modify-write inside one `edit {}`.
- **Repositories** expose domain models only; **hand-written container** `WadahAplikasi` (no DI framework); `wadahViewModel { }` helper creates ViewModels.
- **Migration policy**: version 1 is the first schema (all Phase 3 and Phase 4 tables); no build was installed before it. `fallbackToDestructiveMigration` is deliberately NOT used. Any later entity change needs a version bump, a real `Migration`, and the new schema JSON.
- **Persistence now real**: theme, accent, text size, animation toggle, name, onboarding state, memory switch and widget arrangement (with undo) survive restarts.
- **Onboarding** (5 steps): welcome, preferred name (required), theme/accent, privacy and memory (memory is **opt-in, off by default**), pointer to school setup. No account needed.
- **Memori Hanako screen** (Profil → Kelola memori): view by the 4 categories, add, edit, delete (with confirmation), delete all, mark sensitive, switch memory off. `KonteksAi.pilih` returns nothing when memory is off and leaves out sensitive items unless explicitly requested (for Phase 6).

### Phase 4 — Schedule and activities
- **Jadwal tab**: *Kalender* (day / week / month views; navigation; month grid with dots; tap a day to open it), activity form (one-time or weekly, priority, notes), **conflict detection** (also against the school timetable; breaks never conflict) with an **earliest free slot suggestion** and a "use this slot" button; *Sekolah* (entry/dismissal hours per weekday, subject timetable with teacher/room and break slots, duty roster).
- **Aktivitas tab**: *Tugas* (tasks, homework and exams with due-date status, priority, filter, complete/undo), *Tujuan* (short/long-term goals, milestones, progress bar, mark achieved), *Riwayat* (completed tasks and achieved goals, undo).
- **Beranda** now shows real data on 4 cards (today's schedule, nearest tasks, school hours + duty, active goals); the other 3 cards (prayer, habits, money) still say honestly that the feature is not available yet.
- All forms validate input (`domain/formulir`) and show Indonesian error messages; school data is never pre-filled.

## Current project structure (new or changed since Phase 2)
```
app/src/main/kotlin/id/asisten/app/
  AsistenAplikasi.kt                         WadahAplikasi container + Application
  MainActivity.kt *                          loads settings/profile -> onboarding or app
  domain/{Enums,Label,Prioritas}.kt          keEnum(), Indonesian labels, Prioritas
  domain/{jadwal,aktivitas,formulir,waktu,memori,profil}/   pure logic (JVM-testable)
  domain/dasbor/{RingkasanBeranda,TataLetakCodec}.kt
  data/db/{Entitas,Dao,Pemetaan,AsistenDatabase}.kt
  data/pengaturan/PengaturanRepository.kt    DataStore
  data/repo/Repository.kt                    Memori/Jadwal/Aktivitas repositories
  ui/WadahViewModel.kt  ui/komponen/Komponen.kt
  ui/{beranda,jadwal,sekolah,aktivitas,memori,onboarding,profil,pengaturan,navigation}/
app/src/test/...            11 JVM test classes (109 tests)
app/src/androidTest/...     data/db/AsistenDatabaseTest.kt (5 instrumented tests, NOT run)
app/schemas/                empty until the first build (see next steps)
tools/jvm-check.sh, tools/stubs/   JVM-only logic check (Kotlin 2.0.21 from Gradle's lib dir)
```

## Important technical decisions
1. **Room 2.8.5** (not Room 3.0.3): Room 3 is a breaking redesign (new `androidx.room3` package, `SQLiteDriver`, all-suspend DAOs) and cannot be tried here, so the lower-risk, better-documented line was chosen. Revisit only with a working build.
2. **KSP 2.3.10** (pairing shown in the Kotlin docs). Reports about KSP with AGP 9 "built-in Kotlin" conflict: one source says `android.disallowKotlinSourceSets=false` is required, another says KSP 2.3.6+ works without flags. No workaround flag is set. **If CI fails with "Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin", add `android.disallowKotlinSourceSets=false` to `gradle.properties`** (documented KSP workaround).
3. **DataStore 1.3.0** for small key-value data; Room for structured records.
4. Explicit `sourceSets` block removed from `app/build.gradle.kts` (AGP's built-in Kotlin already includes `src/main/kotlin`; the block was an AGP 9 DSL risk).
5. Memory is **opt-in**: the onboarding switch defaults to off. (`ProfilPengguna.memoriAktif` defaults to true only before onboarding finishes; onboarding always writes an explicit value.)
6. Recurrence is weekly only, and editing/deleting a weekly activity affects every week (no per-day exceptions yet).
7. Mixed string style: Phase 1–2 screens use `strings.xml`; Phase 3–4 screens use Indonesian literals via shared components (`Label.kt`, `Komponen.kt`). Move to resources before any localization work.

## Tests performed and their REAL results
- `tools/jvm-check.sh`: compiles 19 domain files plus the entity/mapper files against Room annotation stubs, and 11 test files, with Kotlin 2.0.21, then runs JUnit. **Result: OK (109 tests).** This proves the domain logic, form validation, entity mapping and layout codec. It does NOT cover Android, Compose, Room, DataStore or the project's real Kotlin version.
- Static checks (Python script): all 9 XML resources are well-formed; every `R.string`/`@string` and drawable reference resolves; brackets balance and `package` matches the directory in all 57 Kotlin files; every internal `import id.asisten.app.*` resolves to a declared symbol (the generated `R` class is excluded: it only exists after a build). These cannot detect type errors or API misuse.
- **Not run**: `./gradlew` (blocked: services.gradle.org / Maven hosts return 403 in the sandbox), KSP/Room annotation processing, Compose compilation, `AsistenDatabaseTest` (needs an emulator), lint, any on-device test. **No APK exists.**

## Known issues, risks and gaps
Unverified compile risks (most likely first):
- KSP + AGP 9 built-in Kotlin (see decision 2); Room schema export path `app/schemas`.
- Compose API details: `TabRow`/`Tab` (may be deprecated, not removed), `LinearProgressIndicator(progress = { })`, `SystemBarStyle.auto` in `MainActivity`, `clickable(role = …)`, `Density(...)` in the theme, `LifecycleEventEffect`, NavHost transition lambdas.
- `wadahViewModel` (inline composable with `crossinline`) and smart-casts in `MainActivity`'s `when`.

Gaps against the master prompt (not hidden):
- **School supply checklist**: no table or UI yet.
- **OCR schedule import** (and entry via Hanako chat/voice): later phases (OCR needs ML Kit or Gemini vision; chat/voice are Phases 6–7).
- **"Rapikan jadwal"**: only a free-slot suggestion on conflict exists; no bulk reschedule.
- **Memory**: items are all user-typed; no "guess/prediction" status or automatic learning yet. That needs new columns, so it requires **a Room version bump with a real migration in Phase 9** (v1 may be installed by then).
- Reminders, alarms, notifications, quiet hours: Phase 5. Prayer, habits, money, study, wellness, journal: Phase 8. Gemini/Supabase: Phases 6 and 9.
- Persistence of the layout "restore previous arrangements" is the capped (20) undo history plus reset to default; there is no named-snapshot picker.
- `AsistenDatabaseTest` was never executed.

## Exact next steps
1. Create a (private) GitHub repo; in Termux: `git remote add origin <url>` then `git push -u origin main`.
2. Actions → run **Build ASISTEN**. If it fails, paste the full failing log into the next session; fix compile errors first, add no features.
3. When green: download the **room-schemas** artifact and commit its contents into `app/schemas/` (needed for future migration checks); download `asisten-debug-apk` and install it on the realme Narzo 50i Prime.
4. On the phone check: onboarding (name required, memory switch defaults to off); theme/accent/text size/animation persist after force-closing the app; Atur widget order persists; Profil → Kelola memori (add/edit/delete); Jadwal → add a one-time and a weekly activity, create a conflict and see the warning and slot suggestion, switch day/week/month; Sekolah → add hours, a lesson, a break, a duty; Aktivitas → add a PR with a due date, complete it, find it in Riwayat, add a goal with milestones; Beranda cards show that data.
5. Optionally run `./gradlew connectedDebugAndroidTest` (emulator needed) for `AsistenDatabaseTest`.
6. Then start Phase 5 (alarms and notifications): `AlarmManager`/`WorkManager`, channels, actions Selesai/Tunda/Buka, reboot recovery, quiet hours, Focus/Rest modes. Do not request the Android 13 notification permission on Android 11; handle exact-alarm access correctly.

## Continuation prompt (paste into the next session)
```
Lanjutkan proyek ASISTEN dari HANDOFF.md terbaru (jangan mulai ulang, jangan minta saya mengulang persyaratan).
Status: kode Fase 1–4 sudah ditulis; logika domain terverifikasi (109 tes JVM lolos lewat tools/jvm-check.sh), tetapi build Android belum pernah dijalankan.
Hasil GitHub Actions saya: <tempel log error, atau tulis "hijau, APK terpasang, semua pemeriksaan di langkah 4 berhasil" / sebutkan yang gagal>.
Jika ada error: perbaiki penyebabnya secara minimal dan jelaskan, tanpa menambah fitur baru. Jika error KSP "kotlin.sourceSets DSL", tambahkan android.disallowKotlinSourceSets=false.
Jika hijau: kerjakan Fase 5 (Alarm dan Notifikasi) sesuai master prompt, lalu perbarui PROGRESS.md, HANDOFF.md, CHANGELOG.md dengan hasil tes yang nyata.
```
