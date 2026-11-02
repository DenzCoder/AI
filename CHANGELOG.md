# CHANGELOG

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
