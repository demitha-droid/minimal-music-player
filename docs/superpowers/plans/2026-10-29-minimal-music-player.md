# Minimal Android Music Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete, minimal Android music player app with bottom tab navigation, featuring Most Popular songs, Favorites, Most Played tracking, custom Playlists, and seamless background playback using Media3 ExoPlayer and Jetpack Compose.

**Architecture:** MVVM architecture with Unidirectional Data Flow (UDF). An AndroidX `MediaSessionService` hosts `ExoPlayer` for reliable background audio, controlled by a shared `PlaybackManager`. Data is persisted in a local Room database with pre-seeded demo tracks and dynamic MediaStore indexing.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, AndroidX Media3 (ExoPlayer + MediaSession), Room Database, Coroutines, StateFlow, Gradle Kotlin DSL.

**Spec:** `docs/superpowers/specs/2026-10-29-minimal-music-player-design.md`

## Global Constraints
- Target platform: Android API 26+ (minSdk 26, targetSdk 35, compileSdk 35).
- UI: 100% Jetpack Compose with Material 3; dark minimal aesthetic.
- Audio engine: AndroidX Media3 ExoPlayer (`1.4.1` or latest stable).
- Storage/Persistence: Room (`2.6.1`), SQLite.
- Background playback: `MediaSessionService` with foreground service notification.
- Zero crashes when local device storage has no audio files; fallback to bundled/streamable demo tracks.

## Review Focus
1. Empty or restricted storage permissions: App must gracefully prompt for permissions and fall back to demo tracks without crashing.
2. Play count tracking: Must record playback accurately (increment play count once per track completion or > 30s playback).
3. Foreground service lifecycle: Audio playback must continue uninterrupted when the app is backgrounded or the screen is turned off.
4. Playlist management: Adding, removing, reordering, and deleting playlists must maintain database integrity.
5. Bottom navigation + Mini-player coexistence: Mini-player must dock cleanly above bottom navigation without obscuring screen content.

---

### Task 1: Android Project Scaffolding & Gradle Build Configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `gradle/wrapper/gradle-wrapper.properties`

**Interfaces:**
- Produces: Runnable Gradle project buildable via `./gradlew` or system Gradle.

- [ ] **Step 1: Create root Gradle configuration files**
  Define `settings.gradle.kts`, `build.gradle.kts`, and `gradle.properties` with Kotlin 2.0.21 and Android Gradle Plugin 8.7.3.
- [ ] **Step 2: Create `app/build.gradle.kts` with required dependencies**
  Configure Compose BOM, Material 3, Media3 ExoPlayer/Session, Room DB with KSP, Lifecycle ViewModel, and Navigation Compose.
