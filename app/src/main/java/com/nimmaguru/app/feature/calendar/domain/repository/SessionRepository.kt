package com.nimmaguru.app.feature.calendar.domain.repository

import com.nimmaguru.app.core.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Session operations.
 * R-ARCH-03: Interface in domain, implementation in data.
 */
interface SessionRepository {
    fun observeUpcomingSessions(): Flow<List<Session>>
    fun observeSessionsByGuru(guruId: String): Flow<List<Session>>
    suspend fun getSession(sessionId: String): Result<Session>
    suspend fun createSession(session: Session): Result<String>
    suspend fun rsvpToSession(sessionId: String, userId: String): Result<Unit>
    suspend fun cancelRsvp(sessionId: String, userId: String): Result<Unit>
    fun observeJoinedSessions(userId: String): Flow<List<Session>>
}
