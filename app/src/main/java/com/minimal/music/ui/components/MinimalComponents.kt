package com.minimal.music.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimal.music.ui.theme.EqualizerGreen
import com.minimal.music.ui.theme.PrimaryViolet
import com.minimal.music.ui.theme.SurfaceBorder
import com.minimal.music.ui.theme.SurfaceElevated
import com.minimal.music.ui.theme.TextMuted
import com.minimal.music.ui.theme.TextPrimary
import com.minimal.music.ui.theme.TextSecondary
import com.minimal.music.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MinimalArtwork(
    title: String,
    modifier: Modifier = Modifier,
    size: Dp? = 48.dp,
    shapeRadius: Dp = 12.dp
) {
    // Generate deterministic pleasing colors from title hash
    val charCode = title.fold(0) { acc, c -> acc + c.code }
    val hue1 = (charCode % 360).toFloat()
    val hue2 = ((charCode + 60) % 360).toFloat()

    val color1 = Color.hsl(hue1, 0.45f, 0.22f)
    val color2 = Color.hsl(hue2, 0.65f, 0.12f)

    val baseModifier = if (size != null) modifier.size(size) else modifier

    Box(
        modifier = baseModifier
            .clip(RoundedCornerShape(shapeRadius))
            .background(Brush.linearGradient(listOf(color1, color2)))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(shapeRadius)),
        contentAlignment = Alignment.Center
    ) {
        val initials = title.take(2).uppercase()
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = if (size != null) (size.value * 0.32f).sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary.copy(alpha = 0.85f)
            )
        )
    }
}

@Composable
fun FolderCoverImage(
    artworkUri: String?,
    folderName: String,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shapeRadius: Dp = 12.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var bitmap by remember(artworkUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(artworkUri) {
        if (!artworkUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(artworkUri)
                    if (file.exists() && file.length() > 0) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) {
                            bitmap = bmp.asImageBitmap()
                        }
                    } else if (artworkUri.startsWith("content://")) {
                        context.contentResolver.openInputStream(Uri.parse(artworkUri))?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                bitmap = bmp.asImageBitmap()
                            }
                        }
                    }
                } catch (_: Exception) {
                    bitmap = null
                }
            }
        } else {
            bitmap = null
        }
    }

    val baseModifier = if (size != null) modifier.size(size) else modifier

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = "Cover for $folderName",
            modifier = baseModifier
                .clip(RoundedCornerShape(shapeRadius))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(shapeRadius)),
            contentScale = contentScale
        )
    } else {
        MinimalArtwork(
            title = folderName,
            size = size,
            shapeRadius = shapeRadius,
            modifier = modifier
        )
    }
}

@Composable
fun EqualizerIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier
            .height(20.dp)
            .width(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h1.dp)
                .clip(CircleShape)
                .background(EqualizerGreen)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h2.dp)
                .clip(CircleShape)
                .background(EqualizerGreen)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(h3.dp)
                .clip(CircleShape)
                .background(EqualizerGreen)
        )
    }
}

@Composable
fun MinimalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search folders or songs..."
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(23.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(PrimaryViolet),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Rounded.Clear,
                    contentDescription = "Clear search",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }
    }
}

@Composable
fun ThemeSwitcherButton(
    currentTheme: ThemeMode,
    onThemeSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val icon = when (currentTheme) {
        ThemeMode.DARK -> Icons.Rounded.DarkMode
        ThemeMode.AMOLED -> Icons.Rounded.Contrast
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceElevated)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Theme: ${currentTheme.title}",
                tint = PrimaryViolet,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceElevated)
        ) {
            ThemeMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val modeIcon = when (mode) {
                                ThemeMode.DARK -> "🌙"
                                ThemeMode.AMOLED -> "⬛"
                                ThemeMode.LIGHT -> "☀️"
                            }
                            Text(text = modeIcon)
                            Text(
                                text = mode.title,
                                fontWeight = if (mode == currentTheme) FontWeight.Bold else FontWeight.Normal,
                                color = if (mode == currentTheme) PrimaryViolet else TextPrimary
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onThemeSelect(mode)
                    }
                )
            }
        }
    }
}

