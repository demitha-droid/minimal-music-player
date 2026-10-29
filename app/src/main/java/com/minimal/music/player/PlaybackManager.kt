package com.minimal.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.minimal.music.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSongPlayedCallback: (suspend (Long) -> Unit)? = null
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var progressJob: Job? = null
    private var currentSongRecordedForPlayCount = false

    // Sleep timer state
    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    private val _isSleepTimerEndOfSong = MutableStateFlow(false)
    val isSleepTimerEndOfSong: StateFlow<Boolean> = _isSleepTimerEndOfSong.asStateFlow()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()?.apply {
                    addListener(playerListener)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressTicker()
            } else {
                stopProgressTicker()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_ENDED -> {
                    checkAndRecordPlayCount(forced = true)
                    if (_isSleepTimerEndOfSong.value) {
                        cancelSleepTimer()
                        pause()
                    } else {
                        skipNext()
                    }
                }
                Player.STATE_READY -> {
                    val duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                    _playbackState.update { it.copy(durationMs = duration) }
                }
                else -> Unit
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_isSleepTimerEndOfSong.value && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                cancelSleepTimer()
                pause()
                return
            }

            currentSongRecordedForPlayCount = false
            val controller = mediaController ?: return
            val currentIndex = controller.currentMediaItemIndex
            val queue = _playbackState.value.queue

            // Match by mediaId first to guarantee accurate queue index
            val songId = mediaItem?.mediaId?.toLongOrNull()
            val matchedIndex = if (songId != null) {
                queue.indexOfFirst { it.id == songId }
            } else {
                -1
            }
            val resolvedIndex = if (matchedIndex >= 0) matchedIndex else currentIndex

            val currentSong = if (resolvedIndex in queue.indices) {
                queue[resolvedIndex]
            } else {
                null
            }

            _playbackState.update {
                it.copy(
                    currentSong = currentSong,
                    queueIndex = resolvedIndex,
                    currentPositionMs = 0L,
                    durationMs = controller.duration.coerceAtLeast(0L)
                )
            }
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val currentPos = controller.currentPosition.coerceAtLeast(0L)
                    val duration = controller.duration.coerceAtLeast(0L)
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = if (duration > 0) duration else it.durationMs
                        )
                    }

                    if (currentPos >= 30_000L) {
                        checkAndRecordPlayCount(forced = false)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun checkAndRecordPlayCount(forced: Boolean) {
        if (!currentSongRecordedForPlayCount) {
            val song = _playbackState.value.currentSong
            if (song != null) {
                currentSongRecordedForPlayCount = true
                scope.launch(Dispatchers.IO) {
                    onSongPlayedCallback?.invoke(song.id)
                }
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val controller = mediaController ?: return
        if (songs.isEmpty()) return

        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val mediaItems = songs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .build()

            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(Uri.parse(song.mediaUri))
                .setMediaMetadata(metadata)
                .build()
        }

        currentSongRecordedForPlayCount = false
        _playbackState.update {
            it.copy(
                queue = songs,
                queueIndex = safeIndex,
                currentSong = songs[safeIndex]
            )
        }

        controller.setMediaItems(mediaItems, safeIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playSong(song: Song, contextQueue: List<Song>? = null) {
        val queue = if (contextQueue != null && contextQueue.any { it.id == song.id }) {
            contextQueue
        } else {
            val currentQ = _playbackState.value.queue
            if (currentQ.any { it.id == song.id }) currentQ else listOf(song)
        }
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playQueue(queue, index)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs.coerceAtLeast(0L))
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipNext() {
        val controller = mediaController ?: return
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        val currentIndex = state.queueIndex.coerceIn(0, queue.lastIndex)
        val nextIndex = if (state.isShuffle) {
            if (queue.size > 1) {
                val others = queue.indices.filter { it != currentIndex }
                others.random()
            } else {
                0
            }
        } else {
            if (currentIndex + 1 < queue.size) {
                currentIndex + 1
            } else {
                0 // Loop back to start
            }
        }

        val nextSong = queue[nextIndex]
        currentSongRecordedForPlayCount = false

        _playbackState.update {
            it.copy(
                currentSong = nextSong,
                queueIndex = nextIndex,
                currentPositionMs = 0L,
                isPlaying = true
            )
        }

        if (nextIndex < controller.mediaItemCount) {
            controller.seekTo(nextIndex, 0L)
            controller.play()
        } else {
            playQueue(queue, nextIndex)
        }
    }

    fun skipPrevious(forcePrevious: Boolean = false) {
        val controller = mediaController ?: return
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        if (!forcePrevious && controller.currentPosition > 3000L) {
            controller.seekTo(0L)
            _playbackState.update { it.copy(currentPositionMs = 0L) }
            return
        }

        val currentIndex = state.queueIndex.coerceIn(0, queue.lastIndex)
        val prevIndex = if (state.isShuffle) {
            if (queue.size > 1) {
                val others = queue.indices.filter { it != currentIndex }
                others.random()
            } else {
                0
            }
        } else {
            if (currentIndex - 1 >= 0) {
                currentIndex - 1
            } else {
                queue.lastIndex
            }
        }

        val prevSong = queue[prevIndex]
        currentSongRecordedForPlayCount = false

        _playbackState.update {
            it.copy(
                currentSong = prevSong,
                queueIndex = prevIndex,
                currentPositionMs = 0L,
                isPlaying = true
            )
        }

        if (prevIndex < controller.mediaItemCount) {
            controller.seekTo(prevIndex, 0L)
            controller.play()
        } else {
            playQueue(queue, prevIndex)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val currentShuffle = _playbackState.value.isShuffle
        val newShuffle = !currentShuffle
        controller.shuffleModeEnabled = newShuffle
        _playbackState.update { it.copy(isShuffle = newShuffle) }
    }

    fun updateSongFavorite(songId: Long, isFavorite: Boolean) {
        _playbackState.update { state ->
            val updatedCurrent = if (state.currentSong?.id == songId) {
                state.currentSong.copy(isFavorite = isFavorite)
            } else {
                state.currentSong
            }
            val updatedQueue = state.queue.map {
                if (it.id == songId) it.copy(isFavorite = isFavorite) else it
            }
            state.copy(
                currentSong = updatedCurrent,
                queue = updatedQueue
            )
        }
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        val playerRepeatMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        controller.repeatMode = playerRepeatMode
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun stop() {
        cancelSleepTimer()
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _playbackState.update {
            it.copy(
                isPlaying = false,
                currentSong = null,
                currentPositionMs = 0L,
                queue = emptyList()
            )
        }
    }

    // Sleep Timer Controls
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        val totalMs = minutes * 60 * 1000L
        val targetTime = System.currentTimeMillis() + totalMs
        _sleepTimerRemainingMs.value = totalMs

        sleepTimerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val remaining = (targetTime - System.currentTimeMillis()).coerceAtLeast(0L)
                _sleepTimerRemainingMs.value = remaining
                if (remaining <= 0L) {
                    _sleepTimerRemainingMs.value = null
                    pause()
                    break
                }
                delay(1000L)
            }
        }
    }

    fun startSleepTimerEndOfSong() {
        cancelSleepTimer()
        _isSleepTimerEndOfSong.value = true
    }

    fun adjustSleepTimer(additionalMinutes: Int) {
        val currentRemaining = _sleepTimerRemainingMs.value ?: return
        val newTotal = (currentRemaining + (additionalMinutes * 60 * 1000L)).coerceAtLeast(60 * 1000L)
        val minutes = (newTotal / (60 * 1000L)).toInt().coerceAtLeast(1)
        startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
        _isSleepTimerEndOfSong.value = false
    }

    fun release() {
        cancelSleepTimer()
        stopProgressTicker()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
