package com.minimal.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref r ON s.id = r.song_id
        WHERE r.playlist_id = :playlistId
        ORDER BY r.added_at DESC
    """)
    fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlist_id = :playlistId")
    fun getSongCountForPlaylistFlow(playlistId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlist_id = :playlistId")
    suspend fun getSongCountForPlaylist(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long): Int

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef): Long

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): Int
}
