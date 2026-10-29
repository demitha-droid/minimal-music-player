package com.minimal.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AddedFolderDao {

    @Query("SELECT * FROM added_folders ORDER BY date_added DESC")
    fun getAllAddedFoldersFlow(): Flow<List<AddedFolderEntity>>

    @Query("SELECT * FROM added_folders")
    suspend fun getAllFoldersList(): List<AddedFolderEntity>

    @Query("SELECT * FROM added_folders WHERE path = :path LIMIT 1")
    suspend fun getFolderByPath(path: String): AddedFolderEntity?

    @Query("SELECT * FROM added_folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Long): AddedFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: AddedFolderEntity): Long

    @Query("UPDATE added_folders SET song_count = :count WHERE id = :id")
    suspend fun updateSongCount(id: Long, count: Int): Int

    @Query("UPDATE added_folders SET artwork_uri = :artworkUri, is_artwork_confirmed = :isConfirmed, candidate_urls = :candidateUrls, candidate_index = :candidateIndex WHERE id = :id")
    suspend fun updateArtwork(id: Long, artworkUri: String?, isConfirmed: Boolean, candidateUrls: String?, candidateIndex: Int): Int

    @Query("UPDATE added_folders SET is_artwork_confirmed = 1 WHERE id = :id")
    suspend fun confirmArtwork(id: Long): Int

    @Query("UPDATE added_folders SET artwork_uri = :artworkUri, candidate_index = :candidateIndex WHERE id = :id")
    suspend fun updateCandidateIndex(id: Long, artworkUri: String?, candidateIndex: Int): Int

    @Query("DELETE FROM added_folders WHERE id = :id")
    suspend fun deleteFolder(id: Long): Int

    @Query("DELETE FROM added_folders WHERE path = :path")
    suspend fun deleteFolderByPath(path: String): Int

    @Query("SELECT COUNT(*) FROM added_folders")
    suspend fun getFolderCount(): Int
}
