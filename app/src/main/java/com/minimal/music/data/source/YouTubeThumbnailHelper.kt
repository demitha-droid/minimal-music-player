package com.minimal.music.data.source

import android.content.Context
import com.minimal.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class YouTubeThumbnailHelper(private val context: Context) {

    private val artDir: File by lazy {
        File(context.filesDir, "song_art").apply { mkdirs() }
    }

    private val videoIdRegex = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"")

    suspend fun fetchAndSaveThumbnail(song: Song): String? = withContext(Dispatchers.IO) {
        val targetFile = File(artDir, "song_${song.id}.jpg")
        
        // If file already exists and is non-empty, reuse it
        if (targetFile.exists() && targetFile.length() > 1024) {
            return@withContext targetFile.absolutePath
        }

        val queries = buildSearchQueries(song)
        for (query in queries) {
            val videoId = searchYouTubeForVideoId(query)
            if (videoId != null) {
                val downloaded = downloadThumbnailForVideoId(videoId, targetFile)
                if (downloaded) {
                    return@withContext targetFile.absolutePath
                }
            }
            delay(150L) // Polite delay between requests
        }

        null
    }

    private fun buildSearchQueries(song: Song): List<String> {
        val queries = mutableListOf<String>()

        val cleanTitle = cleanSongTitle(song.title)
        val cleanArtist = cleanArtistName(song.artist)

        if (cleanArtist.isNotBlank() && cleanArtist != "Unknown Artist") {
            queries.add("$cleanTitle $cleanArtist official audio")
            queries.add("$cleanTitle $cleanArtist")
        } else {
            queries.add("$cleanTitle official audio")
            queries.add(cleanTitle)
        }

        if (song.album.isNotBlank() && song.album != "Unknown Album" && song.album != song.title) {
            queries.add("$cleanTitle ${song.album}")
        }

        return queries.distinct()
    }

    private fun cleanSongTitle(raw: String): String {
        return raw
            .replace(Regex("\\.(mp3|m4a|wav|flac|ogg|aac|opus|wma)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\d+[\\s.-]+"), "") // Remove leading track numbers like "01 - " or "01. "
            .replace(Regex("\\[.*?\\]"), " ") // Remove bracketed info [Official Video], [Audio]
            .replace(Regex("\\(.*?official.*?\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\(.*?video.*?\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\(.*?lyrics.*?\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\(.*?audio.*?\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\(.*?remaster.*?\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("[_\\-]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun cleanArtistName(raw: String): String {
        if (raw.equals("Unknown Artist", ignoreCase = true) || raw.equals("<unknown>", ignoreCase = true)) {
            return ""
        }
        return raw
            .replace(Regex("[_\\-]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun searchYouTubeForVideoId(query: String): String? {
        var conn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://www.youtube.com/results?search_query=$encodedQuery")
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                instanceFollowRedirects = true
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                val match = videoIdRegex.find(html)
                if (match != null) {
                    return match.groupValues[1]
                }
            }
        } catch (_: Exception) {
            // Network failure or timeout
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun downloadThumbnailForVideoId(videoId: String, targetFile: File): Boolean {
        // High quality thumbnail options in preference order
        val candidateUrls = listOf(
            "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            "https://img.youtube.com/vi/$videoId/mqdefault.jpg",
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        )

        for (thumbUrl in candidateUrls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(thumbUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    instanceFollowRedirects = true
                }

                val responseCode = conn.responseCode
                val contentLength = conn.contentLength

                // YouTube returns a 1x1 gif or empty 404 for missing maxresdefault
                if (responseCode == HttpURLConnection.HTTP_OK && (contentLength > 1000 || contentLength == -1)) {
                    val tempFile = File(artDir, "${targetFile.name}.tmp")
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 1000) {
                        if (targetFile.exists()) targetFile.delete()
                        val success = tempFile.renameTo(targetFile)
                        if (success) return true
                    } else {
                        tempFile.delete()
                    }
                }
            } catch (_: Exception) {
                // Try next resolution
            } finally {
                conn?.disconnect()
            }
        }
        return false
    }
}
