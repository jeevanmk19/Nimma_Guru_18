package com.nimmaguru.app.feature.calendar.domain.usecase

import com.nimmaguru.app.feature.calendar.domain.repository.SessionRepository
import javax.inject.Inject

class RsvpToSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: String, userId: String): Result<Unit> =
        sessionRepository.rsvpToSession(sessionId, userId)
}
