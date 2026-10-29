package com.minimal.music.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minimal.music.data.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    fun toModel(songCount: Int = 0): Playlist = Playlist(
        id = id,
        name = name,
        createdAt = createdAt,
        songCount = songCount
    )
}
