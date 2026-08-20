package com.example.domain.model

import org.json.JSONObject
import java.util.UUID

data class AiBotConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Custom AI Bot",
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelId: String = "",
    val systemPrompt: String = "",
    val isDefault: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("apiKey", apiKey)
            put("baseUrl", baseUrl)
            put("modelId", modelId)
            put("systemPrompt", systemPrompt)
            put("isDefault", isDefault)
        }
    }

    companion object {
        const val HINT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"
        const val HINT_MODEL_ID = "gemini-2.5-flash"

        fun fromJson(json: JSONObject): AiBotConfig {
            return AiBotConfig(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Custom Bot"),
                apiKey = json.optString("apiKey", ""),
                baseUrl = json.optString("baseUrl", ""),
                modelId = json.optString("modelId", ""),
                systemPrompt = json.optString("systemPrompt", ""),
                isDefault = json.optBoolean("isDefault", false)
            )
        }

        fun defaultBots(): List<AiBotConfig> = listOf(
            AiBotConfig(
                id = "custom-bot-1",
                name = "My AI Bot",
                apiKey = "",
                baseUrl = "",
                modelId = "",
                systemPrompt = "You are a helpful study and research assistant. Structure responses with clean Markdown outlines and bullet points.",
                isDefault = true
            )
        )
    }
}
