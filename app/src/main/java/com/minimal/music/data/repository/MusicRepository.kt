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
import com.minimal.music.data.source.MediaStoreScanner
import com.minimal.music.data.source.PreseededTracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

            // Create initial default playlist
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

        // Backfill added_folders if missing
        if (addedFolderDao.getFolderCount() == 0) {
            val allSongsList = songDao.getAllSongsList()
            val folderSongs = allSongsList.filter { !it.folderPath.isNullOrBlank() }
            val groups = folderSongs.groupBy { it.folderPath!! }
            for ((path, groupSongs) in groups) {
                val name = groupSongs.firstOrNull()?.folderName ?: "Folder"
                addedFolderDao.insertFolder(
                    AddedFolderEntity(
                        name = name,
                        path = path,
                        songCount = groupSongs.size
                    )
                )
            }
        }
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

            // Register parent folders
            val folderGroups = songs.groupBy { it.folderPath ?: "" }
            for ((path, groupSongs) in folderGroups) {
                if (path.isNotBlank()) {
                    val folderName = groupSongs.firstOrNull()?.folderName ?: "Folder"
                    val existing = addedFolderDao.getFolderByPath(path)
                    val newCount = (existing?.songCount ?: 0) + groupSongs.size
                    addedFolderDao.insertFolder(
                        AddedFolderEntity(
                            id = existing?.id ?: 0L,
                            name = folderName,
                            path = path,
                            songCount = newCount
                        )
                    )
                }
            }
        }
        songs.size
    }

    suspend fun addFolderToLibrary(folder: java.io.File): Int = withContext(Dispatchers.IO) {
        val songs = fileExplorerHelper.scanDirectoryRecursive(folder)
        if (songs.isNotEmpty()) {
            val entities = songs.map { SongEntity.fromModel(it) }
            songDao.insertSongs(entities)

            val existing = addedFolderDao.getFolderByPath(folder.absolutePath)
            addedFolderDao.insertFolder(
                AddedFolderEntity(
                    id = existing?.id ?: 0L,
                    name = folder.name,
                    path = folder.absolutePath,
                    songCount = songs.size
                )
            )
        }
        songs.size
    }

    suspend fun importFromTreeUri(treeUri: android.net.Uri): Int = withContext(Dispatchers.IO) {
        val songs = fileExplorerHelper.scanTreeUriRecursive(treeUri)
        if (songs.isNotEmpty()) {
            val entities = songs.map { SongEntity.fromModel(it) }
            songDao.insertSongs(entities)

            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
            val folderName = doc?.name ?: "Imported Folder"
            val path = treeUri.toString()
            val existing = addedFolderDao.getFolderByPath(path)
            addedFolderDao.insertFolder(
                AddedFolderEntity(
                    id = existing?.id ?: 0L,
                    name = folderName,
                    path = path,
                    songCount = songs.size
                )
            )
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
            addedFolderDao.insertFolder(
                AddedFolderEntity(
                    id = existing?.id ?: 0L,
                    name = "Imported Files",
                    path = path,
                    songCount = newCount
                )
            )
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
