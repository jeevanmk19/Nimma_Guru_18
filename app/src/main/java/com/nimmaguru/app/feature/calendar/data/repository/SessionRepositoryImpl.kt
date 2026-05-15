package com.nimmaguru.app.feature.calendar.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.core.model.Session
import com.nimmaguru.app.feature.calendar.domain.repository.SessionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase implementation of SessionRepository.
 * R-FB-05: All Firebase SDK calls in data layer only.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : SessionRepository {

    private companion object {
        const val TAG = "NimmaGuruSessionRepo"
    }

    private val sessionsCollection = firestore.collection(Constants.COLLECTION_SESSIONS)

    override fun observeUpcomingSessions(): Flow<List<Session>> = callbackFlow {
        val listener = sessionsCollection
            .whereGreaterThan("date", System.currentTimeMillis())
            .orderBy("date", Query.Direction.ASCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeUpcomingSessions error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Session::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    override fun observeSessionsByGuru(guruId: String): Flow<List<Session>> = callbackFlow {
        val listener = sessionsCollection
            .whereEqualTo("guruId", guruId)
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeSessionsByGuru error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Session::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getSession(sessionId: String): Result<Session> {
        return try {
            val doc = sessionsCollection.document(sessionId).get().await()
            val session = doc.toObject(Session::class.java)?.copy(id = doc.id)
            if (session != null) {
                Result.success(session)
            } else {
                Result.failure(Exception("Session not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createSession(session: Session): Result<String> {
        return try {
            val data = hashMapOf(
                "guruId" to session.guruId,
                "guruNameEn" to session.guruNameEn,
                "guruNameKn" to session.guruNameKn,
                "guruPhotoUrl" to session.guruPhotoUrl,
                "subject" to session.subject,
                "description" to session.description,
                "date" to session.date,
                "startTime" to session.startTime,
                "endTime" to session.endTime,
                "venue" to session.venue,
                "maxStudents" to session.maxStudents,
                "attendees" to emptyList<String>(),
                "status" to Constants.SESSION_UPCOMING,
            )
            val docRef = sessionsCollection.add(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rsvpToSession(sessionId: String, userId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val docRef = sessionsCollection.document(sessionId)
                val snapshot = transaction.get(docRef)

                @Suppress("UNCHECKED_CAST")
                val attendees = (snapshot.get("attendees") as? List<String>).orEmpty()
                val maxStudents = snapshot.getLong("maxStudents") ?: 15

                if (attendees.size >= maxStudents) {
                    throw Exception("Session is full")
                }
                if (userId in attendees) {
                    throw Exception("Already registered")
                }

                transaction.update(docRef, "attendees", FieldValue.arrayUnion(userId))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelRsvp(sessionId: String, userId: String): Result<Unit> {
        return try {
            sessionsCollection.document(sessionId)
                .update("attendees", FieldValue.arrayRemove(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeJoinedSessions(userId: String): Flow<List<Session>> = callbackFlow {
        val listener = sessionsCollection
            .whereArrayContains("attendees", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeJoinedSessions error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Session::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }
}
