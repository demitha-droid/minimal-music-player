package com.minimal.music.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.minimal.music.ui.components.FolderCoverImage
import com.minimal.music.ui.components.MinimalSearchBar
import com.minimal.music.ui.components.SongListItem
import com.minimal.music.ui.theme.PrimaryViolet
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
    val openedFolder by viewModel.openedFolder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val scope = rememberCoroutineScope()

    // Handle system back gesture when inside a folder
    BackHandler(enabled = openedFolder != null) {
        viewModel.closeFolder()
    }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (openedFolder == null) {
            // =========================================================================
            // ROOT VIEW OF SONGS TAB: STRICTLY FOLDERS ONLY (NO OTHER SONGS)
            // =========================================================================

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Folders",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "${addedFolders.size} music folders in library",
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
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar for filtering folders
            MinimalSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                placeholderText = "Filter folders..."
            )

            Spacer(modifier = Modifier.height(14.dp))

            val filteredFolders = if (searchQuery.isBlank()) {
                addedFolders
            } else {
                addedFolders.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
            }

            if (addedFolders.isEmpty()) {
                // Empty state: No folders added yet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF262114)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No Music Folders",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add music folders from your device to organize your songs into clean, dedicated collections.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextMuted,
                                    lineHeight = 18.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Button(
                            onClick = { folderPickerLauncher.launch(null) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Folder from Device",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (filteredFolders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No folders match '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
            } else {
                // List of folders (strictly folders, no other songs below)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredFolders,
                        key = { it.id }
                    ) { folder ->
                        FolderOverviewCard(
                            folder = folder,
                            onClick = { viewModel.openFolder(folder) },
                            onPlay = { viewModel.playFolder(folder, shuffle = false) },
                            onShuffle = { viewModel.playFolder(folder, shuffle = true) },
                            onKeepArtwork = {
                                viewModel.confirmFolderArtwork(folder.id) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Album cover saved for ${folder.name}")
                                    }
                                }
                            },
                            onTryAgainArtwork = {
                                viewModel.tryAgainFolderArtwork(folder) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Looking for alternative cover...")
                                    }
                                }
                            },
                            onRefreshArtwork = {
                                viewModel.refreshFolderArtwork(folder) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Searching web for artwork...")
                                    }
                                }
                            },
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

        } else {
            // =========================================================================
            // FOLDER DETAIL VIEW: DISPLAY ONLY SONGS INSIDE THIS SELECTED FOLDER
            // =========================================================================
            val currentFolder = openedFolder!!
            val folderSongs = allSongs.filter {
                it.folderPath == currentFolder.path ||
                    (it.folderName != null && it.folderName == currentFolder.name)
            }

            val displayFolderSongs = if (searchQuery.isBlank()) {
                folderSongs
            } else {
                folderSongs.filter {
                    it.title.contains(searchQuery.trim(), ignoreCase = true) ||
                        it.artist.contains(searchQuery.trim(), ignoreCase = true)
                }
            }

            // Top Bar with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.closeFolder() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Folders",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = currentFolder.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folderSongs.size} tracks",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            viewModel.refreshFolderArtwork(currentFolder) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Searching web for artwork...")
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ImageSearch,
                            contentDescription = "Find web artwork",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar inside folder
            MinimalSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                placeholderText = "Search songs in ${currentFolder.name}..."
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Folder Header Card (Album Art + Pill Play/Shuffle Buttons matching folder side, zero overlaps)
            FolderHeaderBanner(
                folder = currentFolder,
                onPlay = { viewModel.playFolder(currentFolder, shuffle = false) },
                onShuffle = { viewModel.playFolder(currentFolder, shuffle = true) },
                onKeepArtwork = {
                    viewModel.confirmFolderArtwork(currentFolder.id) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Album cover saved")
                        }
                    }
                },
                onTryAgainArtwork = {
                    viewModel.tryAgainFolderArtwork(currentFolder) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Trying another album cover...")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Songs List strictly for this folder
            if (displayFolderSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "No songs match '$searchQuery' in this folder"
                        } else {
                            "No audio files found in '${currentFolder.name}'"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = displayFolderSongs,
                        key = { it.id }
                    ) { song ->
                        SongListItem(
                            song = song,
                            isPlaying = playbackState.isPlaying,
                            isCurrentSong = playbackState.currentSong?.id == song.id,
                            onSongClick = { viewModel.playSong(song, displayFolderSongs) },
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

/**
 * Folder Card on the Root Songs Tab.
 * Layout guarantees strictly ZERO overlap between elements:
 * - Dedicated artwork slot with Keep / Try Again controls if unconfirmed
 * - Folder title and song count
 * - Pill-shaped Play and Shuffle buttons matching the folder sides
 */
@Composable
private fun FolderOverviewCard(
    folder: AddedFolder,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onKeepArtwork: () -> Unit,
    onTryAgainArtwork: () -> Unit,
    onRefreshArtwork: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Section: Album Cover Art & Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Square Album Cover (Local, Web Downloaded, or Sleek Generative Fallback)
                FolderCoverImage(
                    artworkUri = folder.artworkUri,
                    folderName = folder.name,
                    size = 68.dp,
                    shapeRadius = 12.dp
                )

                // Info: Folder Title & Track Count
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${folder.songCount} tracks",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                // 3-dots Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Folder options",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open Folder") },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Search Web Cover") },
                            onClick = {
                                showMenu = false
                                onRefreshArtwork()
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

            // AUTOMATIC WEB ARTWORK CONFIRMATION (Keep / Try Again)
            // When newly found from internet and unconfirmed, show dedicated Keep & Try Again banner
            if (!folder.isArtworkConfirmed && folder.artworkUri != null) {
                ArtworkKeepOrTryAgainBanner(
                    candidateIndex = folder.candidateIndex,
                    onKeep = onKeepArtwork,
                    onTryAgain = onTryAgainArtwork
                )
            }

            // PILL-SHAPED PLAY & SHUFFLE BUTTONS MATCHING THE FOLDER SIDE
            // Clean, non-overlapping side-by-side pill buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pill Play Button
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play folder",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Pill Shuffle Button
                Button(
                    onClick = onShuffle,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle folder",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shuffle",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

/**
 * Top Header Banner inside an Opened Folder.
 * Displays large album artwork, Keep/Try Again controls if unconfirmed, and pill Play/Shuffle buttons.
 * Guarantees zero UI overlap.
 */
@Composable
private fun FolderHeaderBanner(
    folder: AddedFolder,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onKeepArtwork: () -> Unit,
    onTryAgainArtwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Album Art Cover
                FolderCoverImage(
                    artworkUri = folder.artworkUri,
                    folderName = folder.name,
                    size = 90.dp,
                    shapeRadius = 14.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${folder.songCount} songs in folder",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    if (folder.artworkUri != null && folder.isArtworkConfirmed) {
                        Text(
                            text = "✓ Album Cover Saved",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Keep or Try Again Banner if unconfirmed web art
            if (!folder.isArtworkConfirmed && folder.artworkUri != null) {
                ArtworkKeepOrTryAgainBanner(
                    candidateIndex = folder.candidateIndex,
                    onKeep = onKeepArtwork,
                    onTryAgain = onTryAgainArtwork
                )
            }

            // Pill-shaped Play All & Shuffle Buttons matching the folder sides
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Play All Button (Pill shape)
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play All",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Shuffle Button (Pill shape)
                Button(
                    onClick = onShuffle,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shuffle",
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated, non-overlapping banner shown when new album artwork is fetched from the internet.
 * Features:
 * - [✓ Keep] pill button: accepts artwork and makes the prompt go away.
 * - [↻ Try Again] pill button: fetches another candidate image from the web.
 */
@Composable
private fun ArtworkKeepOrTryAgainBanner(
    candidateIndex: Int,
    onKeep: () -> Unit,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141923))
            .border(1.dp, SecondaryCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Web Album Art Found",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = SecondaryCyan,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Preview #${candidateIndex + 1}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 11.sp
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Keep Button (Pill shaped, green accent)
            Button(
                onClick = onKeep,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B382B)),
                border = BorderStroke(1.dp, Color(0xFF34D399)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Keep",
                    color = Color(0xFF34D399),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Try Again Button (Pill shaped, cyan accent)
            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                border = BorderStroke(1.dp, SurfaceBorder),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = SecondaryCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Try Again",
                    color = SecondaryCyan,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}
