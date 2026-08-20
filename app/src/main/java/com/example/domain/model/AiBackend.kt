package com.example.domain.model

import java.util.UUID

data class AiBackend(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,      // e.g. "https://generativelanguage.googleapis.com/v1beta/openai"
    val apiKey: String,
    val models: String,   // comma-separated, e.g. "gemini-3.5-flash-lite"
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val modelsList: List<String>
        get() = getModelList()

    fun getModelList(): List<String> {
        return models.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun getPrimaryModel(): String {
        return getModelList().firstOrNull() ?: "gemini-3.5-flash-lite"
    }

    companion object {
        fun createDefault(apiKey: String = ""): AiBackend {
            return AiBackend(
                id = "default-gemini",
                name = "Gemini",
                url = "https://generativelanguage.googleapis.com/v1beta/openai/",
                apiKey = apiKey,
                models = "gemini-3.5-flash-lite",
                isDefault = true
            )
        }
    }
}
