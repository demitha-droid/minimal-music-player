package com.minimal.music.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minimal.music.data.model.Playlist
import com.minimal.music.data.model.Song
import com.minimal.music.ui.components.MiniPlayerBar
import com.minimal.music.ui.navigation.MinimalBottomNavBar
import com.minimal.music.ui.navigation.Screen
import com.minimal.music.ui.screens.DiscoverScreen
import com.minimal.music.ui.screens.NowPlayingSheet
import com.minimal.music.ui.screens.PlaylistDetailScreen
import com.minimal.music.ui.screens.PlaylistsScreen
import com.minimal.music.ui.screens.SongsScreen
import com.minimal.music.ui.theme.PrimaryViolet
import com.minimal.music.ui.theme.SurfaceDark
import com.minimal.music.ui.theme.SurfaceElevated
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val playbackState by viewModel.playbackState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val mostPopular by viewModel.mostPopular.collectAsState()

    var showNowPlayingSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                if (playbackState.currentSong != null) {
                    MiniPlayerBar(
                        playbackState = playbackState,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.skipNext() },
                        onClick = { showNowPlayingSheet = true }
                    )
                }

                MinimalBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Discover.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Discover.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Discover.route) {
                DiscoverScreen(
                    viewModel = viewModel,
                    onSongSelectedForPlaylist = { selectedSongForPlaylist = it }
                )
            }

            composable(Screen.Songs.route) {
                SongsScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onSongSelectedForPlaylist = { selectedSongForPlaylist = it }
                )
            }

            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    viewModel = viewModel,
                    onOpenSmartPlaylist = { smartName ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(-1L, smartName))
                    },
                    onOpenPlaylist = { playlist ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id, playlist.name))
                    }
                )
            }

            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.LongType },
                    navArgument("playlistName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: -1L
                val rawName = backStackEntry.arguments?.getString("playlistName") ?: "Playlist"
                val playlistName = rawName.replace("_", "/")

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
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    // Now Playing Modal Bottom Sheet
    if (showNowPlayingSheet && playbackState.currentSong != null) {
        NowPlayingSheet(
            playbackState = playbackState,
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
                                    tint = PrimaryViolet,
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
