# HANDOFF — after Phase 2 (UI and Dashboard)

Last updated: 2026-11-02

## Status in one line
Phases 1 and 2 source code is written. **Nothing has been compiled, unit-tested, or installed** (build hosts are blocked in the authoring sandbox). Do not start Phase 3 until the GitHub Actions build is green and the debug APK has been tried on the phone.

## Completed work

### Phase 1 (Foundation) — see CHANGELOG.md
Gradle/AGP 9.5.0/Kotlin 2.4.20/Compose BOM 2026.09.00, MVVM packages, 5-tab bottom navigation, CI workflow.

### Phase 2 (UI and Dashboard)
- **Design system / theme:** `AsistenTheme(pengaturan, darkTheme)` builds light/dark Material 3 schemes from the chosen accent; text size scales via `LocalDensity.fontScale` (0.9 / 1.0 / 1.15); status/navigation bar icon contrast follows the in-app theme.
- **Settings (in memory):** `PengaturanViewModel` — theme mode (Ikuti sistem/Terang/Gelap), 4 accent colors, 3 text sizes, animation on/off. The animation toggle really switches the tab transitions between fade and none.
- **Profil screen:** controls for all of the above, plus an honest note that settings are not yet saved after the app closes.
- **Beranda dashboard:** time-of-day greeting, Hanako card with a lightweight static vector illustration (long orange hair, purple eyes, white flower, white/navy outfit), and 7 widget cards (Jadwal hari ini, Tugas dan prioritas, Info sekolah, Ringkasan sholat, Kemajuan kebiasaan, Ringkasan keuangan, Tujuan dan pengingat). Each card shows an honest "no data / feature not available yet" text — no fake data.
- **Widget customization:** "Atur widget" mode with move up/down, show/hide switch, **Urungkan** (undo, history capped at 20) and **Kembalikan standar**. Pure logic lives in `domain/dasbor/TataLetakWidget.kt`.
- **Unit tests:** `TataLetakWidgetTest` (9 tests) and `SapaanProviderTest` (4 tests).

## Deliberately NOT done in Phase 2 (and why)
- **Persistence** of settings and widget arrangement: roadmap puts DataStore/Room in Phase 3. Until then they reset when the app process ends. "Save arrangement / restore previous arrangements" is therefore only partly done (in-session undo + reset to default); permanent saving is a Phase 3 task.
- **Multiple Hanako expressions:** only one static expression exists. More pre-rendered expressions can be added when Hanako's chat UI is built (Phase 6).
- **Dashboard shortcuts to modules** (Keuangan, Doa, etc.) and the **Morning briefing / Life Overview** cards: the modules do not exist yet, so adding buttons would be non-functional. Add them as each module lands (Phases 4, 8, 9).
- Onboarding: Phase 3 (needs persistence for the preferred name).

## Current project structure (changes since Phase 1 marked *)
```
app/src/main/kotlin/id/asisten/app/
  MainActivity.kt *
  domain/Sapaan.kt
  domain/dasbor/TataLetakWidget.kt *        (WidgetBeranda, TataLetakWidget, RiwayatTataLetak)
  domain/pengaturan/Pengaturan.kt *         (ModeTema, Aksen, UkuranTeks, Pengaturan)
  ui/theme/Theme.kt *
  ui/pengaturan/PengaturanViewModel.kt *
  ui/navigation/{AsistenDestination,AsistenNavHost}.kt (NavHost *)
  ui/beranda/{BerandaViewModel *,BerandaScreen *,WidgetTeks *}.kt
  ui/profil/ProfilScreen.kt *
  ui/screens/BagianScreen.kt
app/src/main/res/drawable/hanako_ilustrasi.xml *
app/src/main/res/values/strings.xml *
app/src/test/kotlin/id/asisten/app/domain/{SapaanProviderTest,dasbor/TataLetakWidgetTest *}.kt
```

