package com.minimal.music.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.music.data.model.AddedFolder
import com.minimal.music.data.model.Song
import com.minimal.music.ui.components.FolderCoverImage
import com.minimal.music.ui.components.MinimalSearchBar
import com.minimal.music.ui.components.SongListItem
import com.minimal.music.ui.components.ThemeSwitcherButton
import com.minimal.music.ui.theme.BgDark
import com.minimal.music.ui.theme.PrimaryViolet
import com.minimal.music.ui.theme.SecondaryCyan
import com.minimal.music.ui.theme.SurfaceBorder
import com.minimal.music.ui.theme.SurfaceDark
import com.minimal.music.ui.theme.SurfaceElevated
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.theme.TextSecondary
import com.minimal.music.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val currentSongId by remember(viewModel) {
        viewModel.playbackState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsState(initial = null)
    val isPlaying by remember(viewModel) {
        viewModel.playbackState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val themeMode by viewModel.themeMode.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Handle system back gesture when inside a folder
    BackHandler(enabled = openedFolder != null) {
        viewModel.closeFolder()
    }

    // SAF folder picker directly from the Songs tab
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
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
            // ROOT VIEW OF SONGS TAB: 2-COLUMN GALLERY GRID (STRICTLY FOLDERS ONLY)
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
                        text = "${addedFolders.size} music folders • 2 per row gallery",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Theme Switcher Button
                    ThemeSwitcherButton(
                        currentTheme = themeMode,
                        onThemeSelect = { viewModel.setThemeMode(it) }
                    )

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
                            tint = TextSecondary,
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

            val filteredFolders = remember(addedFolders, searchQuery) {
                if (searchQuery.isBlank()) {
                    addedFolders
                } else {
                    addedFolders.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
                }
            }

            if (addedFolders.isEmpty()) {
                // Empty state: No folders added yet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp),
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
                                .background(SurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = TextSecondary,
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
                            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = BgDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Folder from Device",
                                color = BgDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (filteredFolders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No folders match '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
            } else {
                // 2 FOLDERS A ROW GALLERY GRID (REDUCED FOLDER SIZE)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredFolders,
                        key = { it.id }
                    ) { folder ->
                        FolderGalleryCard(
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
            val folderSongs = remember(allSongs, currentFolder) {
                val targetPath = currentFolder.path.trimEnd('/', '\\')
                allSongs.filter { song ->
                    val songPath = song.folderPath?.trimEnd('/', '\\')
                    val pathMatch = songPath != null && (
                        songPath.equals(targetPath, ignoreCase = true) ||
                        songPath.startsWith("$targetPath/", ignoreCase = true) ||
                        songPath.startsWith("$targetPath\\", ignoreCase = true)
                    )

                    val nameMatch = !song.folderName.isNullOrBlank() &&
                        song.folderName.equals(currentFolder.name, ignoreCase = true) &&
                        (songPath == null || songPath.contains(currentFolder.name, ignoreCase = true))

                    pathMatch || nameMatch
                }.distinctBy {
                    val normTitle = it.title.trim().lowercase()
                    val normArtist = it.artist.trim().lowercase()
                    val durBucket = it.durationMs / 2000L
                    "${normTitle}::${normArtist}::${durBucket}"
                }
            }

            val displayFolderSongs = remember(folderSongs, searchQuery) {
                if (searchQuery.isBlank()) {
                    folderSongs
                } else {
                    val query = searchQuery.trim()
                    folderSongs.filter {
                        it.title.contains(query, ignoreCase = true) ||
                            it.artist.contains(query, ignoreCase = true)
                    }
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
                            tint = TextSecondary,
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
                songCount = folderSongs.size,
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
                        .padding(bottom = 20.dp),
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
                    contentPadding = PaddingValues(top = 2.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = displayFolderSongs,
                        key = { it.id }
                    ) { song ->
                        SongListItem(
                            song = song,
                            isPlaying = isPlaying,
                            isCurrentSong = currentSongId == song.id,
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
 * Compact Folder Gallery Card for 2-column Grid View.
 * Displays:
 * - Square album cover art filling the card width
 * - Keep / Try Again controls if unconfirmed
 * - Folder name & track count
 * - Pill-shaped Play and Shuffle buttons matching the folder card sides
 * Strictly ZERO element overlap.
 */
@Composable
private fun FolderGalleryCard(
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
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Square Cover Art filling width
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                FolderCoverImage(
                    artworkUri = folder.artworkUri,
                    folderName = folder.name,
                    shapeRadius = 10.dp,
                    modifier = Modifier.fillMaxSize()
                )

                // 3-dots Menu floating top right with backdrop
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Folder options",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
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

            // Keep or Try Again buttons if unconfirmed
            if (!folder.isArtworkConfirmed && folder.artworkUri != null) {
                CompactArtworkActionRow(
                    onKeep = onKeepArtwork,
                    onTryAgain = onTryAgainArtwork
                )
            }

            // Folder Title & Track Count
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${folder.songCount} tracks",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                )
            }

            // PILL-SHAPED PLAY & SHUFFLE BUTTONS MATCHING THE FOLDER SIDE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pill Play Button
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = BgDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Play",
                        color = BgDark,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Pill Shuffle Button
                Button(
                    onClick = onShuffle,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Mix",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Compact Keep & Try Again pill button bar for 2-column gallery cards.
 */
@Composable
private fun CompactArtworkActionRow(
    onKeep: () -> Unit,
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Keep Button (Flat monochrome)
        Button(
            onClick = onKeep,
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = BgDark,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Keep",
                color = BgDark,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }

        // Try Button (Flat monochrome outline)
        Button(
            onClick = onTryAgain,
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, SurfaceBorder),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Try",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            )
        }
    }
}

/**
 * Top Header Banner inside an Opened Folder.
 * Guarantees zero UI overlap.
 */
@Composable
private fun FolderHeaderBanner(
    folder: AddedFolder,
    songCount: Int,
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
                        text = "$songCount songs in folder",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )

                    if (folder.artworkUri != null && folder.isArtworkConfirmed) {
                        Text(
                            text = "✓ Album Cover Saved",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Keep or Try Again Banner if unconfirmed web art
            if (!folder.isArtworkConfirmed && folder.artworkUri != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onKeepArtwork,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = BgDark, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Keep Cover", color = BgDark, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Button(
                        onClick = onTryAgainArtwork,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Try Another", color = TextSecondary, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
                    }
                }
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
                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = BgDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play All",
                        color = BgDark,
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
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shuffle",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}
