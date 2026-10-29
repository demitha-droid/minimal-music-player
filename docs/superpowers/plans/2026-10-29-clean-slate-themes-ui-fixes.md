# Clean Slate, AMOLED & Light Themes, and UI Overlap Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a clean slate by clearing all auto-added folders, strictly enforce manual-only folder management, implement a 3-way theme system (Obsidian Dark, Pure Black AMOLED, Minimal Light) with persistent storage and UI toggle, and eliminate all UI overlapping bugs across the application.

**Architecture:** 
1. Database & Repository: Strip all auto-registration logic from `MusicRepository` and clear `added_folders` table so only user-selected directories exist.
2. Theme System: Implement `ThemeMode` (`DARK`, `AMOLED`, `LIGHT`) with `CompositionLocalProvider(LocalCustomColors)`, dynamic Compose color getters in `Color.kt`, and `SharedPreferences` persistence in `MusicViewModel`.
3. Layout & Insets: Correct `navigationBarsPadding()` modifier ordering in `BottomNavBar`, add width bounds to badge chips in `SongListItem`, make `NowPlayingSheet` vinyl responsive with scroll fallback, and normalize screen bottom padding.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose, Material 3, Room 2.7.0, Media3 ExoPlayer, Coroutines Flow.

---

### Task 1: Clean Slate & Manual-Only Folders Architecture

- [ ] Add `clearAllFolders()` to `AddedFolderDao.kt` and `MusicRepository.kt`.
- [ ] Remove auto-folder insertion loops from `initializeDefaultDataIfNeeded()`, `scanAndSyncDeviceAudio()`, and `addCustomSongs()` in `MusicRepository.kt`.
- [ ] In `MusicRepository.kt` initialization / startup, execute `clearAllFolders()` once to purge stale auto-discovered folders and establish the clean slate.
- [ ] Ensure `addFolderToLibrary(folder: File)` and `importFromTreeUri(treeUri: Uri)` remain the ONLY paths that create `AddedFolderEntity`.
- [ ] Verify compilation and test suite with `.\gradlew.bat testDebugUnitTest`.

---

### Task 2: Multi-Theme Engine (Obsidian Dark, Pure Black AMOLED, Minimal Light)

- [ ] Create `ThemeMode.kt` defining enum `DARK`, `AMOLED`, `LIGHT`.
- [ ] In `Color.kt`:
  - Define `CustomColors` data class with fields: `background`, `surface`, `surfaceElevated`, `surfaceBorder`, `primary`, `primaryAccent`, `secondary`, `textPrimary`, `textSecondary`, `textMuted`, `heartActive`, `equalizerGreen`, `glassSurface`, `isLight`.
  - Define `DarkColors` (Obsidian: `#0C0C0E`), `AmoledColors` (Pure Black: `#000000`), and `LightColors` (Crisp Slate/White: `#F8F9FA` / `#FFFFFF`).
  - Provide dynamic composable color getters (`BgDark`, `SurfaceDark`, `SurfaceElevated`, `SurfaceBorder`, `TextPrimary`, `TextSecondary`, `TextMuted`, `GlassSurface`).
- [ ] In `Theme.kt`:
  - Define `DarkColorScheme`, `AmoledColorScheme`, and `LightColorScheme`.
  - Update `MinimalMusicTheme` to accept `themeMode: ThemeMode` and provide `LocalCustomColors`.
- [ ] In `MusicViewModel.kt`:
  - Add `themeMode: StateFlow<ThemeMode>` and `setThemeMode(mode: ThemeMode)` with `SharedPreferences` persistence.
- [ ] In `MainActivity.kt`:
  - Collect `themeMode` from `viewModel` and pass it to `MinimalMusicTheme`.
- [ ] Add a Theme Switcher button and dropdown menu to `DiscoverScreen.kt` and `SongsScreen.kt`.

---

### Task 3: Fix Overlapping UI Elements & Layout Inset Squishing

- [ ] `BottomNavBar.kt`:
  - Fix modifier ordering: place `navigationBarsPadding()` on the outer `Surface`/container and keep the inner `Row` at a guaranteed `64.dp` height so icons and labels are never squished by system navigation bars.
- [ ] `SongListItem.kt`:
  - Add `.widthIn(max = 110.dp)` and `TextOverflow.Ellipsis` on the folder badge row so long folder names never push `song.artist` off the right edge.
- [ ] `NowPlayingSheet.kt`:
  - Replace rigid `280.dp` vinyl container with `.sizeIn(maxWidth = 260.dp, maxHeight = 260.dp).aspectRatio(1f)`.
  - Add `Modifier.verticalScroll(rememberScrollState())` to the content column to guarantee zero clipping or overlap on compact screens.
- [ ] `SongsScreen.kt` `FolderGalleryCard`:
  - Optimize `CompactArtworkActionRow` button text ("Keep" and "Retry") with compact icon sizing so pill buttons never wrap or truncate.
- [ ] Remove redundant `120.dp` bottom content padding across `DiscoverScreen.kt`, `SongsScreen.kt`, `FoldersScreen.kt`, and `PlaylistsScreen.kt`.

---

### Task 4: Verification, APK Build, and Git Commit

- [ ] Run `.\gradlew.bat testDebugUnitTest` to ensure all tests pass.
- [ ] Run `.\gradlew.bat assembleDebug` to compile and trigger `copyApkToRoot`.
- [ ] Confirm `app-debug.apk` is updated in the project root directory.
- [ ] Commit all changes to git with a clear descriptive message.
