package com.nimmaguru.app.feature.profile.domain.repository

import com.nimmaguru.app.core.model.Guru
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Guru profile operations.
 * R-ARCH-03: Interface in domain, implementation in data.
 */
interface GuruRepository {
    suspend fun getGuru(guruId: String): Result<Guru>
    fun observeGuru(guruId: String): Flow<Guru?>
    fun observeTopGurus(limit: Int = 10): Flow<List<Guru>>
    suspend fun createOrUpdateGuru(guru: Guru): Result<String>
    suspend fun getTopGurus(limit: Int = 20): Result<List<Guru>>
}
