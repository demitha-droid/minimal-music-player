package com.minimal.music.data.source

import android.content.Context
import android.net.Uri
import com.minimal.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(
    val timeMs: Long, // -1L if unsynced plain lyric
    val text: String
)

data class ParsedLyrics(
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val rawText: String
) {
    /**
     * Determines the currently active line index based on playback position.
     * Uses a default [leadOffsetMs] of 450ms so lines highlight as soon as singing begins,
     * compensating for audio buffer latency and vocal onset timing.
     */
    fun getCurrentLineIndex(positionMs: Long, leadOffsetMs: Long = 450L): Int {
        if (!isSynced || lines.isEmpty()) return -1
        val effectiveMs = positionMs + leadOffsetMs
        var activeIndex = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= effectiveMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }

    companion object {
        val EMPTY = ParsedLyrics(isSynced = false, lines = emptyList(), rawText = "")
    }
}

class LyricsHelper(private val context: Context) {

    private val lyricsDir: File by lazy {
        File(context.filesDir, "lyrics").apply { mkdirs() }
    }

    fun parseLyrics(raw: String?): ParsedLyrics = Companion.parseLyrics(raw)

    companion object {
        private val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]""")

        /**
         * Parses an LRC formatted string or plain text lyrics into structured [ParsedLyrics].
         */
        fun parseLyrics(raw: String?): ParsedLyrics {
            if (raw.isNullOrBlank()) {
                return ParsedLyrics.EMPTY
            }

            val lines = raw.lines()
            val parsedLines = mutableListOf<LyricLine>()
            var foundTimestamp = false

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isBlank()) continue

                // Skip standard LRC metadata tags like [ti:...], [ar:...], [al:...], [by:...], [offset:...]
                if (trimmed.startsWith("[ti:") ||
                    trimmed.startsWith("[ar:") ||
                    trimmed.startsWith("[al:") ||
                    trimmed.startsWith("[by:") ||
                    trimmed.startsWith("[offset:") ||
                    trimmed.startsWith("[re:") ||
                    trimmed.startsWith("[ve:") ||
                    trimmed.startsWith("[length:")
                ) {
                    continue
                }

                val matches = timestampRegex.findAll(trimmed).toList()
                if (matches.isNotEmpty()) {
                    foundTimestamp = true
                    // Remove all timestamps from the line to get the lyric text
                    val lyricText = trimmed.replace(timestampRegex, "").trim()

                    for (match in matches) {
                        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                        val msGroup = match.groupValues.getOrNull(3).orEmpty()
                        val ms = when (msGroup.length) {
                            1 -> (msGroup.toIntOrNull() ?: 0) * 100L
                            2 -> (msGroup.toIntOrNull() ?: 0) * 10L
                            3 -> msGroup.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val totalMs = minutes * 60_000L + seconds * 1000L + ms
                        parsedLines.add(LyricLine(timeMs = totalMs, text = lyricText))
                    }
                } else {
                    // Potential plain line
                    if (!foundTimestamp) {
                        parsedLines.add(LyricLine(timeMs = -1L, text = trimmed))
                    }
                }
            }

            return if (foundTimestamp) {
                val sorted = parsedLines.filter { it.timeMs >= 0L }.sortedBy { it.timeMs }
                ParsedLyrics(isSynced = true, lines = sorted, rawText = raw)
            } else {
                ParsedLyrics(isSynced = false, lines = parsedLines, rawText = raw)
            }
        }
    }

    /**
     * Resolves lyrics for a given song. Checks in order:
     * 1. Already supplied song.lyrics or internal disk cache.
     * 2. Local .lrc or .txt file next to the song file.
     * 3. LRCLIB API (exact match then search).
     * 4. Lyrics.ovh API fallback.
     */
    suspend fun resolveLyrics(song: Song, forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val cacheFile = File(lyricsDir, "song_${song.id}.lrc")

        if (!forceRefresh) {
            if (!song.lyrics.isNullOrBlank()) {
                // Ensure cached on disk too
                try {
                    if (!cacheFile.exists() || cacheFile.length() == 0L) {
                        cacheFile.writeText(song.lyrics)
                    }
                } catch (_: Exception) {}
                return@withContext song.lyrics
            }

            if (cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    val cached = cacheFile.readText().trim()
                    if (cached.isNotBlank()) return@withContext cached
                } catch (_: Exception) {}
            }
        }

        // Check local storage alongside the audio file
        val localLyrics = findLocalLrcFile(song)
        if (!localLyrics.isNullOrBlank()) {
            saveToCache(cacheFile, localLyrics)
            return@withContext localLyrics
        }

        // Fetch from remote APIs
        val remoteLyrics = fetchRemoteLyrics(song)
        if (!remoteLyrics.isNullOrBlank()) {
            saveToCache(cacheFile, remoteLyrics)
            return@withContext remoteLyrics
        }

        null
    }

    private fun findLocalLrcFile(song: Song): String? {
        try {
            val uri = Uri.parse(song.mediaUri)
            val filePath = when {
                uri.scheme == "file" -> uri.path
                song.mediaUri.startsWith("/") -> song.mediaUri
                else -> null
            } ?: return null

            val audioFile = File(filePath)
            if (!audioFile.exists()) return null

            val parent = audioFile.parentFile ?: return null
            val baseName = audioFile.nameWithoutExtension

            val possibleLrc = File(parent, "$baseName.lrc")
            if (possibleLrc.exists() && possibleLrc.canRead()) {
                val content = possibleLrc.readText().trim()
                if (content.isNotBlank()) return content
            }

            val possibleTxt = File(parent, "$baseName.txt")
            if (possibleTxt.exists() && possibleTxt.canRead()) {
                val content = possibleTxt.readText().trim()
                if (content.isNotBlank()) return content
            }
        } catch (_: Exception) {
            // Ignore file access exceptions
        }
        return null
    }

    private fun saveToCache(file: File, content: String) {
        try {
            file.writeText(content)
        } catch (_: Exception) {}
    }

    private suspend fun fetchRemoteLyrics(song: Song): String? {
        val cleanTitle = cleanSongTitle(song.title)
        val cleanArtist = cleanArtistName(song.artist)
        if (cleanTitle.isBlank()) return null

        val durationSec = (song.durationMs / 1000).toInt()

        // 1. LRCLIB Exact Lookup
        val lrclibExact = fetchFromLrclibGet(cleanTitle, cleanArtist, durationSec)
        if (!lrclibExact.isNullOrBlank()) {
            return lrclibExact
        }

        // 2. LRCLIB Search Query
        val searchQuery = if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle
        val lrclibSearch = fetchFromLrclibSearch(searchQuery)
        if (!lrclibSearch.isNullOrBlank()) {
            return lrclibSearch
        }

        // 3. Fallback: Lyrics.ovh (plain lyrics)
        if (cleanArtist.isNotBlank()) {
            val ovhLyrics = fetchFromLyricsOvh(cleanArtist, cleanTitle)
            if (!ovhLyrics.isNullOrBlank()) {
                return ovhLyrics
            }
        }

        return null
    }

    private fun fetchFromLrclibGet(title: String, artist: String, durationSec: Int): String? {
        var conn: HttpURLConnection? = null
        try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val urlString = StringBuilder("https://lrclib.net/api/get?track_name=$encTitle")
            if (artist.isNotBlank()) {
                urlString.append("&artist_name=$encArtist")
            }
            if (durationSec > 10) {
                urlString.append("&duration=$durationSec")
            }

            val url = URL(urlString.toString())
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MinimalMusicPlayer/1.0 (Android Music Player)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)

                val synced = obj.optString("syncedLyrics", "").trim()
                if (synced.isNotBlank()) return synced

                val plain = obj.optString("plainLyrics", "").trim()
                if (plain.isNotBlank()) return plain
            }
        } catch (_: Exception) {
            // Ignore network errors and continue to fallback
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun fetchFromLrclibSearch(query: String): String? {
        var conn: HttpURLConnection? = null
        try {
            val encQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://lrclib.net/api/search?q=$encQuery")
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MinimalMusicPlayer/1.0 (Android Music Player)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)

                var fallbackPlain: String? = null

                for (i in 0 until array.length().coerceAtMost(5)) {
                    val item = array.optJSONObject(i) ?: continue
                    val synced = item.optString("syncedLyrics", "").trim()
                    if (synced.isNotBlank()) {
                        return synced
                    }
                    if (fallbackPlain == null) {
                        val plain = item.optString("plainLyrics", "").trim()
                        if (plain.isNotBlank()) {
                            fallbackPlain = plain
                        }
                    }
                }

                if (!fallbackPlain.isNullOrBlank()) {
                    return fallbackPlain
                }
            }
        } catch (_: Exception) {
            // Ignore network errors
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun fetchFromLyricsOvh(artist: String, title: String): String? {
        var conn: HttpURLConnection? = null
        try {
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val url = URL("https://api.lyrics.ovh/v1/$encArtist/$encTitle")
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "MinimalMusicPlayer/1.0 (Android Music Player)")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val lyrics = obj.optString("lyrics", "").trim()
                if (lyrics.isNotBlank()) {
                    return lyrics
                }
            }
        } catch (_: Exception) {
            // Ignore network errors
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun cleanSongTitle(raw: String): String {
        return raw
            .replace(Regex("\\.(mp3|m4a|wav|flac|ogg|aac|opus|wma)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\d+[\\s.-]+"), "")
            .replace(Regex("\\[.*?\\]"), " ")
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
}
