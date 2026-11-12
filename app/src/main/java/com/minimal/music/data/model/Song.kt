package com.minimal.music.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val mediaUri: String,
    val artworkUri: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val popularityScore: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val folderName: String? = null,
    val folderPath: String? = null,
    val lyrics: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
