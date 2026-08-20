package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AIConversationEntity
import com.example.data.local.entity.AIMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {

    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<AIConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE noteId = :noteId ORDER BY updatedAt DESC")
    fun getConversationsForNote(noteId: String): Flow<List<AIConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getConversationsForFolder(folderId: String): Flow<List<AIConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): AIConversationEntity?

    @Query("SELECT * FROM ai_conversations WHERE id = :id LIMIT 1")
    fun observeConversationById(id: String): Flow<AIConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AIConversationEntity)

    @Update
    suspend fun updateConversation(conversation: AIConversationEntity)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<AIMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesForConversationList(conversationId: String): List<AIMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AIMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<AIMessageEntity>)

    @Query("DELETE FROM ai_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
}