- [ ] **Step 3: Create `app/src/main/AndroidManifest.xml`**
  Declare `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `WAKE_LOCK`, and register `MainActivity` and `PlaybackService`.
- [ ] **Step 4: Verify Gradle sync/build verification**
  Run Gradle dry-run or wrapper setup to confirm project structure is recognized.

---

### Task 2: Room Database & Data Models

**Files:**
- Create: `app/src/main/java/com/minimal/music/data/model/Song.kt`
- Create: `app/src/main/java/com/minimal/music/data/model/Playlist.kt`
- Create: `app/src/main/java/com/minimal/music/data/db/SongEntity.kt`
- Create: `app/src/main/java/com/minimal/music/data/db/PlaylistEntity.kt`
- Create: `app/src/main/java/com/minimal/music/data/db/PlaylistSongCrossRef.kt`
- Create: `app/src/main/java/com/minimal/music/data/db/SongDao.kt`
- Create: `app/src/main/java/com/minimal/music/data/db/PlaylistDao.kt`
- Create: `app/src/main/java/com/minimal/music/data/db/MusicDatabase.kt`
- Create: `app/src/test/java/com/minimal/music/data/db/SongDaoTest.kt`

**Interfaces:**
- Produces: `SongDao` (methods: `getAllSongsFlow()`, `getFavoritesFlow()`, `getMostPlayedFlow()`, `getMostPopularFlow()`, `incrementPlayCount()`, `setFavorite()`), `PlaylistDao` (CRUD for playlists and playlist tracks).

- [ ] **Step 1: Write `SongEntity`, `PlaylistEntity`, and `PlaylistSongCrossRef` models**
  Define tables with primary keys, indexes, and relationship types.
- [ ] **Step 2: Define `SongDao` and `PlaylistDao` interfaces**
  Include queries for songs, favorites, most played (`ORDER BY play_count DESC`), and popular songs (`ORDER BY popularity_score DESC`).
- [ ] **Step 3: Define `MusicDatabase` RoomDatabase class**
  Configure Room database instance with singleton pattern.
- [ ] **Step 4: Write unit test for DAO queries and model mappings**
  Verify query sorting and play count increment logic.

---

### Task 3: Music Repository & Audio Scanner

**Files:**
- Create: `app/src/main/java/com/minimal/music/data/source/PreseededTracks.kt`
- Create: `app/src/main/java/com/minimal/music/data/source/MediaStoreScanner.kt`
- Create: `app/src/main/java/com/minimal/music/data/repository/MusicRepository.kt`
- Create: `app/src/test/java/com/minimal/music/data/repository/MusicRepositoryTest.kt`

**Interfaces:**
- Consumes: `SongDao`, `PlaylistDao`, `Context`.
- Produces: `MusicRepository` methods: `getAllSongs()`, `getFavorites()`, `getMostPlayed()`, `getMostPopular()`, `getPlaylists()`, `createPlaylist()`, `addSongToPlaylist()`, `toggleFavorite()`, `recordSongPlayed()`, `syncMedia()`.

- [ ] **Step 1: Implement `PreseededTracks`**
  Provide 8-10 high quality royalty-free demo tracks with titles, artists, durations, artwork colors, popularity scores, and valid audio stream URLs.
- [ ] **Step 2: Implement `MediaStoreScanner`**
  Query Android's `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` for audio files, filtering out ringtones/notifications, extracting ID, title, artist, album, duration, and content URI.
- [ ] **Step 3: Implement `MusicRepository`**
  Orchestrate database persistence, pre-seeding default tracks on initial app run, syncing local device media, updating play counts, and managing playlists.
- [ ] **Step 4: Write unit tests for `MusicRepository`**
  Verify repository emits correct flows for favorites, most played, and custom playlists.

---

### Task 4: Background Playback Engine (Media3 Service & Controller)

**Files:**
- Create: `app/src/main/java/com/minimal/music/player/PlaybackService.kt`
- Create: `app/src/main/java/com/minimal/music/player/PlaybackState.kt`
- Create: `app/src/main/java/com/minimal/music/player/PlaybackManager.kt`

**Interfaces:**
- Consumes: `Context`, `Song`, `MusicRepository`.
- Produces: `PlaybackManager` exposing `playbackState: StateFlow<PlaybackState>`, `playSong(song, queue)`, `togglePlayPause()`, `seekTo(ms)`, `skipNext()`, `skipPrevious()`, `setShuffle(Boolean)`, `setRepeatMode(RepeatMode)`.

- [ ] **Step 1: Define `PlaybackState` and `RepeatMode`**
  Store `currentSong: Song?`, `isPlaying: Boolean`, `currentPositionMs: Long`, `durationMs: Long`, `isShuffle: Boolean`, `repeatMode: RepeatMode`, `queue: List<Song>`.
- [ ] **Step 2: Implement `PlaybackService` extending `MediaSessionService`**
  Initialize `ExoPlayer`, configure notification provider for playback controls (play/pause/skip), handle `MediaSession.Callback`, and audio focus.
- [ ] **Step 3: Implement `PlaybackManager`**
  Provide clean API for ViewModels to observe playback state and issue control commands. Automatically invoke `MusicRepository.recordSongPlayed(songId)` upon significant playback.

---

### Task 5: Minimal UI Design System & Theme

**Files:**
- Create: `app/src/main/java/com/minimal/music/ui/theme/Color.kt`
- Create: `app/src/main/java/com/minimal/music/ui/theme/Type.kt`
- Create: `app/src/main/java/com/minimal/music/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/minimal/music/ui/components/MinimalComponents.kt`
- Create: `app/src/main/java/com/minimal/music/ui/components/SongListItem.kt`
- Create: `app/src/main/java/com/minimal/music/ui/components/MiniPlayerBar.kt`

**Interfaces:**
- Produces: Minimalist design tokens, `MinimalMusicTheme`, `SongListItem`, `MiniPlayerBar`, `MinimalSearchBar`, `EmptyStateView`.

- [ ] **Step 1: Define minimal color palette and typography in `Color.kt` and `Type.kt`**
  Matte black surfaces (`#0C0C0E`), sleek elevated card surfaces (`#18181D`), crisp white text, and electric violet accent (`#8A56FF`).
