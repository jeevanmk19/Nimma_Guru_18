package com.nimmaguru.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nimmaguru.app.R

/**
 * Nimma Guru Typography Scale — Elderly-First Design
 * Based on idea-screens.md Section 2.
 *
 * R-COMP-04: Minimum 18sp for body text — NO EXCEPTIONS.
 * R-L10N-02: Inter (EN) + Noto Sans Kannada (KN) — bundled, not system.
 *
 * Note: Font files need to be placed in res/font/ directory.
 * For now using default fonts until font files are bundled.
 */

// Font families - using default system fonts initially
// TODO: Bundle Inter and Noto Sans Kannada in res/font/ when fonts are available
val InterFontFamily = FontFamily.Default
val NotoSansKannadaFontFamily = FontFamily.Default

val NimmaGuruTypography = Typography(
    // Display Large: 36sp / Bold — Wall of Fame hero
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp,
    ),

    // Headline Large: 28sp / Bold — Screen titles
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),

    // Headline Medium: 24sp / Bold — Sub-screen headers
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    // Title Large: 24sp / SemiBold — Card titles, Guru name
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    // Title Medium: 20sp / Medium — Section headers
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp,
    ),

    // Title Small: 18sp / Medium
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),

    // Body Large: 18sp / Regular — DEFAULT BODY TEXT (MINIMUM per R-COMP-04)
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp,
    ),

    // Body Medium: 16sp / Regular — Secondary info
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp,
    ),

    // Body Small: 16sp / Regular — Minimum readable size
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.4.sp,
    ),

    // Label Large: 16sp / Medium — Buttons, chips
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),

    // Label Medium: 14sp / Medium — Captions only
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),

    // Label Small: 14sp / Medium
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
)
