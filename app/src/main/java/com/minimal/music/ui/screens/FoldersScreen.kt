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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.minimal.music.data.model.Song
import com.minimal.music.data.source.FileExplorerItem
import com.minimal.music.ui.components.EqualizerIndicator
import com.minimal.music.ui.components.MinimalArtwork
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
import java.io.File

@Composable
fun FoldersScreen(
    viewModel: MusicViewModel,
    snackbarHostState: SnackbarHostState,
    onSongSelectedForPlaylist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDir by viewModel.currentDirectory.collectAsState()
    val directoryItems by viewModel.directoryItems.collectAsState()
    val isLoading by viewModel.isLoadingDirectory.collectAsState()
    val currentSongUri by remember(viewModel) {
        viewModel.playbackState.map { it.currentSong?.mediaUri }.distinctUntilChanged()
    }.collectAsState(initial = null)
    val isPlaying by remember(viewModel) {
        viewModel.playbackState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = false)
    val commonRoots = viewModel.commonRoots
    val scope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    // SAF Directory picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}
            viewModel.importFromTreeUri(uri) { count ->
                scope.launch {
                    snackbarHostState.showSnackbar("Imported $count tracks from folder")
                }
            }
        }
    }

    // SAF Files picker
    val filesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFilesFromUris(uris) { count ->
                scope.launch {
                    snackbarHostState.showSnackbar("Imported $count audio files")
                }
            }
        }
    }

    val audioItemsInCurrentDir = directoryItems.filterIsInstance<FileExplorerItem.Audio>()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Title and SAF Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Explorer",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Browse & add local folders or files",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Pick Folder Button
                IconButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = "Pick folder from storage",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Pick Audio Files Button
                IconButton(
                    onClick = { filesPickerLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.NoteAdd,
                        contentDescription = "Pick audio files",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Root Shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonRoots.forEach { root ->
                val isSelected = currentDir?.absolutePath == root.absolutePath
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) TextPrimary else SurfaceElevated)
                        .clickable { viewModel.navigateTo(root) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = root.name.ifEmpty { "Storage" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isSelected) BgDark else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Breadcrumb Bar
        currentDir?.let { dir ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateUp() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Up one directory",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = dir.name.ifEmpty { dir.path },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (audioItemsInCurrentDir.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.addFolderToLibrary(dir) { count ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Added $count songs from '${dir.name}' to library")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Add All (${audioItemsInCurrentDir.size})",
                            color = BgDark,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Directory Content List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryViolet)
            }
        } else if (directoryItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Folder is empty or has no audio files",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                    Text(
                        text = "Use the buttons at top to pick folders or files from anywhere",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted.copy(alpha = 0.7f))
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = directoryItems,
                    key = { item ->
                        when (item) {
                            is FileExplorerItem.Directory -> "dir_${item.path}"
                            is FileExplorerItem.Audio -> "audio_${item.path}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is FileExplorerItem.Directory -> {
                            DirectoryRow(
                                item = item,
                                onClick = { viewModel.navigateTo(item.file) },
                                onAddFolder = {
                                    viewModel.addFolderToLibrary(item.file) { count ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Added $count songs from '${item.name}'")
                                        }
                                    }
                                }
                            )
                        }
                        is FileExplorerItem.Audio -> {
                            val isCurrentSong = currentSongUri == item.song.mediaUri
                            AudioFileRow(
                                item = item,
                                isPlaying = isPlaying,
                                isCurrentSong = isCurrentSong,
                                onPlay = {
                                    val contextSongs = audioItemsInCurrentDir.map { it.song }
                                    viewModel.playSong(item.song, contextSongs)
                                },
                                onAddToLibrary = {
                                    viewModel.addSingleSongToLibrary(item.song) { success ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Added '${item.song.title}' to library")
                                        }
                                    }
                                },
                                onAddToPlaylist = {
                                    onSongSelectedForPlaylist(item.song)
                                },
                                onFetchArtwork = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Downloading YouTube picture for ${item.song.title}...")
                                    }
                                    viewModel.downloadThumbnailForSong(item.song) { success ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (success) "Downloaded YouTube picture for ${item.song.title}"
                                                else "Could not find YouTube picture for ${item.song.title}"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryRow(
    item: FileExplorerItem.Directory,
    onClick: () -> Unit,
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val details = if (item.audioCount > 0) {
                "${item.itemCount} items • ${item.audioCount} audio tracks"
            } else {
                "${item.itemCount} items"
            }

            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted
                )
            )
        }

        if (item.audioCount > 0) {
            IconButton(
                onClick = onAddFolder,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add folder to library",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioFileRow(
    item: FileExplorerItem.Audio,
    isPlaying: Boolean,
    isCurrentSong: Boolean,
    onPlay: () -> Unit,
    onAddToLibrary: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    onFetchArtwork: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPlay() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrentSong && isPlaying) {
                EqualizerIndicator()
            } else {
                MinimalArtwork(
                    title = item.song.title,
                    size = 42.dp,
                    shapeRadius = 10.dp,
                    artworkUri = item.song.artworkUri
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.song.artist} • ${item.sizeFormatted}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onPlay,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play track",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (onFetchArtwork != null) {
                    DropdownMenuItem(
                        text = { Text("Get YouTube Picture") },
                        onClick = {
                            showMenu = false
                            onFetchArtwork()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Add to Library") },
                    onClick = {
                        showMenu = false
                        onAddToLibrary()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = {
                        showMenu = false
                        onAddToPlaylist()
                    }
                )
            }
        }
    }
}
