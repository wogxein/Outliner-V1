package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AIConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class AIMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "model", "system"
    val content: String,
    val sourcesJson: String? = null,
    val searchQueriesJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
