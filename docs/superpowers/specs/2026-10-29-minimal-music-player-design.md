# Minimal Android Music Player - Design Specification

**Date:** 2026-10-29  
**Status:** Approved  
**Target Platform:** Android (API 26+, MinSDK 26, TargetSDK 34/35)  
**Language & UI:** Kotlin, Jetpack Compose, Material 3, AndroidX Media3 (ExoPlayer), Room Database  

---

## 1. Vision & Intent

The goal is to build an Android music player with a refined, ultra-minimal aesthetic. Rather than cluttering the screen with dense menus and noisy visuals, the player focuses on music discovery, effortless playlist management, and playback controls. It features bottom tab navigation, smart auto-generated playlists (*Most Played*, *Favorites*, *Most Popular*), and custom user playlists.

---

## 2. Core Functional Requirements

### 2.1 Navigation & Layout
- **Bottom Navigation Bar**:
  - **Discover / Home**: Hero card for quick resume, "Most Popular" trending songs, and listening highlights.
  - **Songs**: Alphabetical list of all available tracks with real-time search, duration indicators, and quick-action menu (Add to Playlist, Favorite, Play Next).
  - **Playlists**: Hub for Smart Playlists (*Favorites*, *Most Played*, *Recently Added*) and custom user-created playlists with thumbnail grids.
- **Docked Mini-Player**:
  - Floats persistently above the bottom navigation bar when music is active.
  - Displays current song title, artist, small album art / vinyl icon, play/pause toggle, and a progress line.
  - Tap expands into the full-screen **Now Playing** sheet.
- **Full-Screen Now Playing View**:
  - Minimalist vinyl/album cover artwork with subtle ambient glow.
  - Large track title and artist name.
  - Quick Favorite heart button.
  - Interactive scrub bar with current position and total duration timestamps.
  - Playback controls: Shuffle, Previous, Play/Pause, Next, Repeat mode (Off, All, One).
  - Current Queue preview button.

### 2.2 Features Detailed
1. **Most Popular Songs**:
   - Curated trending songs highlighted in the Discover tab and playable with one tap.
   - Populated from pre-seeded demo catalog and extensible remote audio feed.
2. **Favorite Function**:
   - One-tap toggle on any track in list view or Now Playing view.
   - Persisted in Room database (`isFavorite = true`).
   - Accessible from the "Favorites" smart playlist.
3. **Most Played Tracks**:
   - Play counter automatically incremented in Room DB whenever a track finishes or plays past 30 seconds.
   - Dynamic smart playlist query: `SELECT * FROM songs ORDER BY playCount DESC LIMIT 50`.
4. **Custom Playlist System**:
   - Create new playlists with custom titles.
   - Add/remove songs to/from playlists via Room many-to-many relationship (`PlaylistSongCrossRef`).
   - Rename and delete playlists.
5. **Audio Engine & Playback Service**:
   - Powered by `androidx.media3:media3-exoplayer` and `androidx.media3:media3-session`.
   - Foreground playback service with notification controls (play/pause/skip) and lockscreen metadata.
   - MediaStore audio scanner for local device MP3/M4A/FLAC/WAV files + pre-seeded royalty-free music assets for instant out-of-the-box playback on emulators and new devices.

---

## 3. Visual & Aesthetic Guidelines (Minimalism)
- **Palette**: Deep charcoal/pitch black surfaces (`#0D0D11`, `#16161E`), muted slate secondary surfaces (`#22222E`), crisp white/light-gray typography (`#F5F5F7`, `#A0A0B0`), and a single vibrant accent color (`#7C4DFF` or `#00E5FF`).
- **Typography**: Clean sans-serif hierarchy with comfortable spacing and readable durations.
- **Micro-interactions**: Smooth transitions between mini-player and expanded player, bouncy heart toggle animation, smooth progress bar dragging.

---

## 4. Architecture & Technical Choices

### 4.1 Tech Stack
- **Architecture**: MVVM with Unidirectional Data Flow (UDF), Kotlin Coroutines, and StateFlow.
- **UI**: 100% Jetpack Compose with Material 3.
- **Database**: Room Database (`SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRef`).
- **Audio Playback**: AndroidX Media3 (`MediaController` connected to `MediaSessionService`).
- **Build System**: Gradle 8.14+ with Android Gradle Plugin 8.7+ and Kotlin 2.0+.

### 4.2 Trade-offs Considered
1. **Local MediaStore vs. Remote Streaming**:
   - *Choice:* Hybrid approach. Local MediaStore scanner accesses user files on phone storage, while pre-bundled/streamable demo tracks ensure the app is immediately working and delightful without requiring existing local MP3s.
2. **MediaPlayer vs. Media3 ExoPlayer**:
   - *Choice:* AndroidX Media3 is Google's modern standard, handling audio focus, headset disconnects, notification channels, and background execution seamlessly.
