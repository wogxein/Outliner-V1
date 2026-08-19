package com.example.domain.model

import java.util.UUID

data class GroundingCitation(
    val title: String,
    val url: String,
    val snippet: String? = null
)

data class AIConversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New AI Research",
    val noteId: String? = null,
    val folderId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

data class AIMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user", "model", "system"
    val content: String,
    val citations: List<GroundingCitation> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

sealed class AiResearchState {
    object Idle : AiResearchState()
    data class Loading(val status: String = "Searching the web...") : AiResearchState()
    data class Error(val message: String) : AiResearchState()
}
