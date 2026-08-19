package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun getActiveFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isDeleted = 0")
    suspend fun getActiveFoldersList(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    fun observeFolderById(id: String): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE parentId = :parentId AND isDeleted = 0 ORDER BY sortOrder ASC")
    suspend fun getChildrenFolders(parentId: String): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("UPDATE folders SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteFolder(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE folders SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFolder(id: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun hardDeleteFolder(id: String)

    @Query("DELETE FROM folders WHERE isDeleted = 1")
    suspend fun emptyTrashFolders()

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersForBackup(): List<FolderEntity>

    @Query("DELETE FROM folders")
    suspend fun clearAllFolders()
}
