package com.nimmaguru.app.feature.profile.domain.usecase

import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe a Guru profile in real-time.
 * R-ARCH-04: Single operator fun invoke().
 */
class ObserveGuruUseCase @Inject constructor(
    private val guruRepository: GuruRepository,
) {
    operator fun invoke(guruId: String): Flow<Guru?> =
        guruRepository.observeGuru(guruId)
}
