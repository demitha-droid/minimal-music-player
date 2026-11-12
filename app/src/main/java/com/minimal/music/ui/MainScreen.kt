package com.minimal.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.minimal.music.data.model.Song
import com.minimal.music.ui.components.MiniPlayerBar
import com.minimal.music.ui.navigation.MinimalBottomNavBar
import com.minimal.music.ui.navigation.Screen
import com.minimal.music.ui.screens.DiscoverScreen
import com.minimal.music.ui.screens.FoldersScreen
import com.minimal.music.ui.screens.NowPlayingSheet
import com.minimal.music.ui.screens.PlaylistDetailScreen
import com.minimal.music.ui.screens.PlaylistsScreen
import com.minimal.music.ui.screens.SongsScreen
import com.minimal.music.ui.theme.SurfaceElevated
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.theme.TextSecondary
import com.minimal.music.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(Screen.Discover.route) }
    var activePlaylistDetail by remember { mutableStateOf<Pair<Long, String>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val mostPopular by viewModel.mostPopular.collectAsState()

    var showNowPlayingSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    // Intercept back gesture: first dismiss playlist detail overlay, then return to Discover tab
    BackHandler(enabled = activePlaylistDetail != null) {
        activePlaylistDetail = null
    }
    BackHandler(enabled = activePlaylistDetail == null && currentTab != Screen.Discover.route) {
        currentTab = Screen.Discover.route
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                MiniPlayerSection(
                    viewModel = viewModel,
                    onExpand = { showNowPlayingSheet = true }
                )

                MinimalBottomNavBar(
                    currentRoute = if (activePlaylistDetail != null) Screen.Playlists.route else currentTab,
                    onNavigate = { screen ->
                        activePlaylistDetail = null
                        currentTab = screen.route
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Discover Tab
            PersistentTabContainer(
                isActive = currentTab == Screen.Discover.route && activePlaylistDetail == null
            ) {
                DiscoverScreen(
                    viewModel = viewModel,
                    onSongSelectedForPlaylist = { selectedSongForPlaylist = it }
                )
            }

            // 2. Songs Tab
            PersistentTabContainer(
                isActive = currentTab == Screen.Songs.route && activePlaylistDetail == null
            ) {
                SongsScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onSongSelectedForPlaylist = { selectedSongForPlaylist = it },
                    isActive = currentTab == Screen.Songs.route && activePlaylistDetail == null
                )
            }

            // 3. Playlists Tab
            PersistentTabContainer(
                isActive = currentTab == Screen.Playlists.route && activePlaylistDetail == null
            ) {
                PlaylistsScreen(
                    viewModel = viewModel,
                    onOpenSmartPlaylist = { smartName ->
                        activePlaylistDetail = Pair(-1L, smartName)
                    },
                    onOpenPlaylist = { playlist ->
                        activePlaylistDetail = Pair(playlist.id, playlist.name)
                    }
                )
            }

            // 4. Explorer (Folders) Tab
            PersistentTabContainer(
                isActive = currentTab == Screen.Folders.route && activePlaylistDetail == null
            ) {
                FoldersScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onSongSelectedForPlaylist = { selectedSongForPlaylist = it },
                    isActive = currentTab == Screen.Folders.route && activePlaylistDetail == null
                )
            }

            // 5. Playlist Detail Overlay (Zero-delay, smooth fade/slide)
            val playlistDetail = activePlaylistDetail
            AnimatedVisibility(
                visible = playlistDetail != null,
                enter = fadeIn(animationSpec = tween(150)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(150)
                ),
                exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(
                    targetOffsetY = { 40 },
                    animationSpec = tween(150)
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
            ) {
                if (playlistDetail != null) {
                    val (playlistId, playlistName) = playlistDetail
                    val songs: List<Song> = when {
                        playlistId == -1L && playlistName == "Favorites" -> favorites
                        playlistId == -1L && playlistName == "Most Played" -> mostPlayed
                        playlistId == -1L && playlistName == "Most Popular" -> mostPopular
                        else -> {
                            val playlistSongs by viewModel.getSongsForPlaylist(playlistId).collectAsState(initial = emptyList())
                            playlistSongs
                        }
                    }

                    PlaylistDetailScreen(
                        title = playlistName,
                        playlistId = if (playlistId == -1L) null else playlistId,
                        songs = songs,
                        viewModel = viewModel,
                        onBack = { activePlaylistDetail = null }
                    )
                }
            }
        }
    }

    // Now Playing Modal Bottom Sheet
    if (showNowPlayingSheet) {
        NowPlayingSheetWrapper(
            viewModel = viewModel,
            onDismiss = { showNowPlayingSheet = false }
        )
    }

    // Add to Playlist Selection Sheet
    selectedSongForPlaylist?.let { song ->
        ModalBottomSheet(
            onDismissRequest = { selectedSongForPlaylist = null },
            containerColor = SurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                )

                if (playlists.isEmpty()) {
                    Text(
                        text = "No custom playlists yet. Create one in the Playlists tab.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, song.id)
                                        val songTitle = song.title
                                        val plName = playlist.name
                                        selectedSongForPlaylist = null
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Added '$songTitle' to $plName")
                                        }
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Persistent tab wrapper that keeps the Composable mounted in the composition tree
 * with GPU-accelerated opacity + translation transitions.
 * Eliminates DOM-like cold tree destruction/rebuilding and preserves scroll positions.
 */
@Composable
private fun PersistentTabContainer(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "tabAlpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (isActive) 0f else 20f,
        animationSpec = tween(durationMillis = 150),
        label = "tabTranslationY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translationY
            }
            .zIndex(if (isActive) 1f else 0f)
            .then(
                if (!isActive) {
                    Modifier.pointerInput(isActive) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * MiniPlayerSection isolates the mini-player recompositions.
 * By deriving currentSong and isPlaying via distinctUntilChanged(),
 * the 100ms audio progress ticks never recompose MiniPlayerSection or MiniPlayerBar.
 * Progress updates are read during the draw phase inside LinearProgressIndicator.
 */
@Composable
private fun MiniPlayerSection(
    viewModel: MusicViewModel,
    onExpand: () -> Unit
) {
    val currentSong by remember(viewModel) {
        viewModel.playbackState.map { it.currentSong }.distinctUntilChanged()
    }.collectAsState(initial = viewModel.playbackState.value.currentSong)

    val isPlaying by remember(viewModel) {
        viewModel.playbackState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = viewModel.playbackState.value.isPlaying)

    val isMiniPlayerDismissed by viewModel.isMiniPlayerDismissed.collectAsState()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsState()

    val song = currentSong
    if (song != null && !isMiniPlayerDismissed) {
        MiniPlayerBar(
            currentSong = song,
            isPlaying = isPlaying,
            progressProvider = { viewModel.playbackState.value.progress },
            onPlayPauseClick = { viewModel.togglePlayPause() },
            onNextClick = { viewModel.skipNext() },
            onPreviousClick = { viewModel.skipPrevious(forcePrevious = true) },
            onClick = onExpand,
            onDismiss = { viewModel.dismissMiniPlayer() },
            sleepTimerRemainingMs = sleepTimerRemainingMs
        )
    }
}

@Composable
private fun NowPlayingSheetWrapper(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()
    if (playbackState.currentSong != null) {
        NowPlayingSheet(
            playbackState = playbackState,
            viewModel = viewModel,
            onDismiss = onDismiss
        )
    }
}
