# 🎵 Minimal Music Player

A sleek, modern, distraction-free offline music player built for Android using Jetpack Compose, Material 3, and AndroidX Media3 (ExoPlayer).

---

## ✨ Features

- **🎤 Synced Lyrics Support (LRC)**:
  - Real-time active line highlighting synchronized with vocal attack onset.
  - Tap any lyric line to jump playback directly to that timestamp.
  - Multi-source auto-downloader (LRCLIB API, Lyrics.ovh API, and local `.lrc` / `.txt` files alongside your audio).
  - Fully cached offline so lyrics load instantly without internet.
  - "Open Lyrics by Default" preference option in Settings.

- **🎨 Cover Artwork & YouTube Thumbnails**:
  - High-res YouTube thumbnail fetcher for local songs with clean title/artist heuristics.
  - Batch download feature to auto-fetch covers for your entire offline library.
  - Elegant rotating vinyl record artwork view with animated spindle and disc grooving.

- **📁 Explorer Tab & Folder Navigation**:
  - Integrated local storage and directory explorer.
  - Import folders using Android Storage Access Framework (SAF) with persistable URI permissions.
  - Dedicated Folder Gallery cards with one-tap Play and Shuffle buttons.
  - Zero duplicates: built-in database deduplication engine.

- **🎛️ Playback & Experience**:
  - Smooth horizontal swipe gesture on Now Playing to skip tracks (Swipe Left &rarr; Next, Swipe Right &rarr; Previous).
  - Sleep timer with quick presets (15m, 30m, 45m, 60m) or "End of Song" stop.
  - Persistent mini-player bar with swipe-to-dismiss and resume.
  - Custom playlists and smart playlists (*Favorites*, *Most Played*, *Most Popular*).

- **🌗 Minimalist Themes**:
  - Three distinct themes: **Dark**, **AMOLED Pitch Black**, and **Light**.
  - Flat monochrome design language with zero unnecessary clutter.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Compose BOM `2024.09.00`) with Material 3.
- **Audio Engine**: [AndroidX Media3 (ExoPlayer)](https://developer.android.com/media/media3) `1.4.1`.
- **Database**: [Room Database](https://developer.android.com/training/data-storage/room) `2.7.0` with incremental SQLite migrations.
- **Asynchronous / Concurrency**: Kotlin Coroutines & `StateFlow` / `SharedFlow`.
- **Annotation Processing**: [Google KSP](https://github.com/google/ksp) (Kotlin Symbol Processing).
- **Target Platform**: Android 8.0 (API 26) through Android 15 / 16 (API 36).

---

## 📦 Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK (API 36, Build-Tools 36.0.0+)

### Commands
```bash
# Clone the repository
git clone https://github.com/demitha-droid/minimal-music-player.git
cd minimal-music-player

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```
The compiled APK will be output to:
- `app/build/outputs/apk/debug/app-debug.apk`
- Root directory: `app-debug.apk` (via automatic `copyApkToRoot` task)

---

## 📄 License
This project is open-source under the Apache License 2.0.
