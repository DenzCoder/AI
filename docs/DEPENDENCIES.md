# Dependency verification log

Checked 2026-11-02 against official pages. "Used" = declared in `libs.versions.toml` now.

| Item | Version | Source | Used |
|---|---|---|---|
| Android Gradle Plugin | 9.5.0 (stable; 9.6 alpha exists) | developer.android.com AGP release notes | yes |
| Gradle | 9.6.0 (AGP 9.5.0 minimum/default) | AGP 9.5.0 release notes | yes (wrapper) |
| JDK | 17 minimum | AGP 9.5.0 release notes | CI uses 17 |
| SDK Build Tools / max API | 36.0.0 / 37 | AGP 9.5.0 release notes | auto |
| Kotlin | 2.4.20 (2.4.21 referenced by Kotlin docs) | JetBrains blog | yes |
| Compose BOM | 2026.09.00 (material3 1.4.0, foundation 1.12.1) | Compose BOM + mapping pages | yes |
| activity-compose | 1.13.0 (2026-03-11) | AndroidX activity releases | yes |
| navigation-compose | 2.10.2 (2026-09-23), maintenance mode | AndroidX navigation releases | yes |
| lifecycle-* | 2.11.0 (2026-06-17); needs AGP >= 9.2, compileSdk 37 | AndroidX lifecycle releases | yes |
| junit | 4.13.2 | long-standing final 4.x release (not re-checked online) | yes |
| Room 2.x | 2.8.5 (2026-09-09), chosen | AndroidX Room releases | yes |
| Room 3 | 3.0.3 (2026-09-09), `androidx.room3`, breaking redesign; NOT chosen (cannot be tried without a build) | AndroidX Room 3 releases | no |
| KSP | 2.3.10 (pairing in the Kotlin quickstart; 2.3.11 seen as a newer tag). AGP 9 built-in Kotlin: sources conflict, see HANDOFF decision 2 | kotlinlang.org, google/ksp issues #2615/#2729 | yes |
| datastore-preferences | 1.3.0 (2026-10-21) | AndroidX DataStore releases | yes |
| androidx.test runner / core / ext-junit | 1.7.0 / 1.7.0 / 1.3.0 | AndroidX Test releases | androidTest only |
| room-testing | 2.8.5 (same as Room) | AndroidX Room releases | androidTest only |

| work-runtime (WorkManager) | 2.12.0 (AndroidX all-channel versions page, checked 2026-11-04; its minimum compileSdk/AGP was NOT re-checked). Used only through the plain Java-style `Worker`/`PeriodicWorkRequest.Builder` API, no ktx artifact | developer.android.com androidx versions | yes (Phase 5) |

Alarm and notification permissions (Phase 5), as documented by Android: `SCHEDULE_EXACT_ALARM` is not needed on Android 11, can be denied on Android 12+ (and is off by default on newer releases), `POST_NOTIFICATIONS` exists only on Android 13+. The app does not rely on any default: it reads the real state at runtime (`CekIzin`), falls back to inexact alarms and tells the user on the Pengingat screen. `USE_EXACT_ALARM` is deliberately not declared (reserved for alarm-clock and calendar apps).

Not yet verified (check in their phase): Supabase client, Gemini access path, biometric, speech, ML Kit (OCR).
