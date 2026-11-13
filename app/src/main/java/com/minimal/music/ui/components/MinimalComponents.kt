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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
    shapeRadius: Dp = 12.dp,
    artworkUri: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val reqPx = size?.let { (it.value * 2.5f).toInt().coerceIn(90, 480) } ?: 180

    // Initialize directly from in-memory cache to eliminate 1-frame placeholder flash during scrolling
    val initialCached = remember(artworkUri, reqPx) {
        artworkUri?.let { ImageCacheManager.get(it, reqPx)?.asImageBitmap() }
    }
    var bitmap by remember(artworkUri, reqPx) {
        mutableStateOf(initialCached)
    }

    LaunchedEffect(artworkUri, reqPx) {
        if (!artworkUri.isNullOrBlank()) {
            val cached = ImageCacheManager.get(artworkUri, reqPx)
            if (cached != null) {
                bitmap = cached.asImageBitmap()
            } else {
                withContext(Dispatchers.IO) {
                    val bmp = ImageCacheManager.decodeSampledBitmap(context, artworkUri, reqWidth = reqPx, reqHeight = reqPx)
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
    val currentBitmap = bitmap

    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
            contentDescription = "Cover for $title",
            modifier = baseModifier
                .clip(RoundedCornerShape(shapeRadius))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(shapeRadius)),
            contentScale = contentScale
        )
    } else {
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
}

/**
 * High-performance LRU memory cache for downsampled cover art.
 * Eliminates repeated disk reads and prevents Garbage Collector pauses.
 */
object ImageCacheManager {
    private val cacheSize: Int = try {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).coerceIn(1024L * 8, 1024L * 256).toInt()
        (maxMemoryKb / 8).coerceIn(1024 * 4, 1024 * 32)
    } catch (_: Throwable) {
        1024 * 16
    }

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    private fun tier(reqWidth: Int): String = when {
        reqWidth <= 140 -> "sm"
        reqWidth <= 280 -> "md"
        else -> "lg"
    }

    fun get(uriString: String, reqWidth: Int = 360): Bitmap? = try {
        val t = tier(reqWidth)
        val exact = lruCache.get("${uriString}_$t")
        if (exact != null && !exact.isRecycled) {
            exact
        } else {
            val lg = lruCache.get("${uriString}_lg")
            if (lg != null && !lg.isRecycled) {
                lg
            } else {
                val md = lruCache.get("${uriString}_md")
                if (md != null && !md.isRecycled) md else null
            }
        }
    } catch (_: Throwable) {
        null
    }

    fun put(uriString: String, reqWidth: Int, bitmap: Bitmap) {
        try {
            val t = tier(reqWidth)
            lruCache.put("${uriString}_$t", bitmap)
        } catch (_: Throwable) {}
    }

    fun decodeSampledBitmap(context: Context, uriString: String, reqWidth: Int = 360, reqHeight: Int = 360): Bitmap? {
        val cached = get(uriString, reqWidth)
        if (cached != null) return cached

        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            val isContentOrFile = uriString.startsWith("content://") || uriString.startsWith("file://")
            val cleanPath = if (uriString.startsWith("file://")) uriString.removePrefix("file://") else uriString

            if (isContentOrFile) {
                val parsedUri = Uri.parse(uriString)
                val stream = context.contentResolver.openInputStream(parsedUri)
                if (stream != null) {
                    stream.use { BitmapFactory.decodeStream(it, null, options) }
                } else {
                    BitmapFactory.decodeFile(cleanPath, options)
                }
            } else {
                BitmapFactory.decodeFile(cleanPath, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            val bitmap = if (isContentOrFile) {
                val parsedUri = Uri.parse(uriString)
                val stream = context.contentResolver.openInputStream(parsedUri)
                if (stream != null) {
                    stream.use { BitmapFactory.decodeStream(it, null, options) }
                } else {
                    BitmapFactory.decodeFile(cleanPath, options)
                }
            } else {
                BitmapFactory.decodeFile(cleanPath, options)
            }

            if (bitmap != null) {
                put(uriString, reqWidth, bitmap)
            }
            return bitmap
        } catch (_: Throwable) {
            return null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
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
    val reqPx = size?.let { (it.value * 2.5f).toInt().coerceIn(90, 480) } ?: 180

    val initialCached = remember(artworkUri, reqPx) {
        artworkUri?.let { ImageCacheManager.get(it, reqPx)?.asImageBitmap() }
    }
    var bitmap by remember(artworkUri, reqPx) {
        mutableStateOf(initialCached)
    }

    LaunchedEffect(artworkUri, reqPx) {
        if (!artworkUri.isNullOrBlank()) {
            val cached = ImageCacheManager.get(artworkUri, reqPx)
            if (cached != null) {
                bitmap = cached.asImageBitmap()
            } else {
                withContext(Dispatchers.IO) {
                    val bmp = ImageCacheManager.decodeSampledBitmap(context, artworkUri, reqWidth = reqPx, reqHeight = reqPx)
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
    val currentBitmap = bitmap

    if (currentBitmap != null) {
        Image(
            bitmap = currentBitmap,
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
    val s1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s1"
    )
    val s2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s2"
    )
    val s3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "s3"
    )

    Canvas(
        modifier = modifier
            .width(16.dp)
            .height(18.dp)
    ) {
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        val maxHeight = size.height

        val heights = listOf(maxHeight * s1, maxHeight * s2, maxHeight * s3)
        for (i in 0..2) {
            val barHeight = heights[i].coerceIn(barWidth, maxHeight)
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
            modifier = Modifier.size(36.dp)
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
            ThemeMode.entries.forEach { mode ->
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

