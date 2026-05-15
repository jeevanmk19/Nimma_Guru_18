package com.nimmaguru.app.feature.discover.domain.usecase

import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
import javax.inject.Inject

/**
 * Use case to search and filter Gurus.
 * R-ARCH-04: Single invoke().
 */
class SearchGurusUseCase @Inject constructor(
    private val guruRepository: GuruRepository,
) {
    suspend operator fun invoke(
        query: String = "",
        skills: List<String> = emptyList(),
        district: String = "",
    ): Result<List<Guru>> {
        // Get all public gurus, then filter client-side
        // P-FB-09: Firestore doesn't support full-text search,
        // so we fetch + filter. For 10K+ scale, use Algolia/Typesense.
        return guruRepository.getTopGurus(limit = 100).map { gurus ->
            gurus.filter { guru ->
                val matchesQuery = query.isBlank() ||
                    guru.nameEn.contains(query, ignoreCase = true) ||
                    guru.nameKn.contains(query, ignoreCase = true) ||
                    guru.village.contains(query, ignoreCase = true) ||
                    guru.skills.any { it.contains(query, ignoreCase = true) }

                val matchesSkills = skills.isEmpty() ||
                    guru.skills.any { it in skills }

                val matchesDistrict = district.isBlank() ||
                    guru.district.equals(district, ignoreCase = true)

                matchesQuery && matchesSkills && matchesDistrict
            }
        }
    }
}
