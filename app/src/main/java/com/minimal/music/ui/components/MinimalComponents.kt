package com.minimal.music.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val baseModifier = if (size != null) modifier.size(size) else modifier

    Box(
        modifier = baseModifier
            .clip(RoundedCornerShape(shapeRadius))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(shapeRadius)),
        contentAlignment = Alignment.Center
    ) {
        val initials = title.take(2).uppercase()
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = if (size != null) (size.value * 0.32f).sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        )
    }
}

/**
 * High-performance LRU memory cache for downsampled cover art.
 * Eliminates repeated disk reads and prevents Garbage Collector pauses.
 */
object ImageCacheManager {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceIn(1024 * 8, 1024 * 32)

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun get(key: String): Bitmap? = lruCache.get(key)
    fun put(key: String, bitmap: Bitmap) {
        lruCache.put(key, bitmap)
    }

    fun decodeSampledBitmap(context: Context, uriString: String, reqWidth: Int = 360, reqHeight: Int = 360): Bitmap? {
        val cached = get(uriString)
        if (cached != null) return cached

        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            if (uriString.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                BitmapFactory.decodeFile(uriString, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val bitmap = if (uriString.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            } else {
                BitmapFactory.decodeFile(uriString, options)
            }

            if (bitmap != null) {
                put(uriString, bitmap)
            }
            return bitmap
        } catch (_: Throwable) {
            return null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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
    var bitmap by remember(artworkUri) {
        mutableStateOf(artworkUri?.let { ImageCacheManager.get(it)?.asImageBitmap() })
    }

    LaunchedEffect(artworkUri) {
        if (!artworkUri.isNullOrBlank()) {
            if (bitmap == null) {
                withContext(Dispatchers.IO) {
                    val bmp = ImageCacheManager.decodeSampledBitmap(context, artworkUri)
                    if (bmp != null) {
                        bitmap = bmp.asImageBitmap()
                    }
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
    val barColor = EqualizerGreen
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

    Canvas(
        modifier = modifier
            .width(16.dp)
            .height(20.dp)
    ) {
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val maxHeight = this.size.height

        val heights = listOf(h1.dp.toPx(), h2.dp.toPx(), h3.dp.toPx())
        for (i in 0..2) {
            val barHeight = heights[i].coerceIn(2f, maxHeight)
            val left = i * (barWidth + spacing)
            val top = maxHeight - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
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
                    cursorBrush = SolidColor(TextPrimary),
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
                tint = TextSecondary,
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

