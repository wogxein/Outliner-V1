package com.example.domain.model

import java.util.UUID

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
    val role: String, // "user", "assistant" / "model", "system"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

sealed class AiResearchState {
    object Idle : AiResearchState()
    data class Loading(val status: String = "Thinking...") : AiResearchState()
    data class Error(val message: String) : AiResearchState()
}

sealed class AiStreamState {
    object Idle : AiStreamState()
    data class Streaming(val text: String) : AiStreamState()
    data class Done(val text: String) : AiStreamState()
    data class Error(val message: String) : AiStreamState()
}
