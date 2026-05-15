package com.nimmaguru.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Nimma Guru Shape System
 * Based on idea-screens.md Section 2.
 *
 * Small:  12dp rounded  — Chips, small buttons
 * Medium: 16dp rounded  — Cards, text fields
 * Large:  24dp rounded  — Bottom sheets, dialogs
 *
 * R-COMP-06: Never hardcode shapes — always use MaterialTheme.shapes.*
 */
val NimmaGuruShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
