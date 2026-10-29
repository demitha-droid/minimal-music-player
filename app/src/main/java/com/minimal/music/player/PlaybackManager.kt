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
                    skipNext()
                }
                Player.STATE_READY -> {
                    val duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                    _playbackState.update { it.copy(durationMs = duration) }
                }
                else -> Unit
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentSongRecordedForPlayCount = false
            val controller = mediaController ?: return
            val currentIndex = controller.currentMediaItemIndex
            val queue = _playbackState.value.queue

            val currentSong = if (currentIndex in queue.indices) {
                queue[currentIndex]
            } else {
                null
            }

            _playbackState.update {
                it.copy(
                    currentSong = currentSong,
                    queueIndex = currentIndex,
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
        val queue = if (contextQueue != null && contextQueue.contains(song)) {
            contextQueue
        } else {
            listOf(song)
        }
        val index = queue.indexOf(song).coerceAtLeast(0)
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
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else {
            val state = _playbackState.value
            if (state.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty()) {
                controller.seekTo(0, 0L)
                controller.play()
            }
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val currentShuffle = _playbackState.value.isShuffle
        val newShuffle = !currentShuffle
        controller.shuffleModeEnabled = newShuffle
        _playbackState.update { it.copy(isShuffle = newShuffle) }
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

    fun release() {
        stopProgressTicker()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
