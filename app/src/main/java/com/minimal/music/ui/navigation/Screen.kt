package com.minimal.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Discover : Screen("discover", "Discover", Icons.Rounded.Explore)
    object Songs : Screen("songs", "Songs", Icons.Rounded.MusicNote)
    object Folders : Screen("folders", "Explorer", Icons.Rounded.Folder)
    object Playlists : Screen("playlists", "Playlists", Icons.Rounded.LibraryMusic)

    object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}", "Playlist") {
        fun createRoute(playlistId: Long, playlistName: String) =
            "playlist_detail/$playlistId/${playlistName.replace("/", "_")}"
    }

    companion object {
        val bottomNavItems = listOf(Discover, Songs, Playlists, Folders)
    }
}