- [ ] **Step 2: Implement `SongListItem`**
  Minimalist row displaying track title, artist, duration, playback indicator, favorite heart button, and optional play count badge.
- [ ] **Step 3: Implement `MiniPlayerBar`**
  Compact floating bar docked above bottom navigation with smooth progress line, title/artist, play/pause toggle, and skip next.

---

### Task 6: Bottom Navigation & Core Feature Screens

**Files:**
- Create: `app/src/main/java/com/minimal/music/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/minimal/music/ui/navigation/BottomNavBar.kt`
- Create: `app/src/main/java/com/minimal/music/ui/viewmodel/MusicViewModel.kt`
- Create: `app/src/main/java/com/minimal/music/ui/screens/DiscoverScreen.kt`
- Create: `app/src/main/java/com/minimal/music/ui/screens/SongsScreen.kt`
- Create: `app/src/main/java/com/minimal/music/ui/screens/PlaylistsScreen.kt`
- Create: `app/src/main/java/com/minimal/music/ui/screens/PlaylistDetailScreen.kt`

**Interfaces:**
- Consumes: `MusicViewModel`, `PlaybackManager`, `MusicRepository`.
- Produces: Navigation scaffold with bottom tabs: Discover, Songs, Playlists.

- [ ] **Step 1: Implement `MusicViewModel`**
  Expose state for all songs, favorites, most played, most popular songs, playlists, active search query, and UI events.
- [ ] **Step 2: Implement `DiscoverScreen`**
  Showcase hero resume card, "Most Popular" trending carousel/grid, and quick-play popular songs with play count highlights.
- [ ] **Step 3: Implement `SongsScreen`**
  Full searchable songs list with real-time search filtering, sort by title/artist/duration, and instant play on tap.
- [ ] **Step 4: Implement `PlaylistsScreen` & `PlaylistDetailScreen`**
  Display smart playlists (*Favorites*, *Most Played*, *Recently Added*) and custom playlists with a "Create Playlist" dialog and track management.

---

### Task 7: Full-Screen Now Playing View & Main Screen Integration

**Files:**
- Create: `app/src/main/java/com/minimal/music/ui/screens/NowPlayingSheet.kt`
- Create: `app/src/main/java/com/minimal/music/ui/MainScreen.kt`
- Create: `app/src/main/java/com/minimal/music/MainActivity.kt`

**Interfaces:**
- Consumes: `MusicViewModel`, `PlaybackManager`.
- Produces: Complete interactive app entry point with bottom sheet for Now Playing.

- [ ] **Step 1: Implement `NowPlayingSheet`**
  Full-screen sheet with animated vinyl/artwork, interactive scrubber seek bar, heart toggle, shuffle, repeat, previous/play/pause/next controls, and collapsible queue view.
- [ ] **Step 2: Build `MainScreen` scaffold**
  Compose bottom bar, content pages, mini-player, and expandable now playing sheet.
- [ ] **Step 3: Connect `MainActivity`**
  Handle runtime storage/audio permissions, initialize repository and playback service, and set Compose content.

---

### Task 8: Verification, Test Execution & APK Build

**Files:**
- Verify: Gradle project build output
- Target: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: Execute unit tests**
  Run `./gradlew testDebugUnitTest` and ensure all DAO, repository, and logic tests pass.
- [ ] **Step 2: Build the debug APK**
  Run `./gradlew assembleDebug` and verify successful compilation without warnings or errors.
- [ ] **Step 3: Inspect build artifacts**
  Confirm APK exists and is ready for installation/deployment.
