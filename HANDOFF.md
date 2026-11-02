# HANDOFF — after Phase 1 (Foundation)

Last updated: 2026-11-02

## Status in one line
Phase 1 source code is written, but **the build and unit tests have NOT been run**. Phase 1 is therefore NOT verified and Phase 2 must not start until the GitHub Actions build is green.

## Completed work
- Gradle project (Kotlin DSL, version catalog, Gradle wrapper 9.6.0), module `:app`.
- AGP 9.5.0 with built-in Kotlin (no `org.jetbrains.kotlin.android` plugin), Kotlin 2.4.20, Compose BOM 2026.09.00, Material 3.
- Package `id.asisten.app` with `domain/` (pure logic), `ui/` (theme, navigation, screens, beranda). MVVM: `BerandaViewModel` exposes a `StateFlow<Sapaan>`.
- Bottom navigation with the 5 required tabs: Beranda, Jadwal, Aktivitas, AI Hanako, Profil (string routes, state saved/restored).
- Basic theme following system light/dark. Edge-to-edge enabled. Adaptive launcher icon (simple flower mark).
- All user-facing text lives in `res/values/strings.xml` in Indonesian.
- 4 unit tests for the greeting logic (`SapaanProviderTest`).
- GitHub Actions workflow `.github/workflows/build.yml`: unit tests, `assembleDebug`, uploads debug APK + test reports.
- Manifest requests no permissions; `allowBackup=false` (secure default).

## Current project structure
```
asisten/
  settings.gradle.kts  build.gradle.kts  gradle.properties
  gradle/libs.versions.toml  gradle/wrapper/*  gradlew  gradlew.bat
  .github/workflows/build.yml
  app/build.gradle.kts  app/proguard-rules.pro
  app/src/main/AndroidManifest.xml
  app/src/main/kotlin/id/asisten/app/
    MainActivity.kt
    domain/Sapaan.kt
    ui/theme/Theme.kt
    ui/navigation/{AsistenDestination,AsistenNavHost}.kt
    ui/beranda/{BerandaViewModel,BerandaScreen}.kt
    ui/screens/BagianScreen.kt
  app/src/main/res/{values,values-night,drawable,mipmap-anydpi}
  app/src/test/kotlin/id/asisten/app/domain/SapaanProviderTest.kt
  docs/DEPENDENCIES.md  PROGRESS.md  HANDOFF.md  CHANGELOG.md
```

## Important technical decisions
1. **AGP 9.5.0 + compileSdk 37.** `lifecycle 2.11.0` requires AGP >= 9.2.0 and compiles Compose against API 37, so older AGP would force pinning old libraries. AGP 9.5.0 needs Gradle 9.6.0 and JDK 17+ (CI uses JDK 17). `targetSdk` is 36 (conservative behavior-change surface); `minSdk` is 30.
2. **Built-in Kotlin.** AGP 9 ships Kotlin support; only `org.jetbrains.kotlin.plugin.compose` is applied.
3. **String routes** instead of type-safe Navigation, to avoid adding `kotlinx-serialization` (minimal dependencies). Navigation 2.10.2 is in maintenance mode; Navigation 3 can be reconsidered later but is not required.
4. **No DI framework, no Application class yet.** Added only when a phase needs them (no empty placeholders).
5. **Icons:** `material-icons-core` only (no `-extended`, which is very large).
6. **Room (Phase 3) decision pending:** Room 2.8.5 (`androidx.room`, KSP/KAPT) is stable; Room 3.0.3 (`androidx.room3`) is also stable but is a breaking redesign (new package, `SQLiteDriver` required, all DAO functions `suspend`/`Flow`, KSP-only). Recommendation: decide at the start of Phase 3 after Phase 1 CI is green. Room 3 suits a new project; Room 2.8.5 is the lower-risk, more documented option.
7. Build runs only in the cloud (GitHub Actions), matching the project's constraints.

## Files changed
All files are new (initial commit). See structure above.

## Tests performed and their REAL results
- Environment inspection: JDK 21 present, Gradle 8.14.3 present, **no Android SDK**.
- Network from this session: `repo.maven.apache.org`, `dl.google.com` and `services.gradle.org` are denied by the session proxy (HTTP 403). This cannot be bypassed.
- `./gradlew testDebugUnitTest` attempted: **FAILED before any compilation** — it could not download `gradle-9.6.0-bin.zip` (proxy 403). No source was compiled and no test was run.
- Gradle wrapper files were generated offline (`gradle wrapper --gradle-version 9.6.0`); that task succeeded.
- **No APK exists.** None has been generated.

## Known issues / risks (unverified because nothing was compiled)
- Never compiled: first CI run may reveal errors in Gradle DSL (AGP 9 changes), imports, or dependency resolution.
- Compose BOM 2026.09.00 maps material3 to 1.4.0 per the official mapping page; other mappings were not read.
- `kotlin` 2.4.20 was confirmed released (JetBrains blog); `ksp` is not used yet (latest seen: 2.3.11).
- CI SDK-license step assumes `$ANDROID_HOME/cmdline-tools/latest` exists on `ubuntu-latest`; AGP may need `platforms;android-37` auto-download.
- Action versions (`@v4`) were not re-verified for 2026; newer majors may exist.
- Hanako visuals, onboarding, and all data features are intentionally absent (later phases).

## Exact next steps
1. Create an empty GitHub repository (private recommended).
2. In Termux, from the project folder: `git remote add origin <url>` then `git push -u origin main`. (The project is already committed locally on branch `main`.)
3. Open the repo's **Actions** tab and run **Build ASISTEN**. Copy the full log of any failure and paste it into the next session.
4. When green: download the `asisten-debug-apk` artifact, install it on the realme Narzo 50i Prime, and confirm the 5 tabs navigate correctly and the greeting matches the time of day.
5. Only then start Phase 2.

## Continuation prompt (paste into the next session)
```
Lanjutkan proyek ASISTEN dari HANDOFF.md terbaru (jangan mulai ulang, jangan minta saya mengulang persyaratan).
Status: Fase 1 kodenya sudah ditulis tetapi build belum terverifikasi.
Hasil GitHub Actions saya: <tempel log atau tulis "hijau, APK sudah terpasang dan 5 tab berfungsi">.
Jika ada error: perbaiki sumbernya secara minimal, jelaskan penyebabnya, dan jangan menulis fitur Fase 2 dulu.
Jika hijau: kerjakan Fase 2 (UI dan Dashboard) sesuai master prompt, lalu perbarui PROGRESS.md, HANDOFF.md, CHANGELOG.md dengan hasil tes yang nyata.
```
