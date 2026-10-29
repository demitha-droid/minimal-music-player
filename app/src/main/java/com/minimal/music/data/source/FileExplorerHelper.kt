package com.minimal.music.data.source

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.minimal.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class FileExplorerItem {
    data class Directory(
        val file: File,
        val name: String,
        val path: String,
        val itemCount: Int,
        val audioCount: Int
    ) : FileExplorerItem()

    data class Audio(
        val file: File,
        val name: String,
        val path: String,
        val sizeFormatted: String,
        val song: Song
    ) : FileExplorerItem()
}

class FileExplorerHelper(private val context: Context) {

    private val audioExtensions = setOf("mp3", "m4a", "wav", "flac", "ogg", "aac", "opus", "wma")

    fun getCommonRoots(): List<File> {
        val roots = mutableListOf<File>()

        try {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (musicDir != null && musicDir.exists()) roots.add(musicDir)
        } catch (_: Exception) {}

        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists()) roots.add(downloadDir)
        } catch (_: Exception) {}

        try {
            val storageDir = Environment.getExternalStorageDirectory()
            if (storageDir != null && storageDir.exists() && !roots.contains(storageDir)) {
                roots.add(storageDir)
            }
        } catch (_: Exception) {}

        return roots
    }

    suspend fun listDirectory(directory: File): List<FileExplorerItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileExplorerItem>()
        val files = directory.listFiles() ?: return@withContext emptyList()

        for (file in files) {
            if (file.isHidden) continue

            if (file.isDirectory) {
                val subFiles = file.listFiles() ?: emptyArray()
                val visibleSubFiles = subFiles.filter { !it.isHidden }
                val audioCount = visibleSubFiles.count { f ->
                    !f.isDirectory && f.extension.lowercase() in audioExtensions
                }
                items.add(
                    FileExplorerItem.Directory(
                        file = file,
                        name = file.name,
                        path = file.absolutePath,
                        itemCount = visibleSubFiles.size,
                        audioCount = audioCount
                    )
                )
            } else if (file.extension.lowercase() in audioExtensions) {
                val song = extractSongFromFile(file)
                items.add(
                    FileExplorerItem.Audio(
                        file = file,
                        name = file.name,
                        path = file.absolutePath,
                        sizeFormatted = formatFileSize(file.length()),
                        song = song
                    )
                )
            }
        }

        // Directories first alphabetically, then audio files alphabetically
        items.sortedWith(
            compareBy<FileExplorerItem> {
                when (it) {
                    is FileExplorerItem.Directory -> 0
                    is FileExplorerItem.Audio -> 1
                }
            }.thenBy {
                when (it) {
                    is FileExplorerItem.Directory -> it.name.lowercase()
                    is FileExplorerItem.Audio -> it.name.lowercase()
                }
            }
        )
    }

    fun extractSongFromFile(file: File): Song {
        var title = file.nameWithoutExtension
        var artist = "Local File"
        var album = file.parentFile?.name ?: "Storage"
        var durationMs = 0L

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                if (it.isNotBlank()) title = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                if (it.isNotBlank()) artist = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                if (it.isNotBlank()) album = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                durationMs = it.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) {
            // Fall back to filename defaults
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        // Deterministic unique ID from file path hash
        val id = file.absolutePath.hashCode().toLong().let { if (it < 0) -it else it }

        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mediaUri = Uri.fromFile(file).toString(),
            artworkUri = null,
            isFavorite = false,
            playCount = 0,
            popularityScore = 50,
            dateAdded = file.lastModified(),
            folderName = file.parentFile?.name ?: "Storage",
            folderPath = file.parentFile?.absolutePath ?: file.parent ?: ""
        )
    }

    suspend fun scanDirectoryRecursive(directory: File): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        fun scan(dir: File) {
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isHidden) continue
                if (f.isDirectory) {
                    scan(f)
                } else if (f.extension.lowercase() in audioExtensions) {
                    val song = extractSongFromFile(f).copy(
                        folderName = directory.name,
                        folderPath = directory.absolutePath
                    )
                    songs.add(song)
                }
            }
        }
        scan(directory)
        songs
    }

    suspend fun scanTreeUriRecursive(treeUri: Uri): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val documentFile = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val rootFolderName = documentFile.name ?: "Imported Folder"
        val rootFolderPath = treeUri.toString()

        fun scanDocument(doc: DocumentFile) {
            val children = doc.listFiles()
            for (child in children) {
                if (child.isDirectory) {
                    scanDocument(child)
                } else if (child.isFile) {
                    val name = child.name ?: ""
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in audioExtensions) {
                        parseSongFromDocumentFile(
                            doc = child,
                            folderName = rootFolderName,
                            folderPath = rootFolderPath
                        )?.let { songs.add(it) }
                    }
                }
            }
        }

        scanDocument(documentFile)
        songs
    }

    fun parseSongFromDocumentFile(
        doc: DocumentFile,
        folderName: String = "Imported Files",
        folderPath: String = "SAF_FILES"
    ): Song? {
        val uri = doc.uri
        var title = doc.name?.substringBeforeLast('.') ?: "Unknown Track"
        var artist = "Imported Track"
        var album = folderName
        var durationMs = 0L

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                if (it.isNotBlank()) title = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                if (it.isNotBlank()) artist = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                if (it.isNotBlank()) album = it.trim()
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                durationMs = it.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        val id = uri.toString().hashCode().toLong().let { if (it < 0) -it else it }

        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            mediaUri = uri.toString(),
            artworkUri = null,
            isFavorite = false,
            playCount = 0,
            popularityScore = 50,
            dateAdded = System.currentTimeMillis(),
            folderName = folderName,
            folderPath = folderPath
        )
    }

    suspend fun parseSongsFromUris(uris: List<Uri>): List<Song> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            val doc = DocumentFile.fromSingleUri(context, uri)
            if (doc != null) {
                parseSongFromDocumentFile(
                    doc = doc,
                    folderName = "Imported Files",
                    folderPath = "SAF_FILES"
                )
            } else {
                null
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            "%.1f MB".format(mb)
        } else {
            "%.0f KB".format(kb)
        }
    }
}
