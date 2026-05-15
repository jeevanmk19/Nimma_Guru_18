package com.nimmaguru.app.core.model

import androidx.compose.runtime.Immutable

/**
 * Domain model for a mentoring session.
 * Maps to the Firestore `gurus/{guruId}/sessions/{sessionId}` sub-collection
 * from idea-architecture.md Section 5.
 *
 * R-KT-02: All fields are val (immutable).
 */
@Immutable
data class Session(
    val id: String = "",
    val guruId: String = "",
    val guruNameEn: String = "", // Denormalized for display
    val guruNameKn: String = "",
    val guruPhotoUrl: String = "",
    val subject: String = "",
    val description: String = "",
    val date: Long = 0L, // Timestamp
    val startTime: String = "", // "10:00 AM"
    val endTime: String = "", // "12:00 PM"
    val venue: String = "", // Samudaya Bhavana name
    val venueLatitude: Double = 0.0,
    val venueLongitude: Double = 0.0,
    val maxStudents: Int = 15,
    val attendees: List<String> = emptyList(), // User IDs
    val status: String = "upcoming", // "upcoming" | "completed" | "cancelled"
) {
    val attendeeCount: Int get() = attendees.size
    val isFull: Boolean get() = attendeeCount >= maxStudents
    val isUpcoming: Boolean get() = status == "upcoming"
}
