package com.nimmaguru.app.feature.profile.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.core.database.GuruDao
import com.nimmaguru.app.core.database.toDomain
import com.nimmaguru.app.core.database.toEntity
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
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
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuruRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val guruDao: GuruDao,
) : GuruRepository {

    private companion object {
        const val TAG = "NimmaGuruGuruRepo"
        const val TIMEOUT_MS = 30000L
    }

    private val gurusCollection = firestore.collection(Constants.COLLECTION_GURUS)

    override suspend fun getGuru(guruId: String): Result<Guru> {
        return try {
            // Try local first
            val local = guruDao.getGuruById(guruId)
            if (local != null) return Result.success(local.toDomain())

            withTimeout(TIMEOUT_MS) {
                val doc = gurusCollection.document(guruId).get().await()
                val guru = doc.toObject(Guru::class.java)?.copy(id = doc.id)
                if (guru != null) {
                    guruDao.insertGuru(guru.toEntity())
                    Result.success(guru)
                } else Result.failure(Exception("Guru not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getGuru failed", e)
            Result.failure(e)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun observeGuru(guruId: String): Flow<Guru?> = callbackFlow {
        // First try to emit what we have in DB
        val local = guruDao.getGuruById(guruId)
        if (local != null) trySend(local.toDomain())

        val listener = gurusCollection.document(guruId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeGuru error", error)
                    return@addSnapshotListener
                }
                val guru = snapshot?.toObject(Guru::class.java)?.copy(id = snapshot.id)
                if (guru != null) {
                    trySend(guru)
                    // Update local cache
                    GlobalScope.launch {
                        guruDao.insertGuru(guru.toEntity())
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    override fun observeTopGurus(limit: Int): Flow<List<Guru>> {
        return guruDao.getAllGurus()
            .map { entities -> entities.map { it.toDomain() }.take(limit) }
            .onStart {
                // Trigger a refresh when starting to observe
                refreshTopGurus(limit)
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun refreshTopGurus(limit: Int) {
        GlobalScope.launch {
            getTopGurus(limit)
        }
    }

    override suspend fun createOrUpdateGuru(guru: Guru): Result<String> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val uid = auth.currentUser?.uid
                    ?: return@withTimeout Result.failure(Exception("Not authenticated"))

                val existingDoc = gurusCollection.document(uid).get().await()
                val isNew = !existingDoc.exists()

                val totalSessions = if (isNew) 0L else existingDoc.getLong("totalSessions") ?: 0L
                val appreciationCount = if (isNew) 0L else existingDoc.getLong("appreciationCount") ?: 0L
                val avgRating = if (isNew) 0.0 else (existingDoc.getDouble("avgRating") ?: 0.0)
                val fameScore = (appreciationCount * 2.0) + totalSessions + (avgRating * 5.0)

                val data = hashMapOf<String, Any>(
                    "ownerId" to uid,
                    "nameEn" to guru.nameEn,
                    "nameKn" to guru.nameKn,
                    "village" to guru.village,
                    "district" to guru.district,
                    "skills" to guru.skills,
                    "availability" to guru.availability,
                    "bioEn" to guru.bioEn,
                    "bioKn" to guru.bioKn,
                    "photoUrl" to guru.photoUrl,
                    "isPublic" to guru.isPublic,
                    "langPref" to guru.langPref,
                    Constants.FIELD_FAME_SCORE to fameScore,
                    "updatedAt" to System.currentTimeMillis(),
                )

                if (isNew) {
                    data["createdAt"] = System.currentTimeMillis()
                    data["totalSessions"] = 0L
                    data["totalStudents"] = 0L
                    data["appreciationCount"] = 0L
                    data["avgRating"] = 0.0
                }

                gurusCollection.document(uid).set(data, SetOptions.merge()).await()
                
                // Also update local cache
                val updatedGuru = guru.copy(
                    id = uid,
                    fameScore = fameScore.toFloat(),
                    updatedAt = System.currentTimeMillis()
                )
                guruDao.insertGuru(updatedGuru.toEntity())
                
                Result.success(uid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createOrUpdateGuru failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getTopGurus(limit: Int): Result<List<Guru>> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val snapshot = gurusCollection
                    .whereEqualTo(Constants.FIELD_IS_PUBLIC, true)
                    .orderBy(Constants.FIELD_FAME_SCORE, Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                val gurus = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Guru::class.java)?.copy(id = doc.id)
                }
                
                if (gurus.isNotEmpty()) {
                    guruDao.insertGurus(gurus.map { it.toEntity() })
                }
                
                Result.success(gurus)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTopGurus failed", e)
            // Fallback to local if network fails
            val local = guruDao.getAllGurus().map { entities -> 
                entities.map { it.toDomain() }.take(limit) 
            }.firstOrNull() ?: emptyList()
            Result.success(local)
        }
    }
}
