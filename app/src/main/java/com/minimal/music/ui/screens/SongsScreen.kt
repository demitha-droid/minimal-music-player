package com.minimal.music.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.music.data.model.AddedFolder
import com.minimal.music.data.model.Song
import com.minimal.music.ui.components.MinimalSearchBar
import com.minimal.music.ui.components.SongListItem
import com.minimal.music.ui.theme.PrimaryViolet
import androidx.compose.material.icons.rounded.Add
import com.minimal.music.ui.theme.SecondaryCyan
import com.minimal.music.ui.theme.SurfaceBorder
import com.minimal.music.ui.theme.SurfaceDark
import com.minimal.music.ui.theme.SurfaceElevated
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
    val addedFolders by viewModel.addedFolders.collectAsState()
    val selectedFolderFilter by viewModel.selectedFolderFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val scope = rememberCoroutineScope()

    // SAF folder picker directly from the Songs tab
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFromTreeUri(uri) { count ->
                scope.launch {
                    snackbarHostState.showSnackbar("Added $count songs from folder to library")
                }
            }
        }
    }

    val displaySongs = when {
        searchQuery.isNotBlank() -> searchResults
        selectedFolderFilter != null -> allSongs.filter {
            it.folderPath == selectedFolderFilter?.path ||
                (it.folderName != null && it.folderName == selectedFolderFilter?.name)
        }
        else -> allSongs
    }

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
                    text = "Songs",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = if (selectedFolderFilter != null) {
                        "${displaySongs.size} tracks in folder"
                    } else {
                        "${allSongs.size} tracks • folders first"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add Folder Shortcut
                IconButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = "Add folder to library",
                        tint = SecondaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

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

                // Shuffle all / filtered
                IconButton(
                    onClick = {
                        if (displaySongs.isNotEmpty()) {
                            viewModel.playQueue(displaySongs.shuffled(), 0)
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
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        MinimalSearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.onSearchQueryChanged(it) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ADDED FOLDERS SECTION (FIRST)
        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Added Folders",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (addedFolders.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262114))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${addedFolders.size}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFFB300),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                if (selectedFolderFilter != null) {
                    Text(
                        text = "Show All",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PrimaryViolet,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clickable { viewModel.setFolderFilter(null) }
                            .padding(4.dp)
                    )
                } else {
                    Text(
                        text = "+ Add",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryCyan,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clickable { folderPickerLauncher.launch(null) }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (addedFolders.isEmpty()) {
                // Sleek banner prompting to add folders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable { folderPickerLauncher.launch(null) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262114)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No folders added yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Tap to pick a music folder from storage",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add folder",
                        tint = SecondaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Horizontal scrolling list of folder cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    addedFolders.forEach { folder ->
                        val isSelected = selectedFolderFilter?.id == folder.id
                        AddedFolderCard(
                            folder = folder,
                            isSelected = isSelected,
                            onClick = { viewModel.setFolderFilter(folder) },
                            onPlay = { viewModel.playFolder(folder, shuffle = false) },
                            onShuffle = { viewModel.playFolder(folder, shuffle = true) },
                            onRemove = {
                                viewModel.removeAddedFolder(folder, deleteSongs = true) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Removed folder '${folder.name}' from library")
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Active filter indicator chip
        if (selectedFolderFilter != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262114))
                    .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Viewing: ${selectedFolderFilter?.name} (${displaySongs.size})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                IconButton(
                    onClick = { viewModel.setFolderFilter(null) },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear filter",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Songs List
        if (displaySongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        searchQuery.isNotBlank() -> "No songs found for '$searchQuery'"
                        selectedFolderFilter != null -> "No songs in folder '${selectedFolderFilter?.name}'"
                        else -> "No songs found"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
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

@Composable
private fun AddedFolderCard(
    folder: AddedFolder,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(175.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF241F14) else SurfaceElevated)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFB300) else SurfaceBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262114)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Folder options",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play All") },
                            onClick = {
                                showMenu = false
                                onPlay()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Shuffle") },
                            onClick = {
                                showMenu = false
                                onShuffle()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from Library", color = Color(0xFFFF5252)) },
                            onClick = {
                                showMenu = false
                                onRemove()
                            }
                        )
                    }
                }
            }

            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${folder.songCount} tracks",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isSelected) Color(0xFFFFB300) else TextMuted,
                        fontSize = 11.sp
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onShuffle,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle folder",
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(PrimaryViolet)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play folder",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
