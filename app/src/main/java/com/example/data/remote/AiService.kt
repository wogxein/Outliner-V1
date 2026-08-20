package com.example.data.remote

import com.example.BuildConfig
import com.example.domain.model.AIMessage
import com.example.domain.model.AiBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiResponse(
    val text: String
)

class AiService(
    private val backend: AiBackend
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getResolvedApiKey(): String {
        return if (backend.apiKey.isNotBlank()) {
            backend.apiKey.trim()
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun getEndpoint(): String {
        return backend.url.trim().removeSuffix("/") + "/chat/completions"
    }

    private fun buildMessagesJsonArray(messages: List<AIMessage>, systemPrompt: String?): JSONArray {
        val messagesArray = JSONArray()

        // Add system prompt if provided
        if (!systemPrompt.isNullOrBlank()) {
            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", systemPrompt.trim())
            messagesArray.put(systemMsg)
        }

        // Add conversation messages
        messages.forEach { msg ->
            if (msg.content.isNotBlank()) {
                val msgObj = JSONObject()
                val role = when (msg.role.lowercase()) {
                    "assistant", "model" -> "assistant"
                    "system" -> "system"
                    else -> "user"
                }
                msgObj.put("role", role)
                msgObj.put("content", msg.content)
                messagesArray.put(msgObj)
            }
        }

        return messagesArray
    }

    /**
     * Non-streaming chat request formatted for OpenAI-compatible endpoints.
     */
    suspend fun chat(
        messages: List<AIMessage>,
        systemPrompt: String? = null,
        temperature: Double = 0.5
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        try {
            val endpoint = getEndpoint()
            val apiKey = getResolvedApiKey()
            val model = backend.getPrimaryModel()

            val requestBodyJson = JSONObject()
            requestBodyJson.put("model", model)
            requestBodyJson.put("stream", false)
            requestBodyJson.put("temperature", temperature)
            requestBodyJson.put("messages", buildMessagesJsonArray(messages, systemPrompt))

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                return@withContext Result.failure(
                    Exception("HTTP ${response.code}: ${response.message}\n$errorBody")
                )
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.optJSONObject("message")
                val content = message?.optString("content") ?: ""
                Result.success(AiResponse(text = content.trim()))
            } else {
                Result.failure(Exception("No response choices returned by model"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Streaming chat request using Server-Sent Events (SSE).
     * Throttles forwarded chunks to caller at 50ms intervals.
     */
    suspend fun streamChat(
        messages: List<AIMessage>,
        systemPrompt: String? = null,
        temperature: Double = 0.5,
        onChunk: (String) -> Unit,
        onDone: () -> Unit,
        onError: (Exception) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val endpoint = getEndpoint()
            val apiKey = getResolvedApiKey()
            val model = backend.getPrimaryModel()

            val requestBodyJson = JSONObject()
            requestBodyJson.put("model", model)
            requestBodyJson.put("stream", true)
            requestBodyJson.put("temperature", temperature)
            requestBodyJson.put("messages", buildMessagesJsonArray(messages, systemPrompt))

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .addHeader("Accept", "text/event-stream")
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                onError(Exception("HTTP ${response.code}: ${response.message}\n$errorBody"))
                return@withContext
            }

            val source = response.body?.source()
            if (source == null) {
                onError(Exception("Empty response body received"))
                return@withContext
            }

            val reader = source.inputStream().bufferedReader()
            val chunkBuffer = StringBuilder()
            var lastForwardTime = System.currentTimeMillis()

            while (true) {
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("data:")) {
                    val data = trimmed.substring(5).trim()
                    if (data == "[DONE]") {
                        if (chunkBuffer.isNotEmpty()) {
                            onChunk(chunkBuffer.toString())
                            chunkBuffer.setLength(0)
                        }
                        onDone()
                        return@withContext
                    }

                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val firstChoice = choices.getJSONObject(0)
                            val delta = firstChoice.optJSONObject("delta")
                            val content = delta?.optString("content") ?: ""
                            if (content.isNotEmpty()) {
                                chunkBuffer.append(content)
                                val now = System.currentTimeMillis()
                                if (now - lastForwardTime >= 50) {
                                    onChunk(chunkBuffer.toString())
                                    chunkBuffer.setLength(0)
                                    lastForwardTime = now
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Skip malformed SSE frame
                    }
                }
            }

            // Flush remaining chunks before completing
            if (chunkBuffer.isNotEmpty()) {
                onChunk(chunkBuffer.toString())
                chunkBuffer.setLength(0)
            }
            onDone()
        } catch (e: Exception) {
            onError(e)
        }
    }
}
