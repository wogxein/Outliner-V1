package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.OutlineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutlineItemDao {
    @Query("SELECT * FROM outline_items WHERE noteId = :noteId ORDER BY sortOrder ASC")
    fun getItemsForNote(noteId: String): Flow<List<OutlineItemEntity>>

    @Query("SELECT * FROM outline_items WHERE noteId = :noteId ORDER BY sortOrder ASC")
    suspend fun getItemsForNoteList(noteId: String): List<OutlineItemEntity>

    @Query("SELECT * FROM outline_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): OutlineItemEntity?

    @Query("SELECT * FROM outline_items WHERE noteId = :noteId AND parentId IS :parentId ORDER BY sortOrder ASC")
    suspend fun getChildren(noteId: String, parentId: String?): List<OutlineItemEntity>

    @Query("SELECT * FROM outline_items WHERE noteId = :noteId AND (parentId = :parentId OR (:parentId IS NULL AND parentId IS NULL)) ORDER BY sortOrder ASC")
    suspend fun getDirectSiblings(noteId: String, parentId: String?): List<OutlineItemEntity>

    @Query("SELECT * FROM outline_items WHERE text LIKE '%' || :query || '%'")
    suspend fun searchItems(query: String): List<OutlineItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: OutlineItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OutlineItemEntity>)

    @Update
    suspend fun updateItem(item: OutlineItemEntity)

    @Update
    suspend fun updateItems(items: List<OutlineItemEntity>)

    @Query("DELETE FROM outline_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM outline_items WHERE id IN (:ids)")
    suspend fun deleteItems(ids: List<String>)

    @Query("DELETE FROM outline_items WHERE noteId = :noteId")
    suspend fun deleteItemsForNote(noteId: String)

    @Query("SELECT * FROM outline_items")
    suspend fun getAllItemsForBackup(): List<OutlineItemEntity>

    @Query("DELETE FROM outline_items")
    suspend fun clearAllItems()

    @Transaction
    suspend fun replaceItemsForNote(noteId: String, items: List<OutlineItemEntity>) {
        deleteItemsForNote(noteId)
        insertItems(items)
    }
}
