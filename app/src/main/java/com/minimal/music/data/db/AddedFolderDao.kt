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

    @Query("SELECT * FROM added_folders WHERE path = :path LIMIT 1")
    suspend fun getFolderByPath(path: String): AddedFolderEntity?

    @Query("SELECT * FROM added_folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: Long): AddedFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: AddedFolderEntity): Long

    @Query("UPDATE added_folders SET song_count = :count WHERE id = :id")
    suspend fun updateSongCount(id: Long, count: Int): Int

    @Query("DELETE FROM added_folders WHERE id = :id")
    suspend fun deleteFolder(id: Long): Int

    @Query("DELETE FROM added_folders WHERE path = :path")
    suspend fun deleteFolderByPath(path: String): Int

    @Query("SELECT COUNT(*) FROM added_folders")
    suspend fun getFolderCount(): Int
}
