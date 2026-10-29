package com.minimal.music.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minimal.music.data.model.AddedFolder

@Entity(tableName = "added_folders")
data class AddedFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    @ColumnInfo(name = "song_count") val songCount: Int = 0,
    @ColumnInfo(name = "date_added") val dateAdded: Long = System.currentTimeMillis()
) {
    fun toModel(): AddedFolder = AddedFolder(
        id = id,
        name = name,
        path = path,
        songCount = songCount,
        dateAdded = dateAdded
    )

    companion object {
        fun fromModel(folder: AddedFolder): AddedFolderEntity = AddedFolderEntity(
            id = folder.id,
            name = folder.name,
            path = folder.path,
            songCount = folder.songCount,
            dateAdded = folder.dateAdded
        )
    }
}
