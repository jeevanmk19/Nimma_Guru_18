package com.nimmaguru.app.feature.appreciation.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.core.database.GuruDao
import com.nimmaguru.app.core.database.toDomain
import com.nimmaguru.app.core.database.toEntity
import com.nimmaguru.app.core.model.AppreciationNote
import com.nimmaguru.app.feature.appreciation.domain.repository.AppreciationRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppreciationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val guruDao: GuruDao,
) : AppreciationRepository {

    private companion object {
        const val TAG = "NimmaGuruAppreciationRepo"
    }

    private fun appreciationsCollection(guruId: String) =
        firestore.collection(Constants.COLLECTION_GURUS)
            .document(guruId)
            .collection(Constants.SUBCOLLECTION_APPRECIATIONS)

    override fun observeAppreciations(guruId: String): Flow<List<AppreciationNote>> {
        return guruDao.getAppreciationsForGuru(guruId)
            .map { entities -> entities.map { it.toDomain() } }
            .onStart {
                refreshAppreciations(guruId)
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun refreshAppreciations(guruId: String) {
        GlobalScope.launch {
            try {
                appreciationsCollection(guruId)
                    .whereEqualTo(Constants.FIELD_IS_APPROVED, true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "refreshAppreciations error", error)
                            return@addSnapshotListener
                        }
                        val notes = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(AppreciationNote::class.java)?.copy(id = doc.id)
                        }.orEmpty()
                        
                        if (notes.isNotEmpty()) {
                            GlobalScope.launch {
                                guruDao.insertAppreciations(notes.map { it.toEntity() })
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "refreshAppreciations catch", e)
            }
        }
    }

    override fun observeRecentAppreciations(limit: Int): Flow<List<AppreciationNote>> {
        return guruDao.getRecentAppreciations(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .onStart {
                refreshRecentAppreciations(limit)
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun refreshRecentAppreciations(limit: Int) {
        GlobalScope.launch {
            try {
                firestore.collection(Constants.COLLECTION_ALL_APPRECIATIONS)
                    .whereEqualTo(Constants.FIELD_IS_APPROVED, true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "refreshRecentAppreciations error", error)
                            return@addSnapshotListener
                        }
                        val notes = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(AppreciationNote::class.java)?.copy(id = doc.id)
                        }.orEmpty()
                        
                        if (notes.isNotEmpty()) {
                            GlobalScope.launch {
                                guruDao.insertAppreciations(notes.map { it.toEntity() })
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "refreshRecentAppreciations catch", e)
            }
        }
    }

    override suspend fun postAppreciation(note: AppreciationNote): Result<String> {
        val now = System.currentTimeMillis()
        // Save to Room immediately so the user sees it even if upload is slow/fails
        val temporaryId = "temp_${now}"
        val tempNote = note.copy(id = temporaryId, createdAt = now, isApproved = true)
        
        try {
            guruDao.insertAppreciation(tempNote.toEntity())
            
            val perGuruRef = appreciationsCollection(note.guruId).document()
            val globalRef = firestore.collection(Constants.COLLECTION_ALL_APPRECIATIONS).document()
            val guruRef = firestore.collection(Constants.COLLECTION_GURUS).document(note.guruId)

            val baseData = hashMapOf<String, Any>(
                "guruId" to note.guruId,
                "studentName" to note.studentName,
                "message" to note.message,
                "rating" to note.rating,
                "photoUrl" to (note.photoUrl ?: ""),
                "createdAt" to now,
                Constants.FIELD_IS_APPROVED to true, 
            )

            firestore.runTransaction { transaction ->
                val guruSnapshot = transaction.get(guruRef)
                
                val currentCount = guruSnapshot.getLong("appreciationCount")?.toInt() ?: 0
                val currentAvg = guruSnapshot.getDouble("avgRating")?.toFloat() ?: 0f
                val totalSessions = guruSnapshot.getLong("totalSessions")?.toInt() ?: 0
                
                val newCount = currentCount + 1
                val newAvg = ((currentAvg * currentCount) + note.rating) / newCount
                val newFameScore = (newCount * 2f) + totalSessions + (newAvg * 5f)

                transaction.set(perGuruRef, baseData)
                transaction.set(globalRef, baseData + ("noteId" to perGuruRef.id))
                
                transaction.update(
                    guruRef,
                    "appreciationCount", newCount,
                    "avgRating", newAvg,
                    "fameScore", newFameScore
                )
                null
            }.await()

            // Update local cache with real ID and remove temp
            val savedNote = note.copy(id = perGuruRef.id, createdAt = now, isApproved = true)
            guruDao.insertAppreciation(savedNote.toEntity())
            
            return Result.success(perGuruRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "postAppreciation failed", e)
            // If it failed but we saved locally, we could return success or a specific result
            // For now, return failure but note that it might be in Room
            return Result.failure(e)
        }
    }
}
