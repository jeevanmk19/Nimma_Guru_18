package com.nimmaguru.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Nimma Guru Theme — M3 Warm Theme with Dynamic Color fallback.
 *
 * Handles P-NICHE-06: Dynamic colors only on Android 12+ (API 31),
 * falling back to our warm custom palette on older devices.
 *
 * R-COMP-06: All theming goes through MaterialTheme.
 */

private val NimmaGuruLightColorScheme = lightColorScheme(
    primary = NimmaGuruLightPrimary,
    onPrimary = NimmaGuruLightOnPrimary,
    primaryContainer = NimmaGuruLightPrimaryContainer,
    onPrimaryContainer = NimmaGuruLightOnPrimaryContainer,
    secondary = NimmaGuruLightSecondary,
    onSecondary = NimmaGuruLightOnSecondary,
    secondaryContainer = NimmaGuruLightSecondaryContainer,
    onSecondaryContainer = NimmaGuruLightOnSecondaryContainer,
    tertiary = NimmaGuruLightTertiary,
    onTertiary = NimmaGuruLightOnTertiary,
    tertiaryContainer = NimmaGuruLightTertiaryContainer,
    onTertiaryContainer = NimmaGuruLightOnTertiaryContainer,
    error = NimmaGuruLightError,
    onError = NimmaGuruLightOnError,
    errorContainer = NimmaGuruLightErrorContainer,
    onErrorContainer = NimmaGuruLightOnErrorContainer,
    background = NimmaGuruLightBackground,
    onBackground = NimmaGuruLightOnBackground,
    surface = NimmaGuruLightSurface,
    onSurface = NimmaGuruLightOnSurface,
    surfaceVariant = NimmaGuruLightSurfaceVariant,
    onSurfaceVariant = NimmaGuruLightOnSurfaceVariant,
    outline = NimmaGuruLightOutline,
    outlineVariant = NimmaGuruLightOutlineVariant,
    inverseSurface = NimmaGuruLightInverseSurface,
    inverseOnSurface = NimmaGuruLightInverseOnSurface,
    inversePrimary = NimmaGuruLightInversePrimary,
    surfaceTint = NimmaGuruLightSurfaceTint,
)

private val NimmaGuruDarkColorScheme = darkColorScheme(
    primary = NimmaGuruDarkPrimary,
    onPrimary = NimmaGuruDarkOnPrimary,
    primaryContainer = NimmaGuruDarkPrimaryContainer,
    onPrimaryContainer = NimmaGuruDarkOnPrimaryContainer,
    secondary = NimmaGuruDarkSecondary,
    onSecondary = NimmaGuruDarkOnSecondary,
    secondaryContainer = NimmaGuruDarkSecondaryContainer,
    onSecondaryContainer = NimmaGuruDarkOnSecondaryContainer,
    tertiary = NimmaGuruDarkTertiary,
    onTertiary = NimmaGuruDarkOnTertiary,
    tertiaryContainer = NimmaGuruDarkTertiaryContainer,
    onTertiaryContainer = NimmaGuruDarkOnTertiaryContainer,
    error = NimmaGuruDarkError,
    onError = NimmaGuruDarkOnError,
    errorContainer = NimmaGuruDarkErrorContainer,
    onErrorContainer = NimmaGuruDarkOnErrorContainer,
    background = NimmaGuruDarkBackground,
    onBackground = NimmaGuruDarkOnBackground,
    surface = NimmaGuruDarkSurface,
    onSurface = NimmaGuruDarkOnSurface,
    surfaceVariant = NimmaGuruDarkSurfaceVariant,
    onSurfaceVariant = NimmaGuruDarkOnSurfaceVariant,
    outline = NimmaGuruDarkOutline,
    outlineVariant = NimmaGuruDarkOutlineVariant,
    inverseSurface = NimmaGuruDarkInverseSurface,
    inverseOnSurface = NimmaGuruDarkInverseOnSurface,
    inversePrimary = NimmaGuruDarkInversePrimary,
    surfaceTint = NimmaGuruDarkSurfaceTint,
)

@Composable
fun NimmaGuruTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default — use our warm palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NimmaGuruDarkColorScheme
        else -> NimmaGuruLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NimmaGuruTypography,
        shapes = NimmaGuruShapes,
        content = content,
    )
}
