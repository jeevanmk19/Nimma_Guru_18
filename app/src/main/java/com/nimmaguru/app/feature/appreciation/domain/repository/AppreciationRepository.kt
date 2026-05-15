package com.nimmaguru.app.feature.appreciation.domain.repository

import com.nimmaguru.app.core.model.AppreciationNote
import kotlinx.coroutines.flow.Flow

interface AppreciationRepository {
    fun observeAppreciations(guruId: String): Flow<List<AppreciationNote>>
    fun observeRecentAppreciations(limit: Int = 20): Flow<List<AppreciationNote>>
    suspend fun postAppreciation(note: AppreciationNote): Result<String>
}
