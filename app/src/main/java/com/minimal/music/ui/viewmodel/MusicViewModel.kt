package com.minimal.music.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minimal.music.data.model.AddedFolder
import com.minimal.music.data.model.Playlist
import com.minimal.music.data.model.Song
import com.minimal.music.data.repository.MusicRepository
import com.minimal.music.data.source.ParsedLyrics
import com.minimal.music.player.PlaybackManager
import com.minimal.music.player.PlaybackState
import com.minimal.music.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val prefs = application.getSharedPreferences("minimal_music_prefs", android.content.Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            val saved = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            ThemeMode.valueOf(saved)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    private val _openLyricsByDefault = MutableStateFlow(
        prefs.getBoolean("open_lyrics_by_default", true)
    )
    val openLyricsByDefault: StateFlow<Boolean> = _openLyricsByDefault.asStateFlow()

    fun setOpenLyricsByDefault(open: Boolean) {
        _openLyricsByDefault.value = open
        prefs.edit().putBoolean("open_lyrics_by_default", open).apply()
    }

    val playbackManager = PlaybackManager(
        context = application,
        scope = viewModelScope,
        onSongPlayedCallback = { songId ->
            repository.recordSongPlayed(songId)
        }
    )

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Song>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = repository.mostPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPopular: StateFlow<List<Song>> = repository.mostPopular
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val addedFolders: StateFlow<List<AddedFolder>> = repository.addedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderTrackMap: StateFlow<Map<Long, List<Song>>> = combine(allSongs, addedFolders) { songs, folders ->
        val map = mutableMapOf<Long, List<Song>>()
        for (folder in folders) {
            val targetPath = folder.path.trimEnd('/', '\\')
            val matched = songs.filter { song ->
                val songPath = song.folderPath?.trimEnd('/', '\\')
                val pathMatch = songPath != null && (
                    songPath.equals(targetPath, ignoreCase = true) ||
                    songPath.startsWith("$targetPath/", ignoreCase = true) ||
                    songPath.startsWith("$targetPath\\", ignoreCase = true)
                )
                val nameMatch = !song.folderName.isNullOrBlank() &&
                    song.folderName.equals(folder.name, ignoreCase = true) &&
                    (songPath == null || songPath.contains(folder.name, ignoreCase = true))

                pathMatch || nameMatch
            }.distinctBy {
                val normTitle = it.title.trim().lowercase()
                val normArtist = it.artist.trim().lowercase()
                val durBucket = it.durationMs / 2000L
                "${normTitle}::${normArtist}::${durBucket}"
            }
            map[folder.id] = matched
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _selectedFolderFilter = MutableStateFlow<AddedFolder?>(null)
    val selectedFolderFilter: StateFlow<AddedFolder?> = _selectedFolderFilter.asStateFlow()

    private val _openedFolder = MutableStateFlow<AddedFolder?>(null)
    val openedFolder: StateFlow<AddedFolder?> = _openedFolder.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // File Explorer State
    private val _currentDirectory = MutableStateFlow<java.io.File?>(null)
    val currentDirectory: StateFlow<java.io.File?> = _currentDirectory.asStateFlow()

    private val _directoryItems = MutableStateFlow<List<com.minimal.music.data.source.FileExplorerItem>>(emptyList())
    val directoryItems: StateFlow<List<com.minimal.music.data.source.FileExplorerItem>> = _directoryItems.asStateFlow()

    private val _isLoadingDirectory = MutableStateFlow(false)
    val isLoadingDirectory: StateFlow<Boolean> = _isLoadingDirectory.asStateFlow()

    val commonRoots: List<java.io.File> = repository.fileExplorerHelper.getCommonRoots()

    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics: StateFlow<String?> = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Song>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchSongs(query.trim())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
        }
        viewModelScope.launch {
            var lastSongId: Long? = null
            playbackState.collect { state ->
                val song = state.currentSong
                if (song != null && song.id != lastSongId) {
                    lastSongId = song.id
                    loadLyricsForSong(song)
                } else if (song == null) {
                    lastSongId = null
                    _currentLyrics.value = null
                }
            }
        }
        val defaultRoot = commonRoots.firstOrNull()
        if (defaultRoot != null) {
            navigateTo(defaultRoot)
        }
    }

    fun navigateTo(folder: java.io.File) {
        _currentDirectory.value = folder
        loadDirectory(folder)
    }

    fun navigateUp(): Boolean {
        val current = _currentDirectory.value ?: return false
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            navigateTo(parent)
            return true
        }
        return false
    }

    private fun loadDirectory(folder: java.io.File) {
        viewModelScope.launch {
            _isLoadingDirectory.value = true
            val items = repository.fileExplorerHelper.listDirectory(folder)
            _directoryItems.value = items
            _isLoadingDirectory.value = false
        }
    }

    fun addFolderToLibrary(folder: java.io.File, onFinished: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.addFolderToLibrary(folder)
            onFinished?.invoke(count)
        }
    }

    fun addSingleSongToLibrary(song: Song, onFinished: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.addCustomSongs(listOf(song))
            onFinished?.invoke(count > 0)
        }
    }

    fun importFromTreeUri(treeUri: android.net.Uri, onFinished: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.importFromTreeUri(treeUri)
            onFinished?.invoke(count)
        }
    }

    fun importFilesFromUris(uris: List<android.net.Uri>, onFinished: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.importFilesFromUris(uris)
            onFinished?.invoke(count)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFolderFilter(folder: AddedFolder?) {
        _selectedFolderFilter.value = if (_selectedFolderFilter.value?.id == folder?.id && folder != null) {
            null
        } else {
            folder
        }
    }

    fun openFolder(folder: AddedFolder) {
        _openedFolder.value = folder
        _selectedFolderFilter.value = folder
    }

    fun closeFolder() {
        _openedFolder.value = null
        _selectedFolderFilter.value = null
    }

    fun confirmFolderArtwork(folderId: Long, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.confirmFolderArtwork(folderId)
            // Refresh opened folder if active
            if (_openedFolder.value?.id == folderId) {
                _openedFolder.value = _openedFolder.value?.copy(isArtworkConfirmed = true)
            }
            onDone?.invoke()
        }
    }

    fun tryAgainFolderArtwork(folder: AddedFolder, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.cycleFolderArtwork(folder)
            if (result != null && _openedFolder.value?.id == folder.id) {
                _openedFolder.value = _openedFolder.value?.copy(
                    artworkUri = result.artworkUri,
                    isArtworkConfirmed = false,
                    candidateIndex = result.candidateIndex
                )
            }
            onDone?.invoke()
        }
    }

    fun refreshFolderArtwork(folder: AddedFolder, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val songs = repository.getSongsListForFolder(folder.path, folder.name)
            val result = repository.resolveAndSaveArtwork(folder, songs, forceWeb = true)
            if (_openedFolder.value?.id == folder.id) {
                _openedFolder.value = _openedFolder.value?.copy(
                    artworkUri = result.artworkUri,
                    isArtworkConfirmed = result.isConfirmed,
                    candidateUrls = result.candidateUrls,
                    candidateIndex = result.candidateIndex
                )
            }
            onDone?.invoke()
        }
    }

    fun playFolder(folder: AddedFolder, shuffle: Boolean = false) {
        val folderSongs = folderTrackMap.value[folder.id] ?: emptyList()
        if (folderSongs.isNotEmpty()) {
            if (shuffle) {
                playQueue(folderSongs.shuffled(), 0)
            } else {
                playQueue(folderSongs, 0)
            }
        }
    }

    fun removeAddedFolder(
        folder: AddedFolder,
        deleteSongs: Boolean = true,
        onFinished: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.deleteAddedFolder(folder.id, folder.path, deleteSongs)
            if (_selectedFolderFilter.value?.id == folder.id) {
                _selectedFolderFilter.value = null
            }
            if (_openedFolder.value?.id == folder.id) {
                _openedFolder.value = null
            }
            onFinished?.invoke()
        }
    }

    fun clearAllAddedFolders(onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.clearAllAddedFolders()
            _selectedFolderFilter.value = null
            _openedFolder.value = null
            onFinished?.invoke()
        }
    }

    val sleepTimerRemainingMs: StateFlow<Long?> = playbackManager.sleepTimerRemainingMs
    val isSleepTimerEndOfSong: StateFlow<Boolean> = playbackManager.isSleepTimerEndOfSong

    private val _isMiniPlayerDismissed = MutableStateFlow(false)
    val isMiniPlayerDismissed: StateFlow<Boolean> = _isMiniPlayerDismissed.asStateFlow()

    fun dismissMiniPlayer() {
        _isMiniPlayerDismissed.value = true
        playbackManager.pause()
    }

    fun restoreMiniPlayer() {
        _isMiniPlayerDismissed.value = false
    }

    fun startSleepTimer(minutes: Int) = playbackManager.startSleepTimer(minutes)
    fun startSleepTimerEndOfSong() = playbackManager.startSleepTimerEndOfSong()
    fun adjustSleepTimer(additionalMinutes: Int) = playbackManager.adjustSleepTimer(additionalMinutes)
    fun cancelSleepTimer() = playbackManager.cancelSleepTimer()

    fun playSong(song: Song, queue: List<Song>? = null) {
        _isMiniPlayerDismissed.value = false
        val effectiveQueue = if (!queue.isNullOrEmpty() && queue.any { it.id == song.id }) {
            queue
        } else {
            allSongs.value.takeIf { list -> list.any { it.id == song.id } } ?: listOf(song)
        }
        playbackManager.playSong(song, effectiveQueue)
    }

    fun playQueue(queue: List<Song>, startIndex: Int = 0) {
        _isMiniPlayerDismissed.value = false
        playbackManager.playQueue(queue, startIndex)
    }

    fun togglePlayPause() {
        if (_isMiniPlayerDismissed.value) {
            _isMiniPlayerDismissed.value = false
        }
        playbackManager.togglePlayPause()
    }

    fun skipNext() {
        playbackManager.skipNext()
    }

    fun skipPrevious(forcePrevious: Boolean = false) {
        playbackManager.skipPrevious(forcePrevious)
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playbackManager.toggleRepeatMode()
    }

    fun toggleFavorite(song: Song) {
        val newFavorite = !song.isFavorite
        playbackManager.updateSongFavorite(song.id, newFavorite)
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
        }
    }

    fun createPlaylist(name: String, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return repository.getSongsForPlaylist(playlistId)
    }

    fun syncDeviceAudio(onFinished: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.scanAndSyncDeviceAudio()
            onFinished?.invoke(count)
        }
    }

    fun downloadThumbnailForSong(song: Song, onDone: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val path = repository.downloadThumbnailForSong(song)
            onDone?.invoke(path != null)
        }
    }

    fun downloadThumbnailsForSongs(
        songs: List<Song>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
        onDone: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val count = repository.downloadThumbnailsForSongs(songs, onProgress)
            onDone?.invoke(count)
        }
    }

    fun parseLyrics(raw: String?): ParsedLyrics {
        return repository.lyricsHelper.parseLyrics(raw)
    }

    fun loadLyricsForSong(song: Song, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            val lyrics = repository.getLyricsForSong(song, forceRefresh = forceRefresh)
            if (playbackState.value.currentSong?.id == song.id) {
                _currentLyrics.value = lyrics
            }
            _isLoadingLyrics.value = false
        }
    }

    fun retryLyricsForCurrentSong() {
        val current = playbackState.value.currentSong ?: return
        loadLyricsForSong(current, forceRefresh = true)
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
