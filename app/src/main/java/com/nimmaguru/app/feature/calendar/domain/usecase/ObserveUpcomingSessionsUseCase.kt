package com.nimmaguru.app.feature.calendar.domain.usecase

import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.feature.calendar.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUpcomingSessionsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<List<Session>> =
        sessionRepository.observeUpcomingSessions()
}
