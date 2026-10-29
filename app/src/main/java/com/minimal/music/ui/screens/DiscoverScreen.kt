package com.minimal.music.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.music.data.model.Song
import com.minimal.music.ui.components.MinimalArtwork
import com.minimal.music.ui.components.SongListItem
import com.minimal.music.ui.theme.PrimaryAccent
import com.minimal.music.ui.theme.PrimaryViolet
import com.minimal.music.ui.theme.SecondaryCyan
import com.minimal.music.ui.theme.SurfaceBorder
import com.minimal.music.ui.theme.SurfaceDark
import com.minimal.music.ui.theme.SurfaceElevated
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.theme.TextSecondary
import com.minimal.music.ui.viewmodel.MusicViewModel

@Composable
fun DiscoverScreen(
    viewModel: MusicViewModel,
    onSongSelectedForPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val mostPopular by viewModel.mostPopular.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Title & Tagline
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Discover",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Clean, distraction-free listening",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                )
            }
        }

        // Hero Quick-Play Card
        item {
            HeroCard(
                songs = mostPopular,
                onPlayAll = {
                    if (mostPopular.isNotEmpty()) {
                        viewModel.playQueue(mostPopular, 0)
                    }
                },
                onShuffle = {
                    if (allSongs.isNotEmpty()) {
                        val shuffled = allSongs.shuffled()
                        viewModel.playQueue(shuffled, 0)
                    }
                }
            )
        }

        // Most Popular (Trending) Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Most Popular Songs",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "${mostPopular.size} tracks",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                // Horizontal Carousel of Popular Songs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    itemsIndexed(mostPopular.take(10)) { index, song ->
                        PopularSongCard(
                            rank = index + 1,
                            song = song,
                            isCurrentSong = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = { viewModel.playSong(song, mostPopular) }
                        )
                    }
                }
            }
        }

        // Most Played Section (User's personal listening history)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9100),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Most Played",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                if (mostPlayed.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Play songs to build your personal stats",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        mostPlayed.take(5).forEach { song ->
                            SongListItem(
                                song = song,
                                isPlaying = playbackState.isPlaying,
                                isCurrentSong = playbackState.currentSong?.id == song.id,
                                onSongClick = { viewModel.playSong(song, mostPlayed) },
                                onFavoriteToggle = { viewModel.toggleFavorite(song) },
                                onAddToPlaylistClick = { onSongSelectedForPlaylist(song) },
                                showPlayCount = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    songs: List<Song>,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF201633),
                        Color(0xFF141926)
                    )
                )
            )
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VIBE OF THE DAY",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PrimaryViolet,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Curated & Local Hits",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet)
                        .clickable { onPlayAll() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayAll,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Play Top", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = onShuffle,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Shuffle", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PopularSongCard(
    rank: Int,
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(136.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.TopStart) {
            MinimalArtwork(
                title = song.title,
                size = 128.dp,
                shapeRadius = 14.dp
            )

            // Rank Badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    color = if (isCurrentSong) PrimaryViolet else TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
