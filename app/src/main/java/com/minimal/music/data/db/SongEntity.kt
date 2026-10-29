package com.minimal.music.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minimal.music.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0L,
    @ColumnInfo(name = "media_uri") val mediaUri: String,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "play_count") val playCount: Int = 0,
    @ColumnInfo(name = "popularity_score") val popularityScore: Int = 0,
    @ColumnInfo(name = "date_added") val dateAdded: Long = System.currentTimeMillis()
) {
    fun toModel(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        mediaUri = mediaUri,
        artworkUri = artworkUri,
        isFavorite = isFavorite,
        playCount = playCount,
        popularityScore = popularityScore,
        dateAdded = dateAdded
    )

    companion object {
        fun fromModel(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            mediaUri = song.mediaUri,
            artworkUri = song.artworkUri,
            isFavorite = song.isFavorite,
            playCount = song.playCount,
            popularityScore = song.popularityScore,
            dateAdded = song.dateAdded
        )
    }
}
