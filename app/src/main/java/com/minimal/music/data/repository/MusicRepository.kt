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
import com.minimal.music.data.source.MediaStoreScanner
import com.minimal.music.data.source.PreseededTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
        val count = songDao.getSongCount()
        if (count == 0) {
            val preseededEntities = PreseededTracks.tracks.map { SongEntity.fromModel(it) }
            songDao.insertSongs(preseededEntities)

            // Insert initial folder for preseeded tracks
            addedFolderDao.insertFolder(
                AddedFolderEntity(
                    name = "Featured Music",
                    path = "FEATURED_MUSIC",
                    songCount = PreseededTracks.tracks.size,
                    isArtworkConfirmed = true
                )
            )

            // Create initial default playlists
            val chillVibesId = playlistDao.insertPlaylist(PlaylistEntity(name = "Chill Vibes"))
            if (chillVibesId > 0) {
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(chillVibesId, 1001L))
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(chillVibesId, 1003L))
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(chillVibesId, 1006L))
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(chillVibesId, 1008L))
            }

            val lateNightId = playlistDao.insertPlaylist(PlaylistEntity(name = "Night Drive"))
            if (lateNightId > 0) {
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(lateNightId, 1002L))
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(lateNightId, 1004L))
                playlistDao.addSongToPlaylist(PlaylistSongCrossRef(lateNightId, 1007L))
            }
        }

        // Clean slate migration: clear previously auto-populated folders so only manual folders appear
        val prefs = context.getSharedPreferences("minimal_music_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("clean_slate_v1", false)) {
            addedFolderDao.clearAll()
            prefs.edit().putBoolean("clean_slate_v1", true).apply()
        }

        // Check for any manually added folders that need artwork resolution
        val allFolders = addedFolderDao.getAllFoldersList()
        for (folderEntity in allFolders) {
            if (folderEntity.artworkUri.isNullOrBlank()) {
                val model = folderEntity.toModel()
                val songsInFolder = getSongsListForFolder(model.path, model.name)
                resolveAndSaveArtwork(model, songsInFolder, forceWeb = false)
            }
        }
    }

    suspend fun clearAllAddedFolders(): Int = withContext(Dispatchers.IO) {
        addedFolderDao.clearAll()
    }

    suspend fun getSongsListForFolder(folderPath: String, folderName: String): List<Song> = withContext(Dispatchers.IO) {
        val all = songDao.getAllSongsList()
        all.filter {
            it.folderPath == folderPath || (it.folderName != null && it.folderName == folderName)
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

    suspend fun scanAndSyncDeviceAudio(): Int = withContext(Dispatchers.IO) {
        val deviceSongs = mediaStoreScanner.scanDeviceAudio()
        if (deviceSongs.isNotEmpty()) {
            val entities = deviceSongs.map { SongEntity.fromModel(it) }
            songDao.insertSongs(entities)
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
            songDao.insertSongs(entities)
        }
        songs.size
    }

    suspend fun addFolderToLibrary(folder: java.io.File): Int = withContext(Dispatchers.IO) {
        var songs = fileExplorerHelper.scanDirectoryRecursive(folder)
        if (songs.isEmpty()) {
            songs = mediaStoreScanner.scanFolder(folder.absolutePath)
        }
        if (songs.isNotEmpty()) {
            val entities = songs.map { SongEntity.fromModel(it) }
            songDao.insertSongs(entities)

            val existing = addedFolderDao.getFolderByPath(folder.absolutePath)
            val folderId = addedFolderDao.insertFolder(
                AddedFolderEntity(
                    id = existing?.id ?: 0L,
                    name = folder.name,
                    path = folder.absolutePath,
                    songCount = songs.size
                )
            )
            val inserted = addedFolderDao.getFolderById(if (existing != null) existing.id else folderId)
            if (inserted != null) {
                resolveAndSaveArtwork(inserted.toModel(), songs, forceWeb = false)
            }
        }
        songs.size
    }

    suspend fun importFromTreeUri(treeUri: android.net.Uri): Int = withContext(Dispatchers.IO) {
        try {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: Exception) {}

        val songs = fileExplorerHelper.scanTreeUriRecursive(treeUri)
        if (songs.isNotEmpty()) {
            val entities = songs.map { SongEntity.fromModel(it) }
            songDao.insertSongs(entities)

            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            val folderName = doc?.name ?: "Imported Folder"
            val path = treeUri.toString()
            val existing = addedFolderDao.getFolderByPath(path)
            val folderId = addedFolderDao.insertFolder(
                AddedFolderEntity(
                    id = existing?.id ?: 0L,
                    name = folderName,
                    path = path,
                    songCount = songs.size
                )
            )
            val inserted = addedFolderDao.getFolderById(if (existing != null) existing.id else folderId)
            if (inserted != null) {
                resolveAndSaveArtwork(inserted.toModel(), songs, forceWeb = false)
            }
        }
        songs.size
    }

    suspend fun importFilesFromUris(uris: List<android.net.Uri>): Int = withContext(Dispatchers.IO) {
        val songs = fileExplorerHelper.parseSongsFromUris(uris)
        if (songs.isNotEmpty()) {
            val entities = songs.map { SongEntity.fromModel(it) }
            songDao.insertSongs(entities)

            val path = "SAF_FILES"
            val existing = addedFolderDao.getFolderByPath(path)
            val newCount = (existing?.songCount ?: 0) + songs.size
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
                resolveAndSaveArtwork(inserted.toModel(), songs, forceWeb = false)
            }
        }
        songs.size
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
}
