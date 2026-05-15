package com.nimmaguru.app.feature.auth.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.core.model.User
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    private companion object {
        const val TIMEOUT_MS = 30000L // Increased to 30s
    }

    private val usersCollection = firestore.collection(Constants.COLLECTION_USERS)

    override suspend fun getUser(userId: String): Result<User?> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val doc = usersCollection.document(userId).get().await()
                if (doc.exists()) {
                    val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                    Result.success(user)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "getUser failed", e)
            Result.failure(e)
        }
    }

    override fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error observing user: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)?.copy(id = snapshot.id)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val data = hashMapOf(
                    "name" to user.name,
                    "district" to user.district,
                    "phone" to user.phone,
                    "role" to user.role,
                    "langPref" to user.langPref,
                    "updatedAt" to System.currentTimeMillis()
                )
                
                val doc = usersCollection.document(user.id).get().await()
                if (!doc.exists()) {
                    data["createdAt"] = System.currentTimeMillis()
                }

                usersCollection.document(user.id).set(data, SetOptions.merge()).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "saveUser failed", e)
            Result.failure(e)
        }
    }

    override suspend fun upgradeToGuru(userId: String): Result<Unit> {
        return try {
            withTimeout(TIMEOUT_MS) {
                val data = mapOf(
                    "role" to Constants.ROLE_GURU,
                    "updatedAt" to System.currentTimeMillis()
                )
                usersCollection.document(userId)
                    .set(data, SetOptions.merge())
                    .await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "upgradeToGuru failed", e)
            Result.failure(e)
        }
    }
}
