package com.nimmaguru.app.core.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme

/**
 * Guru avatar that shows a photo if available, otherwise renders
 * colored initials.
 *
 * Photo source supports three forms:
 *   - http(s)://... URL          → loaded via Coil AsyncImage
 *   - data:image/jpeg;base64,... → decoded inline via Bitmap.decodeByteArray
 *   - blank string               → falls back to colored initials
 *
 * R-COMP-04: Min touch target 48 dp, accessible contrast.
 */
@Composable
fun GuruAvatar(
    name: String,
    modifier: Modifier = Modifier,
    photoUrl: String = "",
    size: Dp = 56.dp,
    contentDescription: String? = null,
) {
    val initials = remember(name) { extractInitials(name) }
    val bgColor = remember(name) { generateColor(name) }

    when {
        photoUrl.startsWith("data:image", ignoreCase = true) -> {
            // Inline base64 — decode once and cache via remember.
            val bitmap = remember(photoUrl) { decodeDataUrlBitmap(photoUrl) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription ?: name,
                    modifier = modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                InitialsAvatar(initials, bgColor, size, modifier)
            }
        }
        photoUrl.isNotBlank() -> {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription ?: name,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        else -> InitialsAvatar(initials, bgColor, size, modifier)
    }
}

@Composable
private fun InitialsAvatar(
    initials: String,
    bgColor: Color,
    size: Dp,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun decodeDataUrlBitmap(dataUrl: String): Bitmap? {
    val commaIndex = dataUrl.indexOf(',')
    if (commaIndex < 0 || commaIndex == dataUrl.length - 1) return null
    val payload = dataUrl.substring(commaIndex + 1)
    return runCatching {
        val bytes = Base64.decode(payload, Base64.NO_WRAP or Base64.URL_SAFE)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

/**
 * Extract up to 2 initials from a name.
 * "Ramesh Kumar" → "RK"
 * "Lakshmi" → "L"
 */
private fun extractInitials(name: String): String {
    return name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
}

/**
 * Generate a warm, deterministic color from a name hash.
 * Ensures same name = same color across sessions.
 */
private fun generateColor(name: String): Color {
    val warmColors = listOf(
        Color(0xFFE65100), // Deep Orange (primary)
        Color(0xFFAD1457), // Pink
        Color(0xFF6A1B9A), // Purple
        Color(0xFF1565C0), // Blue
        Color(0xFF00838F), // Teal
        Color(0xFF2E7D32), // Green
        Color(0xFFF57F17), // Amber
        Color(0xFF4E342E), // Brown
        Color(0xFF37474F), // Blue Grey
        Color(0xFFC62828), // Red
    )
    val index = (name.hashCode().toLong() and 0xFFFFFFFFL).toInt() % warmColors.size
    return warmColors[kotlin.math.abs(index)]
}

@Preview(showBackground = true)
@Composable
private fun Preview_GuruAvatar_Initials() {
    NimmaGuruTheme {
        GuruAvatar(name = "Ramesh Kumar", size = 72.dp)
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_GuruAvatar_SingleName() {
    NimmaGuruTheme {
        GuruAvatar(name = "Lakshmi", size = 56.dp)
    }
}
