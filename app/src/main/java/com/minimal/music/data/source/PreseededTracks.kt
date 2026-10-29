package com.minimal.music.data.source

import com.minimal.music.data.model.Song

object PreseededTracks {
    val tracks: List<Song> = listOf(
        Song(
            id = 1001L,
            title = "Midnight Echoes",
            artist = "Aether",
            album = "Nocturne Chronicles",
            durationMs = 212000L,
            mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            artworkUri = "linear-gradient:45:#1A1A24:#4A306D",
            isFavorite = true,
            playCount = 42,
            popularityScore = 98,
            folderName = "Featured Music",
            folderPath = "FEATURED_MUSIC"
        ),
        Song(
            id = 1002L,
            title = "Solar Drift",
            artist = "Komorebi",
            album = "Horizon Light",
            durationMs = 184000L,
            mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            artworkUri = "linear-gradient:45:#182026:#2A4B5E",
            isFavorite = false,
            playCount = 37,
            popularityScore = 95,
            folderName = "Featured Music",
            folderPath = "FEATURED_MUSIC"
        ),
        Song(
            id = 1003L,
            title = "Velvet Skyline",
            artist = "Sora & Mira",
            album = "Dusk Sessions",
            durationMs = 238000L,
            mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            artworkUri = "linear-gradient:45:#21141E:#5E2442",
            isFavorite = true,
            playCount = 29,
            popularityScore = 91,
            folderName = "Featured Music",
            folderPath = "FEATURED_MUSIC"
        ),
        Song(
            id = 1004L,
            title = "Neon Mirage",
            artist = "CyberPulse",
            album = "Synthetic Dreams",
            durationMs = 196000L,
            mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            artworkUri = "linear-gradient:45:#111B24:#1C5870",
            isFavorite = false,
            playCount = 21,
            popularityScore = 88,
            folderName = "Featured Music",
            folderPath = "FEATURED_MUSIC"
        ),
        Song(
            id = 1005L,
            title = "Cerulean Waves",
            artist = "Deep Cascade",
            album = "Submerged",
            durationMs = 245000L,
            mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            artworkUri = "linear-gradient:45:#0F1D1A:#1E4B3E",
            isFavorite = false,
            playCount = 18,
            popularityScore = 84,
            folderName = "Featured Music",
            folderPath = "FEATURED_MUSIC"
        ),
        Song(
            id = 1006L,
            title = "Golden Hour Whispers",
            artist = "Solstice",
            album = "Quiet Glow",
            durationMs = 168000L,
            mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            artworkUri = "linear-gradient:45:#261E14:#6B4E26",
            isFavorite = true,
            playCount = 15,
            popularityScore = 80,
            folderName = "Featured Music",
            folderPath = "FEATURED_MUSIC"
        ),
        Song(
            id = 1007L,
            title = "Astral Blossom",
            artist = "Nebula Garden",
            album = "Cosmic Flora",
            durationMs = 221000L,
            mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            artworkUri = "linear-gradient:45:#221626:#5A2B6D",
            isFavorite = false,
            playCount = 11,
            popularityScore = 77,
            folderName = "Featured Music",
            folderPath = "FEATURED_MUSIC"
        ),
        Song(
            id = 1008L,
            title = "Urban Raindrops",
            artist = "Lo-Fi Beats Collective",
            album = "Windowpanes",
            durationMs = 175000L,
            mediaUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            artworkUri = "linear-gradient:45:#16191D:#323C47",
            isFavorite = false,
            playCount = 8,
            popularityScore = 73,
            folderName = "Featured Music",
            folderPath = "FEATURED_MUSIC"
        )
    )
}
