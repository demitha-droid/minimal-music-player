package com.minimal.music.data.source

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.minimal.music.data.model.AddedFolder
import com.minimal.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class FolderArtworkResult(
    val artworkUri: String?,
    val isConfirmed: Boolean,
    val candidateUrls: List<String>,
    val candidateIndex: Int
)

class FolderArtworkHelper(private val context: Context) {

    private val artDir: File by lazy {
        File(context.filesDir, "folder_art").apply { mkdirs() }
    }

    suspend fun resolveArtworkForFolder(
        folder: AddedFolder,
        songs: List<Song>,
        forceWeb: Boolean = false
    ): FolderArtworkResult = withContext(Dispatchers.IO) {
        // 1. Try local art first (unless forceWeb is requested)
        if (!forceWeb) {
            val localArt = findLocalArtwork(folder, songs)
            if (localArt != null) {
                return@withContext FolderArtworkResult(
                    artworkUri = localArt.absolutePath,
                    isConfirmed = true,
                    candidateUrls = emptyList(),
                    candidateIndex = 0
                )
            }
        }

        // 2. Search web for album art
        val candidates = searchWebCandidates(folder, songs)
        if (candidates.isNotEmpty()) {
            val previewFile = File(artDir, "folder_${folder.id}_preview_0.jpg")
            val downloaded = downloadImage(candidates[0], previewFile)
            if (downloaded) {
                return@withContext FolderArtworkResult(
                    artworkUri = previewFile.absolutePath,
                    isConfirmed = false, // Keep / Try Again controls will be shown!
                    candidateUrls = candidates,
                    candidateIndex = 0
                )
            }
        }

        FolderArtworkResult(
            artworkUri = null,
            isConfirmed = true,
            candidateUrls = emptyList(),
            candidateIndex = 0
        )
    }

    suspend fun cycleNextCandidate(
        folder: AddedFolder,
        songs: List<Song>
    ): FolderArtworkResult? = withContext(Dispatchers.IO) {
        var candidates = folder.candidateUrls
        if (candidates.isEmpty()) {
            candidates = searchWebCandidates(folder, songs)
        }

        if (candidates.isEmpty()) return@withContext null

        val nextIndex = (folder.candidateIndex + 1) % candidates.size
        val previewFile = File(artDir, "folder_${folder.id}_preview_${nextIndex}_${System.currentTimeMillis()}.jpg")
        val downloaded = downloadImage(candidates[nextIndex], previewFile)

        if (downloaded) {
            FolderArtworkResult(
                artworkUri = previewFile.absolutePath,
                isConfirmed = false,
                candidateUrls = candidates,
                candidateIndex = nextIndex
            )
        } else {
            null
        }
    }

    private fun findLocalArtwork(folder: AddedFolder, songs: List<Song>): File? {
        // 1. Check embedded ID3 artwork in songs
        val retriever = MediaMetadataRetriever()
        for (song in songs.take(5)) {
            try {
                if (song.mediaUri.startsWith("content://")) {
                    retriever.setDataSource(context, Uri.parse(song.mediaUri))
                } else if (File(song.mediaUri).exists()) {
                    retriever.setDataSource(song.mediaUri)
                }
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    val target = File(artDir, "folder_${folder.id}_embedded.jpg")
                    target.writeBytes(picture)
                    retriever.release()
                    return target
                }
            } catch (_: Exception) {}
        }
        try {
            retriever.release()
        } catch (_: Exception) {}

        // 2. Check local folder directory if standard file path
        if (!folder.path.startsWith("content://")) {
            val dir = File(folder.path)
            if (dir.exists() && dir.isDirectory) {
                val candidateNames = listOf(
                    "cover.jpg", "cover.png", "cover.jpeg",
                    "folder.jpg", "folder.png", "albumart.jpg",
                    "front.jpg", "artwork.jpg"
                )
                for (name in candidateNames) {
                    val file = File(dir, name)
                    if (file.exists() && file.isFile && file.length() > 0) {
                        return file
                    }
                }
            }
        } else {
            // 3. Check SAF Tree DocumentFile
            try {
                val treeDoc = DocumentFile.fromTreeUri(context, Uri.parse(folder.path))
                if (treeDoc != null && treeDoc.isDirectory) {
                    val imageFile = treeDoc.listFiles().firstOrNull { doc ->
                        doc.isFile && (doc.type?.startsWith("image/") == true ||
                            doc.name?.lowercase()?.let {
                                it.endsWith(".jpg") || it.endsWith(".png") || it.endsWith(".jpeg")
                            } == true)
                    }
                    if (imageFile != null) {
                        val target = File(artDir, "folder_${folder.id}_doc.jpg")
                        context.contentResolver.openInputStream(imageFile.uri)?.use { input ->
                            FileOutputStream(target).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (target.exists() && target.length() > 0) {
                            return target
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    private fun searchWebCandidates(folder: AddedFolder, songs: List<Song>): List<String> {
        val queries = buildSearchQueries(folder, songs)
        for (query in queries) {
            val results = queryItunes(query)
            if (results.isNotEmpty()) {
                return results
            }
        }
        return emptyList()
    }

    private fun buildSearchQueries(folder: AddedFolder, songs: List<Song>): List<String> {
        val queries = mutableListOf<String>()

        val firstSongWithArtist = songs.firstOrNull {
            it.artist.isNotBlank() && !it.artist.equals("Unknown Artist", ignoreCase = true)
        }

        val firstSongWithAlbum = songs.firstOrNull {
            it.album.isNotBlank() && !it.album.equals("Unknown Album", ignoreCase = true)
        }

        if (firstSongWithArtist != null && firstSongWithAlbum != null) {
            queries.add("${firstSongWithArtist.artist} ${firstSongWithAlbum.album}")
        }
        if (firstSongWithAlbum != null) {
            queries.add(firstSongWithAlbum.album)
        }
        if (firstSongWithArtist != null) {
            queries.add(firstSongWithArtist.artist)
        }

        // Clean folder name
        val cleanName = folder.name
            .replace(Regex("\\[.*?\\]|\\(.*?\\)|_|-|\\d{4}"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

        if (cleanName.isNotBlank() && cleanName.length > 2) {
            queries.add(cleanName)
        }

        return queries.distinct()
    }

    private fun queryItunes(term: String): List<String> {
        return try {
            val encoded = URLEncoder.encode(term, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encoded&entity=album&limit=8")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MinimalMusicPlayer/1.0 (Android)")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                parseItunesResponse(response)
            } else {
                conn.disconnect()
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseItunesResponse(jsonStr: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val json = JSONObject(jsonStr)
            val results = json.getJSONArray("results")
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                if (item.has("artworkUrl100")) {
                    val rawUrl = item.getString("artworkUrl100")
                    // Upgrade to 600x600 high resolution
                    val highResUrl = rawUrl.replace("100x100bb", "600x600bb")
                    if (!list.contains(highResUrl)) {
                        list.add(highResUrl)
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun downloadImage(imageUrl: String, targetFile: File): Boolean {
        return try {
            val url = URL(imageUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MinimalMusicPlayer/1.0 (Android)")
            }

            if (conn.responseCode == 200) {
                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                conn.disconnect()
                targetFile.exists() && targetFile.length() > 0
            } else {
                conn.disconnect()
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
