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
    @ColumnInfo(name = "date_added") val dateAdded: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "artwork_uri") val artworkUri: String? = null,
    @ColumnInfo(name = "is_artwork_confirmed") val isArtworkConfirmed: Boolean = true,
    @ColumnInfo(name = "candidate_urls") val candidateUrls: String? = null,
    @ColumnInfo(name = "candidate_index") val candidateIndex: Int = 0
) {
    fun toModel(): AddedFolder {
        val urls = candidateUrls?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
        return AddedFolder(
            id = id,
            name = name,
            path = path,
            songCount = songCount,
            dateAdded = dateAdded,
            artworkUri = artworkUri,
            isArtworkConfirmed = isArtworkConfirmed,
            candidateUrls = urls,
            candidateIndex = candidateIndex
        )
    }

    companion object {
        fun fromModel(folder: AddedFolder): AddedFolderEntity = AddedFolderEntity(
            id = folder.id,
            name = folder.name,
            path = folder.path,
            songCount = folder.songCount,
            dateAdded = folder.dateAdded,
            artworkUri = folder.artworkUri,
            isArtworkConfirmed = folder.isArtworkConfirmed,
            candidateUrls = if (folder.candidateUrls.isNotEmpty()) folder.candidateUrls.joinToString("|||") else null,
            candidateIndex = folder.candidateIndex
        )
    }
}
