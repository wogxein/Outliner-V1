package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AiBackendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiBackendDao {
    @Query("SELECT * FROM ai_backends ORDER BY createdAt ASC")
    fun getAllBackends(): Flow<List<AiBackendEntity>>

    @Query("SELECT * FROM ai_backends ORDER BY createdAt ASC")
    suspend fun getAllBackendsList(): List<AiBackendEntity>

    @Query("SELECT * FROM ai_backends WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultBackend(): AiBackendEntity?

    @Query("SELECT * FROM ai_backends WHERE id = :id LIMIT 1")
    suspend fun getBackendById(id: String): AiBackendEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackend(backend: AiBackendEntity)

    @Update
    suspend fun updateBackend(backend: AiBackendEntity)

    @Query("DELETE FROM ai_backends WHERE id = :id")
    suspend fun deleteBackend(id: String)
}
