package com.minimal.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("""
        SELECT * FROM songs 
        ORDER BY 
            CASE WHEN folder_path IS NOT NULL AND folder_path != '' THEN 0 ELSE 1 END,
            CASE WHEN folder_name IS NOT NULL THEN folder_name ELSE '' END COLLATE NOCASE ASC,
            title COLLATE NOCASE ASC
    """)
    fun getAllSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavoritesFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE play_count > 0 ORDER BY play_count DESC, popularity_score DESC LIMIT :limit")
    fun getMostPlayedFlow(limit: Int = 50): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY popularity_score DESC, play_count DESC LIMIT :limit")
    fun getMostPopularFlow(limit: Int = 30): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%'
           OR folder_name LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN folder_path IS NOT NULL AND folder_path != '' THEN 0 ELSE 1 END,
            title COLLATE NOCASE ASC
    """)
    fun searchSongsFlow(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folder_path = :folderPath ORDER BY title COLLATE NOCASE ASC")
    fun getSongsByFolderPathFlow(folderPath: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE folder_path = :folderPath ORDER BY title COLLATE NOCASE ASC")
    suspend fun getSongsByFolderPath(folderPath: String): List<SongEntity>

    @Query("DELETE FROM songs WHERE folder_path = :folderPath")
    suspend fun deleteSongsByFolderPath(folderPath: String): Int

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsList(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>): List<Long>

    @Update
    suspend fun updateSong(song: SongEntity): Int

    @Query("UPDATE songs SET is_favorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: Long, isFavorite: Boolean): Int

    @Query("UPDATE songs SET play_count = play_count + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long): Int

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("DELETE FROM songs WHERE media_uri LIKE '%soundhelix%' OR folder_path = 'FEATURED_MUSIC' OR id BETWEEN 1001 AND 1010")
    suspend fun deleteFillerSongs(): Int

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>): Int

    @Query("UPDATE songs SET artwork_uri = :artworkUri WHERE id = :songId")
    suspend fun updateSongArtwork(songId: Long, artworkUri: String): Int

    @Query("SELECT * FROM songs WHERE artwork_uri IS NULL OR artwork_uri = '' OR artwork_uri LIKE 'content://%'")
    suspend fun getSongsNeedingArtwork(): List<SongEntity>
}
