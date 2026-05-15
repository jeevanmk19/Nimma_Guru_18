package com.nimmaguru.app.core.model

import androidx.compose.runtime.Immutable

/**
 * Domain model for an appreciation/thank-you note.
 * Maps to Firestore `gurus/{guruId}/appreciations/{noteId}` sub-collection
 * from idea-architecture.md Section 5.
 *
 * R-KT-02: All fields are val (immutable).
 */
@Immutable
data class AppreciationNote(
    val id: String = "",
    val guruId: String = "",
    val studentName: String = "",
    val message: String = "",
    val photoUrl: String? = null, // Optional image
    val rating: Int = 0, // 1-5 stars
    val isApproved: Boolean = false, // AI moderation flag
    val createdAt: Long = 0L,
)
