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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.music.data.model.Playlist
import com.minimal.music.ui.theme.HeartActive
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
fun PlaylistsScreen(
    viewModel: MusicViewModel,
    onOpenSmartPlaylist: (name: String) -> Unit,
    onOpenPlaylist: (playlist: Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val mostPopular by viewModel.mostPopular.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create", color = PrimaryViolet)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceElevated
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Smart mixes & your collections",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }

                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryViolet)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Create Playlist",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Smart Playlists Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Smart Mixes",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
                )

                SmartPlaylistTile(
                    title = "Favorites",
                    subtitle = "${favorites.size} songs loved by you",
                    icon = Icons.Filled.Favorite,
                    iconTint = HeartActive,
                    onClick = { onOpenSmartPlaylist("Favorites") }
                )

                SmartPlaylistTile(
                    title = "Most Played",
                    subtitle = "${mostPlayed.size} tracks on repeat",
                    icon = Icons.Rounded.LocalFireDepartment,
                    iconTint = Color(0xFFFF9100),
                    onClick = { onOpenSmartPlaylist("Most Played") }
                )

                SmartPlaylistTile(
                    title = "Most Popular Hits",
                    subtitle = "${mostPopular.size} trending top charts",
                    icon = Icons.Rounded.Star,
                    iconTint = SecondaryCyan,
                    onClick = { onOpenSmartPlaylist("Most Popular") }
                )
            }
        }

        // Custom Playlists Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "My Playlists (${playlists.size})",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextSecondary)
                )

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tap + above to create your first playlist",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        playlists.forEach { playlist ->
                            CustomPlaylistItem(
                                playlist = playlist,
                                onClick = { onOpenPlaylist(playlist) },
                                onDelete = { viewModel.deletePlaylist(playlist.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
            )
        }
    }
}

@Composable
private fun CustomPlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

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
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.QueueMusic,
                contentDescription = null,
                tint = PrimaryViolet,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songCount} tracks",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
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
                DropdownMenuItem(
                    text = { Text("Delete playlist", color = Color(0xFFFF5252)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
