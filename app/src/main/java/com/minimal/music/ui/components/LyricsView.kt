package com.minimal.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.music.data.source.ParsedLyrics
import com.minimal.music.ui.theme.BgDark
import com.minimal.music.ui.theme.SurfaceBorder
import com.minimal.music.ui.theme.SurfaceDark
import com.minimal.music.ui.theme.SurfaceElevated
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.theme.TextSecondary

@Composable
fun LyricsView(
    rawLyrics: String?,
    isLoading: Boolean,
    currentPositionMs: Long,
    parsedLyrics: ParsedLyrics,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Determine currently playing line index if synced
    val activeIndex = remember(currentPositionMs, parsedLyrics) {
        if (parsedLyrics.isSynced) {
            parsedLyrics.getCurrentLineIndex(currentPositionMs)
        } else {
            -1
        }
    }

    // Auto-scroll list to active line (only if user is not actively scrolling)
    LaunchedEffect(activeIndex) {
        if (parsedLyrics.isSynced && activeIndex >= 0 && parsedLyrics.lines.isNotEmpty()) {
            if (!listState.isScrollInProgress) {
                val targetIndex = (activeIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Status badge & Refresh button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (parsedLyrics.isSynced) TextPrimary else TextMuted
                            )
                    )
                    Text(
                        text = when {
                            isLoading && rawLyrics.isNullOrBlank() -> "SEARCHING"
                            parsedLyrics.isSynced -> "SYNCED LRC"
                            rawLyrics.isNullOrBlank() -> "NO LYRICS"
                            else -> "PLAIN LYRICS"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (parsedLyrics.isSynced) TextPrimary else TextSecondary
                        )
                    )
                }

                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Lyrics",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Body Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading && rawLyrics.isNullOrBlank() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = TextPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Downloading lyrics...",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                            )
                        }
                    }

                    rawLyrics.isNullOrBlank() || parsedLyrics.lines.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SearchOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "No lyrics found online",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                                    .clickable { onRetry() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Retry Search",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = parsedLyrics.lines,
                                key = { index, line -> "${line.timeMs}_$index" }
                            ) { index, line ->
                                val isCurrent = parsedLyrics.isSynced && index == activeIndex
                                val isPast = parsedLyrics.isSynced && index < activeIndex

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = parsedLyrics.isSynced && line.timeMs >= 0L) {
                                            onSeekTo(line.timeMs)
                                        }
                                        .background(
                                            if (isCurrent) SurfaceDark else Color.Transparent
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = line.text,
                                        textAlign = TextAlign.Center,
                                        style = if (isCurrent) {
                                            MaterialTheme.typography.bodyLarge.copy(
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                lineHeight = 22.sp
                                            )
                                        } else if (isPast) {
                                            MaterialTheme.typography.bodyMedium.copy(
                                                color = TextSecondary.copy(alpha = 0.55f),
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp
                                            )
                                        } else {
                                            MaterialTheme.typography.bodyMedium.copy(
                                                color = TextMuted,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Top & Bottom subtle gradient fade overlays
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(SurfaceElevated, Color.Transparent)
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, SurfaceElevated)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
