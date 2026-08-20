package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MeaningResult(
    val queryText: String,
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val simpleExplanation: String,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val example: String? = null,
    val contextOrRephrase: String? = null,
    val additionalDefinitions: List<String> = emptyList()
)

object DatamuseMeaningService {

    private fun fetchJsonArray(urlString: String): JSONArray? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 OutlinerNoteApp")

            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                JSONArray(text)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun findMeaning(text: String): Result<MeaningResult> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please select or enter a word."))
        }

        // Clean query: if user selected punctuation or spaces around word
        val words = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val targetWord = if (words.size == 1) {
            trimmed.lowercase().filter { it.isLetter() || it == '-' || it == '\'' }
        } else {
            // Phrase or sentence: extract primary search term or query phrase
            words.first().lowercase().filter { it.isLetter() || it == '-' }
        }

        if (targetWord.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Could not identify word from selection."))
        }

        try {
            coroutineScope {
                val encodedWord = URLEncoder.encode(targetWord, "UTF-8")
                
                // 1. Fetch Definition: https://api.datamuse.com/words?sp=WORD&md=d&max=3
                val defDeferred = async {
                    fetchJsonArray("https://api.datamuse.com/words?sp=$encodedWord&md=d&max=3")
                }
                // 2. Fetch Synonyms: https://api.datamuse.com/words?rel_syn=WORD&max=10
                val synDeferred = async {
                    fetchJsonArray("https://api.datamuse.com/words?rel_syn=$encodedWord&max=10")
                }
                // 3. Fetch Antonyms: https://api.datamuse.com/words?rel_ant=WORD&max=10
                val antDeferred = async {
                    fetchJsonArray("https://api.datamuse.com/words?rel_ant=$encodedWord&max=10")
                }

                val defArray = defDeferred.await()
                val synArray = synDeferred.await()
                val antArray = antDeferred.await()

                val synonymsList = mutableListOf<String>()
                synArray?.let {
                    for (i in 0 until it.length()) {
                        val w = it.getJSONObject(i).optString("word", "")
                        if (w.isNotBlank()) synonymsList.add(w)
                    }
                }

                val antonymsList = mutableListOf<String>()
                antArray?.let {
                    for (i in 0 until it.length()) {
                        val w = it.getJSONObject(i).optString("word", "")
                        if (w.isNotBlank()) antonymsList.add(w)
                    }
                }

                val definitionsList = mutableListOf<Pair<String, String>>() // (partOfSpeech, defText)
                if (defArray != null && defArray.length() > 0) {
                    for (i in 0 until defArray.length()) {
                        val item = defArray.getJSONObject(i)
                        val defs = item.optJSONArray("defs")
                        if (defs != null) {
                            for (d in 0 until defs.length()) {
                                val rawDef = defs.optString(d, "")
                                if (rawDef.isNotBlank()) {
                                    val parts = rawDef.split("\t", limit = 2)
                                    val pos = when (parts.getOrNull(0)?.trim()?.lowercase()) {
                                        "n" -> "Noun"
                                        "v" -> "Verb"
                                        "adj" -> "Adjective"
                                        "adv" -> "Adverb"
                                        "u" -> "General"
                                        else -> "Word"
                                    }
                                    val defText = parts.getOrNull(1)?.trim() ?: rawDef
                                    definitionsList.add(pos to defText)
                                }
                            }
                        }
                    }
                }

                // If Datamuse definitions were empty, try DictionaryAPI fallback
                if (definitionsList.isEmpty()) {
                    try {
                        val fallback = DictionaryService.lookupWord(targetWord).getOrNull()
                        if (fallback != null && fallback.isNotEmpty()) {
                            val first = fallback.first()
                            first.meanings.forEach { m ->
                                val posName = m.partOfSpeech.replaceFirstChar { it.uppercase() }
                                m.definitions.forEach { d ->
                                    definitionsList.add(posName to d.definition)
                                }
                                if (synonymsList.isEmpty()) {
                                    synonymsList.addAll(m.synonyms)
                                }
                                if (antonymsList.isEmpty()) {
                                    antonymsList.addAll(m.antonyms)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore fallback error
                    }
                }

                if (definitionsList.isEmpty()) {
                    return@coroutineScope Result.failure(Exception("No definitions found for \"$trimmed\"."))
                }

                val primaryPos = definitionsList.first().first
                val primaryDef = definitionsList.first().second
                val extraDefs = definitionsList.drop(1).map { "${it.first}: ${it.second}" }

                // Build simple explanation and rephrase
                val simpleExplanation = buildSimpleExplanation(targetWord, primaryPos, primaryDef)
                val contextOrRephrase = buildContextRephrase(trimmed, targetWord, primaryPos, primaryDef, synonymsList)

                val result = MeaningResult(
                    queryText = trimmed,
                    word = targetWord.replaceFirstChar { it.uppercase() },
                    partOfSpeech = primaryPos,
                    definition = primaryDef,
                    simpleExplanation = simpleExplanation,
                    synonyms = synonymsList.distinct().take(10),
                    antonyms = antonymsList.distinct().take(10),
                    example = "Example: In this context, \"$targetWord\" refers to $primaryDef",
                    contextOrRephrase = contextOrRephrase,
                    additionalDefinitions = extraDefs.take(3)
                )

                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSimpleExplanation(word: String, pos: String, def: String): String {
        val cleanDef = def.removeSuffix(".")
        return when (pos.lowercase()) {
            "noun" -> "A $word refers to $cleanDef."
            "verb" -> "To $word means to $cleanDef."
            "adjective" -> "Describes something that is $cleanDef."
            "adverb" -> "Describes an action done in a way that is $cleanDef."
            else -> "$word means $cleanDef."
        }
    }

    private fun buildContextRephrase(
        originalSelection: String,
        word: String,
        pos: String,
        def: String,
        synonyms: List<String>
    ): String {
        return if (synonyms.isNotEmpty()) {
            "You can simply rephrase or replace \"$word\" with: ${synonyms.take(3).joinToString(", ")}."
        } else {
            "In simpler terms: $def."
        }
    }
}
