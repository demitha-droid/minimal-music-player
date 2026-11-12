package com.minimal.music.data.repository

import android.content.Context
import com.minimal.music.data.db.AddedFolderEntity
import com.minimal.music.data.db.MusicDatabase
import com.minimal.music.data.db.PlaylistEntity
import com.minimal.music.data.db.PlaylistSongCrossRef
import com.minimal.music.data.db.SongEntity
import com.minimal.music.data.model.AddedFolder
import com.minimal.music.data.model.Playlist
import com.minimal.music.data.model.Song
import com.minimal.music.data.source.FolderArtworkHelper
import com.minimal.music.data.source.FolderArtworkResult
import com.minimal.music.data.source.LyricsHelper
import com.minimal.music.data.source.MediaStoreScanner
import com.minimal.music.data.source.YouTubeThumbnailHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val database: MusicDatabase = MusicDatabase.getDatabase(context)
) {
    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val addedFolderDao = database.addedFolderDao()
    private val mediaStoreScanner = MediaStoreScanner(context)
    val fileExplorerHelper = com.minimal.music.data.source.FileExplorerHelper(context)
    val folderArtworkHelper = FolderArtworkHelper(context)
    val youTubeThumbnailHelper = YouTubeThumbnailHelper(context)
    val lyricsHelper = LyricsHelper(context)

    val allSongs: Flow<List<Song>> = songDao.getAllSongsFlow()
        .map { entities -> entities.map { it.toModel() } }
        .flowOn(Dispatchers.IO)

    val addedFolders: Flow<List<AddedFolder>> = addedFolderDao.getAllAddedFoldersFlow()
        .map { entities -> entities.map { it.toModel() } }
        .flowOn(Dispatchers.IO)

    val favorites: Flow<List<Song>> = songDao.getFavoritesFlow()
        .map { entities -> entities.map { it.toModel() } }
        .flowOn(Dispatchers.IO)

    val mostPlayed: Flow<List<Song>> = songDao.getMostPlayedFlow()
        .map { entities -> entities.map { it.toModel() } }
        .flowOn(Dispatchers.IO)

    val mostPopular: Flow<List<Song>> = songDao.getMostPopularFlow()
        .map { entities -> entities.map { it.toModel() } }
        .flowOn(Dispatchers.IO)

    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylistsFlow()
        .map { entities ->
            entities.map { entity ->
                val count = playlistDao.getSongCountForPlaylist(entity.id)
                entity.toModel(songCount = count)
            }
        }
        .flowOn(Dispatchers.IO)

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylistFlow(playlistId)
            .map { entities -> entities.map { it.toModel() } }
            .flowOn(Dispatchers.IO)
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongsFlow(query)
            .map { entities -> entities.map { it.toModel() } }
            .flowOn(Dispatchers.IO)
    }

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        // Purge filler music and stale featured music folders
        songDao.deleteFillerSongs()
        addedFolderDao.deleteFolderByPath("FEATURED_MUSIC")

        // Database deduplication purge: identify and eliminate duplicate songs
        val allSongsList = songDao.getAllSongsList()
        val seenSignatures = mutableSetOf<String>()
        val seenUris = mutableSetOf<String>()
        val duplicateIds = mutableListOf<Long>()

        for (song in allSongsList) {
            val uriLower = song.mediaUri.lowercase()
            val normTitle = song.title.trim().lowercase()
            val normArtist = song.artist.trim().lowercase()
            val durBucket = song.durationMs / 2000L
            val metaKey = if (normTitle.isNotBlank()) "${normTitle}::${normArtist}::${durBucket}" else null

            val isDuplicate = (song.mediaUri.isNotBlank() && seenUris.contains(uriLower)) ||
                    (metaKey != null && seenSignatures.contains(metaKey))

            if (isDuplicate) {
                duplicateIds.add(song.id)
            } else {
                if (song.mediaUri.isNotBlank()) seenUris.add(uriLower)
                metaKey?.let { seenSignatures.add(it) }
            }
        }

        if (duplicateIds.isNotEmpty()) {
            songDao.deleteSongsByIds(duplicateIds)
        }

        // Clean slate migration: clear previously auto-populated folders so only manual folders appear
        val prefs = context.getSharedPreferences("minimal_music_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("clean_slate_v1", false)) {
            addedFolderDao.clearAll()
            prefs.edit().putBoolean("clean_slate_v1", true).apply()
        }

        // Synchronize accurate song counts and check for artwork resolution
        val allFolders = addedFolderDao.getAllFoldersList()
        for (folderEntity in allFolders) {
            val model = folderEntity.toModel()
            val songsInFolder = getSongsListForFolder(model.path, model.name)
            if (folderEntity.songCount != songsInFolder.size) {
                addedFolderDao.updateSongCount(folderEntity.id, songsInFolder.size)
            }
            if (folderEntity.artworkUri.isNullOrBlank()) {
                resolveAndSaveArtwork(model, songsInFolder, forceWeb = false)
            }
        }

        // Automatic background resolution of YouTube thumbnails for songs needing artwork
        val songsNeedingArt = songDao.getSongsNeedingArtwork()
        if (songsNeedingArt.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                for (entity in songsNeedingArt) {
                    try {
                        val path = youTubeThumbnailHelper.fetchAndSaveThumbnail(entity.toModel())
                        if (path != null) {
                            songDao.updateSongArtwork(entity.id, path)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    suspend fun clearAllAddedFolders(): Int = withContext(Dispatchers.IO) {
        addedFolderDao.clearAll()
    }

    suspend fun getSongsListForFolder(folderPath: String, folderName: String): List<Song> = withContext(Dispatchers.IO) {
        val all = songDao.getAllSongsList()
        val targetPath = folderPath.trimEnd('/', '\\')
        all.filter { song ->
            val songPath = song.folderPath?.trimEnd('/', '\\')
            val pathMatch = songPath != null && (
                songPath.equals(targetPath, ignoreCase = true) ||
                songPath.startsWith("$targetPath/", ignoreCase = true) ||
                songPath.startsWith("$targetPath\\", ignoreCase = true)
            )
            val nameMatch = !song.folderName.isNullOrBlank() &&
                song.folderName.equals(folderName, ignoreCase = true) &&
                (songPath == null || songPath.contains(folderName, ignoreCase = true))

            pathMatch || nameMatch
        }.distinctBy {
            val normTitle = it.title.trim().lowercase()
            val normArtist = it.artist.trim().lowercase()
            val durBucket = it.durationMs / 2000L
            "${normTitle}::${normArtist}::${durBucket}"
        }.map { it.toModel() }
    }

    suspend fun resolveAndSaveArtwork(
        folder: AddedFolder,
        songs: List<Song>,
        forceWeb: Boolean = false
    ): FolderArtworkResult = withContext(Dispatchers.IO) {
        val result = folderArtworkHelper.resolveArtworkForFolder(folder, songs, forceWeb)
        if (result.artworkUri != null) {
            addedFolderDao.updateArtwork(
                id = folder.id,
                artworkUri = result.artworkUri,
                isConfirmed = result.isConfirmed,
                candidateUrls = result.candidateUrls.joinToString("|||"),
                candidateIndex = result.candidateIndex
            )
        }
        result
    }

    suspend fun confirmFolderArtwork(folderId: Long): Boolean = withContext(Dispatchers.IO) {
        val rows = addedFolderDao.confirmArtwork(folderId)
        rows > 0
    }

    suspend fun cycleFolderArtwork(folder: AddedFolder): FolderArtworkResult? = withContext(Dispatchers.IO) {
        val songs = getSongsListForFolder(folder.path, folder.name)
        val result = folderArtworkHelper.cycleNextCandidate(folder, songs)
        if (result != null && result.artworkUri != null) {
            addedFolderDao.updateArtwork(
                id = folder.id,
                artworkUri = result.artworkUri,
                isConfirmed = false,
                candidateUrls = result.candidateUrls.joinToString("|||"),
                candidateIndex = result.candidateIndex
            )
        }
        result
    }

    private suspend fun deduplicateAndInsertSongs(newEntities: List<SongEntity>): Int {
        if (newEntities.isEmpty()) return 0
        val existingSongs = songDao.getAllSongsList()
        val existingUris = mutableSetOf<String>()
        val existingSignatures = mutableSetOf<String>()

        for (s in existingSongs) {
            if (s.mediaUri.isNotBlank()) {
                existingUris.add(s.mediaUri.lowercase())
            }
            val normTitle = s.title.trim().lowercase()
            val normArtist = s.artist.trim().lowercase()
            val durBucket = s.durationMs / 2000L
            if (normTitle.isNotBlank()) {
                existingSignatures.add("${normTitle}::${normArtist}::${durBucket}")
            }
        }

        val toInsert = mutableListOf<SongEntity>()
        for (s in newEntities) {
            val uriLower = s.mediaUri.lowercase()
            val normTitle = s.title.trim().lowercase()
            val normArtist = s.artist.trim().lowercase()
            val durBucket = s.durationMs / 2000L
            val metaKey = if (normTitle.isNotBlank()) "${normTitle}::${normArtist}::${durBucket}" else null

            val isDuplicate = (s.mediaUri.isNotBlank() && existingUris.contains(uriLower)) ||
                    (metaKey != null && existingSignatures.contains(metaKey))

            if (!isDuplicate) {
                toInsert.add(s)
                if (s.mediaUri.isNotBlank()) existingUris.add(uriLower)
                metaKey?.let { existingSignatures.add(it) }
            }
        }

        if (toInsert.isNotEmpty()) {
            songDao.insertSongs(toInsert)
            CoroutineScope(Dispatchers.IO).launch {
                for (entity in toInsert) {
                    try {
                        val path = youTubeThumbnailHelper.fetchAndSaveThumbnail(entity.toModel())
                        if (path != null) {
                            songDao.updateSongArtwork(entity.id, path)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return toInsert.size
    }

    suspend fun scanAndSyncDeviceAudio(): Int = withContext(Dispatchers.IO) {
        val deviceSongs = mediaStoreScanner.scanDeviceAudio()
        if (deviceSongs.isNotEmpty()) {
            val entities = deviceSongs.map { SongEntity.fromModel(it) }
            deduplicateAndInsertSongs(entities)
        }
        deviceSongs.size
    }

    fun getSongsByFolder(folderPath: String): Flow<List<Song>> {
        return songDao.getSongsByFolderPathFlow(folderPath)
            .map { entities -> entities.map { it.toModel() } }
            .flowOn(Dispatchers.IO)
    }

    suspend fun addCustomSongs(songs: List<Song>): Int = withContext(Dispatchers.IO) {
        if (songs.isNotEmpty()) {
            val entities = songs.map { SongEntity.fromModel(it) }
            deduplicateAndInsertSongs(entities)
        }
        songs.size
    }

    suspend fun addFolderToLibrary(folder: java.io.File): Int = withContext(Dispatchers.IO) {
        var songs = fileExplorerHelper.scanDirectoryRecursive(folder)
        if (songs.isEmpty()) {
            songs = mediaStoreScanner.scanFolder(folder.absolutePath)
        }
        val distinctSongs = songs.distinctBy {
            val normTitle = it.title.trim().lowercase()
            val normArtist = it.artist.trim().lowercase()
            val durBucket = it.durationMs / 2000L
            "${normTitle}::${normArtist}::${durBucket}"
        }
        if (distinctSongs.isNotEmpty()) {
            val entities = distinctSongs.map { SongEntity.fromModel(it) }
            deduplicateAndInsertSongs(entities)

            val existing = addedFolderDao.getFolderByPath(folder.absolutePath)
            val folderId = addedFolderDao.insertFolder(
                AddedFolderEntity(
                    id = existing?.id ?: 0L,
                    name = folder.name,
                    path = folder.absolutePath,
                    songCount = distinctSongs.size
                )
            )
            val inserted = addedFolderDao.getFolderById(if (existing != null) existing.id else folderId)
            if (inserted != null) {
                resolveAndSaveArtwork(inserted.toModel(), distinctSongs, forceWeb = false)
            }
        }
        distinctSongs.size
    }

    suspend fun importFromTreeUri(treeUri: android.net.Uri): Int = withContext(Dispatchers.IO) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: Exception) {}

        val songs = fileExplorerHelper.scanTreeUriRecursive(treeUri)
        val distinctSongs = songs.distinctBy {
            val normTitle = it.title.trim().lowercase()
            val normArtist = it.artist.trim().lowercase()
            val durBucket = it.durationMs / 2000L
            "${normTitle}::${normArtist}::${durBucket}"
        }
        if (distinctSongs.isNotEmpty()) {
            val entities = distinctSongs.map { SongEntity.fromModel(it) }
            deduplicateAndInsertSongs(entities)

            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            val folderName = doc?.name ?: "Imported Folder"
            val path = treeUri.toString()
            val existing = addedFolderDao.getFolderByPath(path)
            val folderId = addedFolderDao.insertFolder(
                AddedFolderEntity(
                    id = existing?.id ?: 0L,
                    name = folderName,
                    path = path,
                    songCount = distinctSongs.size
                )
            )
            val inserted = addedFolderDao.getFolderById(if (existing != null) existing.id else folderId)
            if (inserted != null) {
                resolveAndSaveArtwork(inserted.toModel(), distinctSongs, forceWeb = false)
            }
        }
        distinctSongs.size
    }

    suspend fun importFilesFromUris(uris: List<android.net.Uri>): Int = withContext(Dispatchers.IO) {
        val songs = fileExplorerHelper.parseSongsFromUris(uris)
        val distinctSongs = songs.distinctBy {
            val normTitle = it.title.trim().lowercase()
            val normArtist = it.artist.trim().lowercase()
            val durBucket = it.durationMs / 2000L
            "${normTitle}::${normArtist}::${durBucket}"
        }
        if (distinctSongs.isNotEmpty()) {
            val entities = distinctSongs.map { SongEntity.fromModel(it) }
            deduplicateAndInsertSongs(entities)

            val path = "SAF_FILES"
            val existing = addedFolderDao.getFolderByPath(path)
            val currentSongsInFolder = getSongsListForFolder(path, "Imported Files")
            val newCount = currentSongsInFolder.size
            val folderId = addedFolderDao.insertFolder(
                AddedFolderEntity(
                    id = existing?.id ?: 0L,
                    name = "Imported Files",
                    path = path,
                    songCount = newCount
                )
            )
            val inserted = addedFolderDao.getFolderById(if (existing != null) existing.id else folderId)
            if (inserted != null) {
                resolveAndSaveArtwork(inserted.toModel(), currentSongsInFolder, forceWeb = false)
            }
        }
        distinctSongs.size
    }

    suspend fun deleteAddedFolder(folderId: Long, folderPath: String, deleteSongs: Boolean = true) = withContext(Dispatchers.IO) {
        addedFolderDao.deleteFolder(folderId)
        if (deleteSongs) {
            songDao.deleteSongsByFolderPath(folderPath)
        }
    }

    suspend fun toggleFavorite(songId: Long, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        songDao.setFavorite(songId, !currentFavorite)
    }

    suspend fun recordSongPlayed(songId: Long) = withContext(Dispatchers.IO) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun downloadThumbnailForSong(song: Song): String? = withContext(Dispatchers.IO) {
        val path = youTubeThumbnailHelper.fetchAndSaveThumbnail(song)
        if (path != null) {
            songDao.updateSongArtwork(song.id, path)
        }
        path
    }

    suspend fun downloadThumbnailsForSongs(
        songs: List<Song>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        var updatedCount = 0
        songs.forEachIndexed { index, song ->
            val path = youTubeThumbnailHelper.fetchAndSaveThumbnail(song)
            if (path != null) {
                songDao.updateSongArtwork(song.id, path)
                updatedCount++
            }
            onProgress?.invoke(index + 1, songs.size)
        }
        updatedCount
    }

    suspend fun getLyricsForSong(song: Song, forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        val existingDbLyrics = if (!forceRefresh) songDao.getSongLyrics(song.id) else null
        if (!existingDbLyrics.isNullOrBlank()) {
            return@withContext existingDbLyrics
        }

        val resolved = lyricsHelper.resolveLyrics(song, forceRefresh = forceRefresh)
        if (!resolved.isNullOrBlank()) {
            songDao.updateSongLyrics(song.id, resolved)
        }
        resolved
    }
}
