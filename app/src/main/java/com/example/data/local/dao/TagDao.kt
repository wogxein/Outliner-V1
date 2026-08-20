package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ItemTagCrossRef
import com.example.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: ItemTagCrossRef)

    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId AND tagId = :tagId")
    suspend fun deleteCrossRef(itemId: String, tagId: String)

    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId")
    suspend fun removeAllTagsFromItem(itemId: String)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN item_tag_cross_ref c ON t.id = c.tagId
        WHERE c.itemId = :itemId
    """)
    suspend fun getTagsForItem(itemId: String): List<TagEntity>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsForBackup(): List<TagEntity>

    @Query("SELECT * FROM item_tag_cross_ref")
    suspend fun getAllCrossRefsForBackup(): List<ItemTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<ItemTagCrossRef>)

    @Query("DELETE FROM tags")
    suspend fun clearAllTags()

    @Query("DELETE FROM item_tag_cross_ref")
    suspend fun clearAllCrossRefs()
}
