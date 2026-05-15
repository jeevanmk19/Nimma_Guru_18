package com.nimmaguru.app.feature.auth.domain.repository

import com.nimmaguru.app.core.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(userId: String): Result<User?>
    fun observeUser(userId: String): Flow<User?>
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun upgradeToGuru(userId: String): Result<Unit>
}
