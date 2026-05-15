package com.nimmaguru.app.feature.calendar.domain.usecase

import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.feature.calendar.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * One-shot fetch of a session by id. Repo doesn't yet expose a snapshot
 * listener for a single session — list listeners deliver updates for
 * the calendar/joined views, and the detail screen typically reloads
 * on RSVP via the same listener.
 */
class ObserveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<Session> =
        sessionRepository.getSession(sessionId)
}
