package com.nimmaguru.app.feature.calendar.domain.usecase

import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.feature.calendar.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionsByGuruUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(guruId: String): Flow<List<Session>> =
        sessionRepository.observeSessionsByGuru(guruId)
}
