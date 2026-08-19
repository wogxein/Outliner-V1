package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.AIMessage
import com.example.domain.model.GroundingCitation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAiService {

    private const val TAG = "GeminiAiService"
    const val DEFAULT_MODEL = "gemini-2.5-flash"
    const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class AiResponse(
        val text: String,
        val citations: List<GroundingCitation> = emptyList(),
        val searchQueries: List<String> = emptyList(),
        val rawResponse: String? = null
    )

    /**
     * Executes multi-turn research/chat request with Google Search grounding.
     * Supports user-configured API key, custom Base URL, and custom Model ID.
     */
    suspend fun research(
        messages: List<AIMessage>,
        systemPrompt: String? = null,
        enableSearchGrounding: Boolean = true,
        customApiKey: String? = null,
        customBaseUrl: String? = null,
        customModelId: String? = null
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = if (!customApiKey.isNullOrBlank()) {
                customApiKey.trim()
            } else {
                try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
            }

            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException("AI API key is not configured. Please enter your API key in Settings or AI Research Setup.")
                )
            }

            val baseUrl = if (!customBaseUrl.isNullOrBlank()) customBaseUrl.trim().removeSuffix("/") else DEFAULT_BASE_URL
            val model = if (!customModelId.isNullOrBlank()) customModelId.trim() else DEFAULT_MODEL

            val requestJson = JSONObject()

            // 1. System instruction
            val defaultSystemText = buildString {
                append("You are an intelligent, accurate, and helpful Web Research Assistant deeply integrated into a hierarchical note-taking & outliner application. ")
                append("When searching the web, provide precise, up-to-date facts with clear explanations. ")
                append("Format your answers with clean Markdown headings (e.g. ## Topic, ### Subtopic) and bullet points (- Point) where appropriate, so they can easily be transformed into outline notes. ")
                append("Ensure all factual statements from the web are backed by reliable sources.")
                if (!systemPrompt.isNullOrBlank()) {
                    append("\n\nContext & Specific Instructions:\n")
                    append(systemPrompt)
                }
            }

            val systemObj = JSONObject()
            val systemParts = JSONArray()
            systemParts.put(JSONObject().put("text", defaultSystemText))
            systemObj.put("parts", systemParts)
            requestJson.put("systemInstruction", systemObj)

            // 2. Contents (multi-turn conversation)
            val contentsArray = JSONArray()
            for (msg in messages) {
                if (msg.content.isBlank()) continue
                val role = if (msg.role == "user") "user" else "model"
                val contentObj = JSONObject()
                contentObj.put("role", role)
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", msg.content))
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }
            requestJson.put("contents", contentsArray)

            // 3. Tools: Google Search grounding
            if (enableSearchGrounding) {
                val toolsArray = JSONArray()
                val toolObj = JSONObject()
                toolObj.put("googleSearch", JSONObject())
                toolsArray.put(toolObj)
                requestJson.put("tools", toolsArray)
            }

            // 4. Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            requestJson.put("generationConfig", genConfig)

            val url = if (baseUrl.contains("?")) {
                "$baseUrl/$model:generateContent&key=$apiKey"
            } else {
                "$baseUrl/$model:generateContent?key=$apiKey"
            }

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "AI API failed with code ${response.code}: $responseString")
                val errorMsg = try {
                    val errJson = JSONObject(responseString)
                    errJson.optJSONObject("error")?.optString("message") ?: "API Error HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val resultJson = JSONObject(responseString)
            val candidates = resultJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No response generated by model."))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")

            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    val t = p.optString("text", "")
                    if (t.isNotEmpty()) {
                        textBuilder.append(t)
                    }
                }
            }

            // Extract Grounding metadata
            val citations = mutableListOf<GroundingCitation>()
            val searchQueries = mutableListOf<String>()

            val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                // Search queries
                val queriesArray = groundingMetadata.optJSONArray("webSearchQueries")
                if (queriesArray != null) {
                    for (i in 0 until queriesArray.length()) {
                        val q = queriesArray.optString(i)
                        if (q.isNotBlank()) searchQueries.add(q)
                    }
                }

                // Grounding chunks (Sources)
                val chunksArray = groundingMetadata.optJSONArray("groundingChunks")
                if (chunksArray != null) {
                    for (i in 0 until chunksArray.length()) {
                        val chunk = chunksArray.getJSONObject(i)
                        val web = chunk.optJSONObject("web")
                        if (web != null) {
                            val uri = web.optString("uri", "")
                            val title = web.optString("title", "").ifBlank { uri }
                            if (uri.isNotBlank()) {
                                citations.add(GroundingCitation(title = title, url = uri))
                            }
                        }
                    }
                }
            }

            val finalCitations = citations.distinctBy { it.url }

            Result.success(
                AiResponse(
                    text = textBuilder.toString().trim(),
                    citations = finalCitations,
                    searchQueries = searchQueries,
                    rawResponse = responseString
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during AI research call", e)
            Result.failure(e)
        }
    }
}
