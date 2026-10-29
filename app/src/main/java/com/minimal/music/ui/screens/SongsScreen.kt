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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.minimal.music.data.model.Song
import com.minimal.music.ui.components.MinimalSearchBar
import com.minimal.music.ui.components.SongListItem
import com.minimal.music.ui.theme.PrimaryViolet
import com.minimal.music.ui.theme.SurfaceDark
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.theme.TextSecondary
import com.minimal.music.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@Composable
fun SongsScreen(
    viewModel: MusicViewModel,
    snackbarHostState: SnackbarHostState,
    onSongSelectedForPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val scope = rememberCoroutineScope()

    val displaySongs = if (searchQuery.isNotBlank()) searchResults else allSongs

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Title and actions header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "All Songs",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "${allSongs.size} tracks in library",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Rescan device button
                IconButton(
                    onClick = {
                        viewModel.syncDeviceAudio { count ->
                            scope.launch {
                                snackbarHostState.showSnackbar("Synced $count tracks from device")
                            }
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Rescan device files",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Shuffle all
                IconButton(
                    onClick = {
                        if (allSongs.isNotEmpty()) {
                            viewModel.playQueue(allSongs.shuffled(), 0)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle All",
                        tint = androidx.compose.ui.graphics.Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        MinimalSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChanged(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (displaySongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No songs found for '$searchQuery'" else "No songs found",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    items = displaySongs,
                    key = { it.id }
                ) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = playbackState.isPlaying,
                        isCurrentSong = playbackState.currentSong?.id == song.id,
                        onSongClick = { viewModel.playSong(song, displaySongs) },
                        onFavoriteToggle = { viewModel.toggleFavorite(song) },
                        onAddToPlaylistClick = { onSongSelectedForPlaylist(song) },
                        showPlayCount = true
                    )
                }
            }
        }
    }
}
