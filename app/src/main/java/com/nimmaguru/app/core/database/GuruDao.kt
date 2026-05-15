package com.nimmaguru.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GuruDao {
    // Gurus
    @Query("SELECT * FROM gurus ORDER BY fameScore DESC")
    fun getAllGurus(): Flow<List<GuruEntity>>

    @Query("SELECT * FROM gurus WHERE id = :id")
    suspend fun getGuruById(id: String): GuruEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGurus(gurus: List<GuruEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuru(guru: GuruEntity)

    @Query("DELETE FROM gurus")
    suspend fun deleteAllGurus()

    // Appreciations
    @Query("SELECT * FROM appreciations WHERE guruId = :guruId ORDER BY createdAt DESC")
    fun getAppreciationsForGuru(guruId: String): Flow<List<AppreciationEntity>>

    @Query("SELECT * FROM appreciations ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentAppreciations(limit: Int): Flow<List<AppreciationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppreciations(appreciations: List<AppreciationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppreciation(appreciation: AppreciationEntity)
}
