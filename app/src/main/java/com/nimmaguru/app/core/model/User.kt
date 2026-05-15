package com.nimmaguru.app.core.model

import androidx.compose.runtime.Immutable

/**
 * Domain model for a basic User profile.
 * Maps to the Firestore `users/{userId}` document schema.
 */
@Immutable
data class User(
    val id: String = "",
    val phone: String = "",
    val name: String = "",
    val district: String = "",
    val role: String = "student", // "student", "guru", "admin"
    val langPref: String = "kn",
    val fcmToken: String = "",
    val guruId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