## Important technical decisions
1. (Phase 1) AGP 9.5.0 + compileSdk 37, built-in Kotlin, string routes, no DI framework, `material-icons-core` only, Room 2.8.5 vs Room 3.0.3 to be decided at Phase 3 start (see docs/DEPENDENCIES.md).
2. Pure logic (greeting, widget layout/undo) is kept in `domain/` with no Android/Compose types so it is unit-testable on the JVM.
3. Accent drives `primary`; navy remains `secondary`. Container colors are derived with `lerp` toward white/black.
4. Dark mode is resolved in `MainActivity` (needs `isSystemInDarkTheme()` for "Ikuti sistem") and passed to the theme.
5. Widget arrangement is an immutable value + history list, so persisting it in Phase 3 only needs a serializer (order + hidden set).

## Files changed in Phase 2
New: `domain/dasbor/TataLetakWidget.kt`, `domain/pengaturan/Pengaturan.kt`, `ui/pengaturan/PengaturanViewModel.kt`, `ui/profil/ProfilScreen.kt`, `ui/beranda/WidgetTeks.kt`, `res/drawable/hanako_ilustrasi.xml`, `test/.../dasbor/TataLetakWidgetTest.kt`.
Modified: `MainActivity.kt`, `ui/theme/Theme.kt`, `ui/navigation/AsistenNavHost.kt`, `ui/beranda/BerandaViewModel.kt`, `ui/beranda/BerandaScreen.kt`, `res/values/strings.xml`, docs.

## Tests performed and their REAL results
- **Gradle build / unit tests / lint: NOT RUN.** The authoring sandbox blocks `services.gradle.org`, `repo.maven.apache.org`, `dl.google.com` (proxy 403) and has no Android SDK. Earlier attempt: `./gradlew testDebugUnitTest` failed before compiling anything (could not download Gradle 9.6.0).
- **Static checks that WERE run** (Python script, not a compiler): all 9 XML resource files are well-formed; every `R.string.*`/`@string/*` reference exists in `strings.xml` and no string is unused; every referenced drawable/mipmap exists; brackets balance in all 15 Kotlin files; each file's `package` matches its directory. These checks cannot detect type errors, wrong imports, or API misuse.
- **No APK exists.**

## Known issues / risks (all unverified)
- First CI run may fail on compile errors I could not catch (imports, Compose API signatures, AGP 9 DSL). Most likely places: `SystemBarStyle.auto` lambda in `MainActivity`, `clickable(role=…)` in `ProfilScreen`, `Density(...)` in `Theme`, `LifecycleEventEffect` usage, NavHost transition lambdas.
- (resolved) unused `BorderStroke` import removed from `ProfilScreen.kt`.
- Animation toggle covers tab transitions only; other animations will honor the same setting when they are added.
- Settings and widget arrangement are lost when the process dies (by design until Phase 3).
- CI action versions (`@v4`) and SDK-license step were not verified for 2026.

## Exact next steps
1. Create a (private) GitHub repo; in Termux: `git remote add origin <url>` then `git push -u origin main`.
2. Actions tab → run **Build ASISTEN**. If it fails, paste the full failing log into the next session; fix compile errors first, no new features.
3. When green, install `asisten-debug-apk` on the realme Narzo 50i Prime and check: 5 tabs; greeting by time; Hanako card shows the illustration; Profil → theme/accent/text size/animation take effect immediately; Beranda → Atur widget → move/hide/undo/reset works; layout is not clipped at "Besar" text size.
4. Then start Phase 3 (Local Database): first decide Room 2.8.5 vs Room 3.0.3, add KSP, DataStore for settings, Room for profile/memory/widget layout, migrations, onboarding.

## Continuation prompt (paste into the next session)
```
Lanjutkan proyek ASISTEN dari HANDOFF.md terbaru (jangan mulai ulang, jangan minta saya mengulang persyaratan).
Status: kode Fase 1 dan Fase 2 sudah ditulis tetapi belum pernah dikompilasi.
Hasil GitHub Actions saya: <tempel log error, atau tulis "hijau, APK terpasang, semua pemeriksaan di langkah 3 berhasil" / sebutkan yang gagal>.
Jika ada error: perbaiki penyebabnya secara minimal dan jelaskan, tanpa menambah fitur baru.
Jika hijau: kerjakan Fase 3 (Local Database) sesuai master prompt, termasuk menyimpan pengaturan tampilan dan tata letak widget secara permanen, lalu perbarui PROGRESS.md, HANDOFF.md, CHANGELOG.md dengan hasil tes yang nyata.
```
