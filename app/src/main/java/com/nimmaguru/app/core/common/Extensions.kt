package com.nimmaguru.app.core.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kotlin extension functions used across the app.
 */

/**
 * Format a timestamp to a human-readable date string.
 */
fun Long.toFormattedDate(pattern: String = "MMM dd, yyyy"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Format a timestamp to a time string.
 */
fun Long.toFormattedTime(pattern: String = "hh:mm a"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Safely get a value or return a default — avoids !! (R-KT-01).
 */
fun String?.orDefault(default: String = ""): String = this ?: default

/**
 * Walk a Compose [Context] (often a `ContextWrapper` proxy) up to the host
 * [Activity]. Required by Firebase Phone Auth, which needs an Activity for
 * reCAPTCHA / Play Integrity verification.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
