package com.minimal.music.data.source

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.minimal.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    suspend fun scanDeviceAudio(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataColumn = it.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown Title"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn) ?: "Unknown Album"
                    val durationMs = it.getLong(durationColumn)
                    val albumId = it.getLong(albumIdColumn)
                    val dateAdded = it.getLong(dateAddedColumn)

                    val filePath = if (dataColumn >= 0) it.getString(dataColumn) else null
                    val file = if (!filePath.isNullOrBlank()) File(filePath) else null
                    val folderName = file?.parentFile?.name ?: "Music"
                    val folderPath = file?.parentFile?.absolutePath ?: file?.parent ?: ""

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    songs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            durationMs = durationMs,
                            mediaUri = contentUri,
                            artworkUri = artworkUri,
                            isFavorite = false,
                            playCount = 0,
                            popularityScore = 50,
                            dateAdded = dateAdded * 1000,
                            folderName = folderName,
                            folderPath = folderPath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songs
    }

    suspend fun scanFolder(targetFolderPath: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )

        // Matches folder path and any subfolder files
        val normalizedPath = targetFolderPath.trimEnd(File.separatorChar)
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= 5000) AND ${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("$normalizedPath%")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            val folderFile = File(normalizedPath)
            val folderDefaultName = folderFile.name.ifEmpty { "Music" }

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataColumn = it.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown Title"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn) ?: "Unknown Album"
                    val durationMs = it.getLong(durationColumn)
                    val albumId = it.getLong(albumIdColumn)
                    val dateAdded = it.getLong(dateAddedColumn)

                    val filePath = if (dataColumn >= 0) it.getString(dataColumn) else null
                    val file = if (!filePath.isNullOrBlank()) File(filePath) else null
                    val folderName = file?.parentFile?.name ?: folderDefaultName

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    songs.add(
                        Song(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            durationMs = durationMs,
                            mediaUri = contentUri,
                            artworkUri = artworkUri,
                            isFavorite = false,
                            playCount = 0,
                            popularityScore = 50,
                            dateAdded = dateAdded * 1000,
                            folderName = folderDefaultName,
                            folderPath = normalizedPath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songs
    }
}
