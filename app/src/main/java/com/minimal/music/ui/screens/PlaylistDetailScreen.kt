package com.minimal.music.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.minimal.music.data.model.Song
import com.minimal.music.ui.components.SongListItem
import com.minimal.music.ui.theme.PrimaryViolet
import com.minimal.music.ui.theme.SurfaceDark
import com.minimal.music.ui.theme.SurfaceElevated
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.theme.TextSecondary
import com.minimal.music.ui.viewmodel.MusicViewModel

@Composable
fun PlaylistDetailScreen(
    title: String,
    playlistId: Long?,
    songs: List<Song>,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by viewModel.playbackState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Back button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${songs.size} tracks",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Play All & Shuffle Buttons
        if (songs.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.playQueue(songs, 0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All", color = Color.Black, style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    onClick = { viewModel.playQueue(songs.shuffled(), 0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracks in this playlist yet",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = playbackState.isPlaying,
                        isCurrentSong = playbackState.currentSong?.id == song.id,
                        onSongClick = { viewModel.playSong(song, songs) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song) },
                        onRemoveFromPlaylistClick = if (playlistId != null) {
                            { viewModel.removeSongFromPlaylist(playlistId, song.id) }
                        } else null,
                        showPlayCount = true
                    )
                }
            }
        }
    }
}
