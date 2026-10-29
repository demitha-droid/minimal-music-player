package com.minimal.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavoritesFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE play_count > 0 ORDER BY play_count DESC, popularity_score DESC LIMIT :limit")
    fun getMostPlayedFlow(limit: Int = 50): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY popularity_score DESC, play_count DESC LIMIT :limit")
    fun getMostPopularFlow(limit: Int = 30): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC")
    fun searchSongsFlow(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<SongEntity>): List<Long>

    @Update
    suspend fun updateSong(song: SongEntity): Int

    @Query("UPDATE songs SET is_favorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: Long, isFavorite: Boolean): Int

    @Query("UPDATE songs SET play_count = play_count + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long): Int

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
}
