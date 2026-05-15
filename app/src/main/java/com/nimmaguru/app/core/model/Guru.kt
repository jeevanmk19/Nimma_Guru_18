package com.nimmaguru.app.core.model

import androidx.compose.runtime.Immutable

/**
 * Domain model for a Guru profile.
 * Maps to the Firestore `gurus/{guruId}` document schema from
 * idea-architecture.md Section 5.
 *
 * Wall-of-Fame ranking is now driven by a denormalized [fameScore]
 * field that is recomputed and written every time the Guru document
 * is touched (P1.3). [computedFameScore] keeps the original formula
 * available for client-side preview / sort tie-breaking.
 *
 * @Immutable annotation helps Compose skip recomposition (P-NICHE-03).
 * R-KT-02: All fields are val.
 */
@Immutable
data class Guru(
    val id: String = "",
    val nameEn: String = "",
    val nameKn: String = "",
    val photoUrl: String = "",
    val village: String = "",
    val district: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val skills: List<String> = emptyList(),
    val availability: Map<String, String> = emptyMap(), // {"Sat": "10:00-12:00"}
    val bioEn: String = "",
    val bioKn: String = "",
    val phone: String = "", // Private — never shown publicly
    val isPublic: Boolean = true,
    val contactConsent: Boolean = false,
    val langPref: String = "kn",
    val totalSessions: Int = 0,
    val totalStudents: Int = 0,
    val appreciationCount: Int = 0,
    val avgRating: Float = 0f,
    /** Denormalized for `orderBy("fameScore", DESCENDING)` queries. */
    val fameScore: Float = 0f,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /**
     * Wall of Fame ranking score — formula from idea-features.md FR10:
     * (appreciationCount * 2) + totalSessions + (avgRating * 5)
     *
     * This is what the persisted [fameScore] field should hold; we
     * recompute it on every write.
     */
    val computedFameScore: Float
        get() = (appreciationCount * 2f) + totalSessions + (avgRating * 5f)
}
