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
| Room 2.x | 2.8.5 (2026-09-09) | AndroidX Room releases | Phase 3 |
| Room 3 | 3.0.3 (2026-09-09), `androidx.room3`, breaking | AndroidX Room 3 releases | Phase 3 (decide) |
| KSP | 2.3.11 seen as latest tag; quickstart uses 2.3.10 | google/ksp releases, kotlinlang.org | Phase 3 |

Not yet verified (check in their phase): DataStore, WorkManager, Supabase client, Gemini access path, biometric, speech.
