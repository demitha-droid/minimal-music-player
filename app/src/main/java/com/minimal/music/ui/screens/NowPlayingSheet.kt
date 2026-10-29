package com.minimal.music.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.music.player.PlaybackState
import com.minimal.music.player.RepeatMode as AppRepeatMode
import com.minimal.music.ui.components.MinimalArtwork
import com.minimal.music.ui.components.SleepTimerSheet
import com.minimal.music.ui.theme.BgDark
import com.minimal.music.ui.theme.HeartActive
import com.minimal.music.ui.theme.PrimaryViolet
import com.minimal.music.ui.theme.SurfaceBorder
import com.minimal.music.ui.theme.SurfaceElevated
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.theme.TextSecondary
import com.minimal.music.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    playbackState: PlaybackState,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isUserSeeking by remember { mutableStateOf(false) }
    var seekPositionRatio by remember { mutableFloatStateOf(0f) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }

    val remainingMs by viewModel.sleepTimerRemainingMs.collectAsState()
    val isEndOfSong by viewModel.isSleepTimerEndOfSong.collectAsState()

    // Vinyl rotation animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgDark,
        dragHandle = null,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.bodySmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                )

                IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) HeartActive else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vinyl / Album Cover Artwork
            Box(
                modifier = Modifier
                    .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF282835),
                                Color(0xFF14141B),
                                Color(0xFF09090D)
                            )
                        )
                    )
                    .border(2.dp, SurfaceBorder, CircleShape)
                    .rotate(if (playbackState.isPlaying) rotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves rings
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .border(1.dp, Color(0xFF20202A), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(1.dp, Color(0xFF252532), CircleShape)
                )

                // Center Label
                MinimalArtwork(
                    title = song.title,
                    size = 100.dp,
                    shapeRadius = 50.dp
                )

                // Center Spindle hole
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(BgDark)
                        .border(1.dp, SurfaceBorder, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Artist Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider & Timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentRatio = if (isUserSeeking) seekPositionRatio else playbackState.progress

                Slider(
                    value = currentRatio,
                    onValueChange = {
                        isUserSeeking = true
                        seekPositionRatio = it
                    },
                    onValueChangeFinished = {
                        val targetMs = (seekPositionRatio * playbackState.durationMs).toLong()
                        viewModel.seekTo(targetMs)
                        isUserSeeking = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryViolet,
                        activeTrackColor = PrimaryViolet,
                        inactiveTrackColor = SurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayedPositionMs = if (isUserSeeking) {
                        (seekPositionRatio * playbackState.durationMs).toLong()
                    } else {
                        playbackState.currentPositionMs
                    }

                    Text(
                        text = formatTime(displayedPositionMs),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                    Text(
                        text = formatTime(playbackState.durationMs),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffle) PrimaryViolet else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = { viewModel.skipPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Large Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet)
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = { viewModel.skipNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    val repeatIcon = when (playbackState.repeatMode) {
                        AppRepeatMode.ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    }
                    val repeatTint = when (playbackState.repeatMode) {
                        AppRepeatMode.OFF -> TextMuted
                        else -> PrimaryViolet
                    }
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repeat",
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Row: Sleep Timer & Queue index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isTimerActive = remainingMs != null || isEndOfSong

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isTimerActive) Color(0xFF2B2240) else SurfaceElevated)
                        .border(
                            1.dp,
                            if (isTimerActive) PrimaryViolet else SurfaceBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { showSleepTimerSheet = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = "Sleep Timer",
                        tint = if (isTimerActive) PrimaryViolet else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when {
                            isEndOfSong -> "End of Song"
                            remainingMs != null -> {
                                val totalSec = (remainingMs!! / 1000).coerceAtLeast(0)
                                val m = totalSec / 60
                                val s = totalSec % 60
                                "%d:%02d remaining".format(m, s)
                            }
                            else -> "Sleep Timer"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isTimerActive) PrimaryViolet else TextSecondary,
                            fontWeight = if (isTimerActive) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }

                val queue = playbackState.queue
                val currentIdx = playbackState.queueIndex
                if (queue.isNotEmpty()) {
                    Text(
                        text = "${currentIdx + 1} / ${queue.size}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showSleepTimerSheet) {
            SleepTimerSheet(
                remainingMs = remainingMs,
                isEndOfSong = isEndOfSong,
                onStartTimer = { viewModel.startSleepTimer(it) },
                onStartEndOfSong = { viewModel.startSleepTimerEndOfSong() },
                onAdjustTimer = { viewModel.adjustSleepTimer(it) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { showSleepTimerSheet = false }
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
